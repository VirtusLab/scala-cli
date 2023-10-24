package scala.cli.commands.fix

import caseapp.core.RemainingArgs

import scala.build.Ops.EitherMap2
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.input.*
import scala.build.internal.Constants
import scala.build.options.{BuildOptions, Scope, SuppressWarningOptions}
import scala.build.preprocessing.directives.*
import scala.build.preprocessing.{ExtractedDirectives, SheBang}
import scala.build.{CrossSources, Logger, Position, Sources}
import scala.cli.commands.shared.SharedOptions
import scala.cli.commands.{ScalaCommand, SpecificationLevel}
import scala.collection.immutable.HashMap
import scala.util.chaining.scalaUtilChainingOps

object Fix extends ScalaCommand[FixOptions] {
  override def group                   = "Main"
  override def scalaSpecificationLevel = SpecificationLevel.EXPERIMENTAL
  override def sharedOptions(options: FixOptions): Option[SharedOptions] = Some(options.shared)

  lazy val targetDirectivesKeysSet = DirectivesPreprocessingUtils.requireDirectiveHandlers
    .flatMap(_.keys.flatMap(_.nameAliases)).toSet
  lazy val usingDirectivesKeysGrouped = DirectivesPreprocessingUtils.usingDirectiveHandlers
    .flatMap(_.keys)
  lazy val usingDirectivesWithTestPrefixKeysGrouped =
    DirectivesPreprocessingUtils.usingDirectiveWithReqsHandlers
      .flatMap(_.keys)

  val newLine = System.lineSeparator()

  override def runCommand(options: FixOptions, args: RemainingArgs, logger: Logger): Unit = {
    val inputs = options.shared.inputs(args.remaining, () => Inputs.default()).orExit(logger)

    val (mainSources, testSources) = getProjectSources(inputs)
      .left.map(CompositeBuildException(_))
      .orExit(logger)

    // Only initial inputs are used, new inputs discovered during processing of
    // CrossSources.forInput may be shared between projects
    val writableInputs: Seq[OnDisk] = inputs.flattened()
      .collect { case onDisk: OnDisk => onDisk }

    def isExtractedFromWritableInput(position: Option[Position.File]): Boolean = {
      val originOrPathOpt = position.map(_.path)
      originOrPathOpt match {
        case Some(Right(path)) => writableInputs.exists(_.path == path)
        case _                 => false
      }
    }

    val projectFileContents = new StringBuilder()

    given LoggingUtilities(logger, inputs.workspace)

    // Deal with directives from the Main scope
    val (directivesFromWritableMainInputs, testDirectivesFromMain) = {
      val originalMainDirectives = getExtractedDirectives(mainSources)
        .filterNot(hasTargetDirectives)

      val transformedMainDirectives = unifyCorrespondingNameAliases(originalMainDirectives)

      val allDirectives = for {
        transformedMainDirective <- transformedMainDirectives
        directive                <- transformedMainDirective.directives
      } yield directive

      val (testScopeDirectives, allMainDirectives) =
        allDirectives.partition(_.key.startsWith("test"))

      createFormattedLinesAndAppend(allMainDirectives, projectFileContents, isTest = false)

      (
        transformedMainDirectives.filter(d => isExtractedFromWritableInput(d.positions)),
        testScopeDirectives
      )
    }

    // Deal with directives from the Test scope
    val directivesFromWritableTestInputs: Seq[TransformedTestDirectives] =
      if (
        testSources.paths.nonEmpty || testSources.inMemory.nonEmpty || testDirectivesFromMain.nonEmpty
      ) {
        val originalTestDirectives = getExtractedDirectives(testSources)
          .filterNot(hasTargetDirectives)

        val transformedTestDirectives = unifyCorrespondingNameAliases(originalTestDirectives)
          .pipe(maybeTransformIntoTestEquivalent)

        val allDirectives = for {
          directivesWithTestPrefix <- transformedTestDirectives.map(_.withTestPrefix)
          directive                <- directivesWithTestPrefix ++ testDirectivesFromMain
        } yield directive

        createFormattedLinesAndAppend(allDirectives, projectFileContents, isTest = true)

        transformedTestDirectives
          .filter(ttd => isExtractedFromWritableInput(ttd.positions))
      }
      else Seq(TransformedTestDirectives(Nil, Nil, None))

    projectFileContents.append(newLine)

    // Write extracted directives to project.scala
    logger.message(s"Writing ${Constants.projectFileName}")
    os.write.over(inputs.workspace / Constants.projectFileName, projectFileContents.toString)

    def isProjectFile(position: Option[Position.File]): Boolean =
      position.exists(_.path.contains(inputs.workspace / Constants.projectFileName))

    // Remove directives from their original files, skip the project.scala file
    directivesFromWritableMainInputs
      .filterNot(e => isProjectFile(e.positions))
      .foreach(d => removeDirectivesFrom(d.positions))
    directivesFromWritableTestInputs
      .filterNot(ttd => isProjectFile(ttd.positions))
      .foreach(ttd => removeDirectivesFrom(ttd.positions, toKeep = ttd.noTestPrefixAvailable))
  }

  def getProjectSources(inputs: Inputs): Either[::[BuildException], (Sources, Sources)] = {
    val buildOptions = BuildOptions()

    val (crossSources, _) = CrossSources.forInputs(
      inputs,
      preprocessors = Sources.defaultPreprocessors(
        buildOptions.archiveCache,
        buildOptions.internal.javaClassNameVersionOpt,
        () => buildOptions.javaHome().value.javaCommand
      ),
      logger = logger,
      suppressWarningOptions = SuppressWarningOptions.suppressAll,
      exclude = buildOptions.internal.exclude
    ).orExit(logger)

    val sharedOptions = crossSources.sharedOptions(buildOptions)
    val scopedSources = crossSources.scopedSources(sharedOptions).orExit(logger)

    val mainSources = scopedSources.sources(Scope.Main, sharedOptions, inputs.workspace)
    val testSources = scopedSources.sources(Scope.Test, sharedOptions, inputs.workspace)

    (mainSources, testSources).traverseN
  }

  def getExtractedDirectives(sources: Sources)(
    using loggingUtilities: LoggingUtilities
  ): Seq[ExtractedDirectives] = {
    val logger = loggingUtilities.logger

    val fromPaths = sources.paths.map { (path, _) =>
      val (_, content) = SheBang.partitionOnShebangSection(os.read(path))
      logger.debug(s"Extracting directives from ${loggingUtilities.relativePath(path)}")
      ExtractedDirectives.from(content.toCharArray, Right(path), logger, _ => None).orExit(logger)
    }

    val fromInMemory = sources.inMemory.map { inMem =>
      val originOrPath = inMem.originalPath.map((_, path) => path)
      val content = originOrPath match {
        case Right(path) =>
          logger.debug(s"Extracting directives from ${loggingUtilities.relativePath(path)}")
          os.read(path)
        case Left(origin) =>
          logger.debug(s"Extracting directives from $origin")
          inMem.wrapperParamsOpt match {
            // In case of script snippets, we need to drop the top wrapper lines
            case Some(wrapperParams) => String(inMem.content)
                .linesWithSeparators
                .drop(wrapperParams.topWrapperLineCount)
                .mkString
            case None => String(inMem.content)
          }
      }

      val (_, contentWithNoShebang) = SheBang.partitionOnShebangSection(content)

      ExtractedDirectives.from(
        contentWithNoShebang.toCharArray,
        originOrPath,
        logger,
        _ => None
      ).orExit(logger)
    }

    fromPaths ++ fromInMemory
  }

  def hasTargetDirectives(extractedDirectives: ExtractedDirectives): Boolean = {
    // Filter out all elements that contain using target directives
    val directivesInElement = extractedDirectives.directives.map(_.key)
    directivesInElement.exists(key => targetDirectivesKeysSet.contains(key))
  }

  def unifyCorrespondingNameAliases(extractedDirectives: Seq[ExtractedDirectives]) =
    extractedDirectives.map { extracted =>
      // All keys that we migrate, not all in general
      val allKeysGrouped   = usingDirectivesKeysGrouped ++ usingDirectivesWithTestPrefixKeysGrouped
      val strictDirectives = extracted.directives

      val strictDirectivesWithNewKeys = strictDirectives.flatMap { strictDir =>
        val newKeyOpt = allKeysGrouped.find(_.nameAliases.contains(strictDir.key))
          .flatMap(_.nameAliases.headOption)
          .map { key =>
            if (key.startsWith("test"))
              val withTestStripped = key.stripPrefix("test").stripPrefix(".")
              "test." + withTestStripped.take(1).toLowerCase + withTestStripped.drop(1)
            else
              key
          }

        newKeyOpt.map(newKey => strictDir.copy(key = newKey))
      }

      extracted.copy(directives = strictDirectivesWithNewKeys)
    }

  /** Transforms directives into their 'test.' equivalent if it exists
    *
    * @param extractedDirectives
    * @return
    *   an instance of TransformedTestDirectives containing transformed directives and those that
    *   could not be transformed since they have no 'test.' equivalent
    */
  def maybeTransformIntoTestEquivalent(extractedDirectives: Seq[ExtractedDirectives])
    : Seq[TransformedTestDirectives] =
    for {
      extractedFromSingleElement <- extractedDirectives
      directives = extractedFromSingleElement.directives
    } yield {
      val (withTestEquivalent, noTestEquivalent) = directives.partition { directive =>
        usingDirectivesWithTestPrefixKeysGrouped.exists(
          _.nameAliases.contains("test." + directive.key)
        )
      }

      val transformedToTestEquivalents = withTestEquivalent.map {
        case StrictDirective(key, values) => StrictDirective("test." + key, values)
      }

      TransformedTestDirectives(
        withTestPrefix = transformedToTestEquivalents,
        noTestPrefixAvailable = noTestEquivalent,
        positions = extractedFromSingleElement.positions
      )
    }

  def removeDirectivesFrom(
    position: Option[Position.File],
    toKeep: Seq[StrictDirective] = Nil
  )(
    using loggingUtilities: LoggingUtilities
  ): Unit = {
    position match {
      case Some(Position.File(Right(path), _, _, offset)) =>
        val (shebangSection, strippedContent) = SheBang.partitionOnShebangSection(os.read(path))

        def ignoreOrAddNewLine(str: String) = if str.isBlank then "" else str + newLine

        val keepLines = ignoreOrAddNewLine(shebangSection) + ignoreOrAddNewLine(toKeep.mkString(
          "",
          newLine,
          newLine
        ))
        val newContents  = keepLines + strippedContent.drop(offset).stripLeading()
        val relativePath = loggingUtilities.relativePath(path)

        loggingUtilities.logger.message(s"Removing directives from $relativePath")
        if (toKeep.nonEmpty) {
          loggingUtilities.logger.message("  Keeping:")
          toKeep.foreach(d => loggingUtilities.logger.message(s"    $d"))
        }

        os.write.over(path, newContents.stripLeading())
      case _ => ()
    }
  }

  def createFormattedLinesAndAppend(
    strictDirectives: Seq[StrictDirective],
    projectFileContents: StringBuilder,
    isTest: Boolean
  ): Unit = {
    if (strictDirectives.nonEmpty) {
      projectFileContents
        .append(if (projectFileContents.nonEmpty) newLine else "")
        .append(if isTest then "// Test" else "// Main")
        .append(newLine)

      strictDirectives
        // group by key to merge values
        .groupBy(_.key)
        .map { (key, directives) =>
          StrictDirective(key, directives.flatMap(_.values))
        }
        // group by key prefixes to create splits between groups
        .groupBy(dir => (if (isTest) dir.key.stripPrefix("test.") else dir.key).takeWhile(_ != '.'))
        .map { (_, directives) =>
          directives.flatMap(_.explodeToStringsWithColLimit()).toSeq.sorted
        }
        .toSeq
        .filter(_.nonEmpty)
        .sortBy(_.head)(using directivesOrdering)
        // append groups to the StringBuilder, add new lines between groups that are bigger than one line
        .foldLeft(0) { (lastSize, directiveLines) =>
          val newSize = directiveLines.size
          if (lastSize > 1 || (lastSize != 0 && newSize > 1)) projectFileContents.append(newLine)

          directiveLines.foreach(projectFileContents.append(_).append(newLine))

          newSize
        }
    }
  }

  case class TransformedTestDirectives(
    withTestPrefix: Seq[StrictDirective],
    noTestPrefixAvailable: Seq[StrictDirective],
    positions: Option[Position.File]
  )

  case class LoggingUtilities(
    logger: Logger,
    workspacePath: os.Path
  ) {
    def relativePath(path: os.Path) =
      if (path.startsWith(workspacePath)) path.relativeTo(workspacePath)
      else path
  }

  private val directivesOrdering: Ordering[String] = {
    def directivesOrder(key: String): Int = {
      val handlersOrder = Seq(
        ScalaVersion.handler.keys,
        Platform.handler.keys,
        Jvm.handler.keys,
        JavaHome.handler.keys,
        ScalaNative.handler.keys,
        ScalaJs.handler.keys,
        ScalacOptions.handler.keys,
        JavaOptions.handler.keys,
        JavacOptions.handler.keys,
        JavaProps.handler.keys,
        MainClass.handler.keys,
        scala.build.preprocessing.directives.Sources.handler.keys,
        ScriptWrapper.handler.keys,
        Toolkit.handler.keys,
        Dependency.handler.keys
      )

      handlersOrder.zipWithIndex
        .find(_._1.flatMap(_.nameAliases).contains(key))
        .map(_._2)
        .getOrElse(if key.startsWith("publish") then 20 else 15)
    }

    Ordering.by { directiveLine =>
      val key = directiveLine
        .stripPrefix("//> using")
        .stripLeading()
        .stripPrefix("test.")
        // separate key from value
        .takeWhile(!_.isWhitespace)

      directivesOrder(key)
    }
  }
}
