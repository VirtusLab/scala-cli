package scala.cli.commands.fix
import com.github.difflib.{DiffUtils, UnifiedDiffUtils}
import os.{BasePathImpl, FilePath}

import scala.build.Ops.EitherMap2
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.input.*
import scala.build.internal.Constants
import scala.build.options.{BuildOptions, Scope, SuppressWarningOptions}
import scala.build.preprocessing.directives.*
import scala.build.preprocessing.{ExtractedDirectives, SheBang}
import scala.build.{CrossSources, Logger, Position, Sources}
import scala.cli.commands.util.CommandHelpers
import scala.jdk.CollectionConverters.*
import scala.util.chaining.scalaUtilChainingOps

object BuiltInRules extends CommandHelpers {
  private lazy val targetDirectivesKeysSet = DirectivesPreprocessingUtils.requireDirectiveHandlers
    .flatMap(_.keys.flatMap(_.nameAliases)).toSet
  private lazy val usingDirectivesKeysGrouped = DirectivesPreprocessingUtils.usingDirectiveHandlers
    .flatMap(_.keys)
  private lazy val usingDirectivesWithTestPrefixKeysGrouped =
    DirectivesPreprocessingUtils.usingDirectiveWithReqsHandlers
      .flatMap(_.keys)

  private lazy val directiveTestPrefix = "test."
  extension (strictDirective: StrictDirective) {
    private def hasTestPrefix: Boolean        = strictDirective.key.startsWith(directiveTestPrefix)
    private def existsTestEquivalent: Boolean =
      !strictDirective.hasTestPrefix &&
      usingDirectivesWithTestPrefixKeysGrouped
        .exists(_.nameAliases.contains(directiveTestPrefix + strictDirective.key))
  }

  private val newLine: String = scala.build.internal.AmmUtil.lineSeparator

  /** @return
    *   true if any changes are needed; always false unless running with `check`, as changes are
    *   applied right away otherwise
    */
  def runRules(
    inputs: Inputs,
    buildOptions: BuildOptions,
    check: Boolean,
    logger: Logger
  )(using ScalaCliInvokeData): Boolean = {
    val (mainSources, testSources) = getProjectSources(inputs, logger)
      .left.map(CompositeBuildException(_))
      .orExit(logger)

    val sourcesCount =
      mainSources.paths.length + mainSources.inMemory.length +
        testSources.paths.length + testSources.inMemory.length
    sourcesCount match
      case 0 =>
        logger.message("No sources to migrate directives from.")
        logger.message("Nothing to do.")
        false
      case 1 =>
        logger.message("No need to migrate directives for a single source file project.")
        logger.message("Nothing to do.")
        false
      case _ =>
        migrateDirectives(inputs, buildOptions, mainSources, testSources, check, logger)
  }

  private def migrateDirectives(
    inputs: Inputs,
    buildOptions: BuildOptions,
    mainSources: Sources,
    testSources: Sources,
    check: Boolean,
    logger: Logger
  ): Boolean = {
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
      val originalMainDirectives =
        getExtractedDirectives(mainSources, buildOptions.suppressWarningOptions)
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
        transformedMainDirectives.filter(d => isExtractedFromWritableInput(d.position)),
        testScopeDirectives
      )
    }

    // Deal with directives from the Test scope
    val directivesFromWritableTestInputs: Seq[TransformedTestDirectives] =
      if (
        testSources.paths.nonEmpty || testSources.inMemory.nonEmpty ||
        testDirectivesFromMain.nonEmpty
      ) {
        val originalTestDirectives =
          getExtractedDirectives(testSources, buildOptions.suppressWarningOptions)
            .filterNot(hasTargetDirectives)

        val transformedTestDirectives = unifyCorrespondingNameAliases(originalTestDirectives)
          .pipe(maybeTransformIntoTestEquivalent)

        val allDirectives = for {
          directivesWithTestPrefix              <- transformedTestDirectives.map(_.withTestPrefix)
          directivesWithNoTestPrefixEquivalents <-
            transformedTestDirectives.map {
              _.noTestPrefixAvailable
                .filter(_.existsTestEquivalent)
            }
          directive <-
            directivesWithTestPrefix ++ directivesWithNoTestPrefixEquivalents ++
              testDirectivesFromMain
        } yield directive

        createFormattedLinesAndAppend(allDirectives, projectFileContents, isTest = true)

        transformedTestDirectives
          .filter(ttd => isExtractedFromWritableInput(ttd.positions))
      }
      else Seq(TransformedTestDirectives(Nil, Nil, None))

    projectFileContents.append(newLine)

    // Write extracted directives to project.scala
    val projectFilePath        = inputs.workspace / Constants.projectFileName
    val newProjectFileContents = projectFileContents.toString
    val projectFileNeedsUpdate =
      if check then
        reportCheckFailure(projectFilePath, newProjectFileContents)
      else
        logger.message(s"Writing ${Constants.projectFileName}")
        os.write.over(projectFilePath, newProjectFileContents)
        false

    def isProjectFile(position: Option[Position.File]): Boolean =
      position.exists(_.path.contains(inputs.workspace / Constants.projectFileName))

    // Remove directives from their original files, skip the project.scala file
    val mainInputsNeedUpdate = directivesFromWritableMainInputs
      .filterNot(e => isProjectFile(e.position))
      .map(d => removeDirectivesFrom(d.position, check))
    val testInputsNeedUpdate = directivesFromWritableTestInputs
      .filterNot(ttd => isProjectFile(ttd.positions))
      .map(ttd =>
        removeDirectivesFrom(
          position = ttd.positions,
          check = check,
          toKeep = ttd.noTestPrefixAvailable.filterNot(_.existsTestEquivalent)
        )
      )

    projectFileNeedsUpdate || (mainInputsNeedUpdate ++ testInputsNeedUpdate).contains(true)
  }

  /** Logs a unified diff of the changes `fix` would have applied to `path`.
    *
    * @return
    *   true if the file is out of date
    */
  private def reportCheckFailure(path: os.Path, newContents: String)(
    using loggingUtilities: LoggingUtilities
  ): Boolean =
    val oldContents = if os.exists(path) then os.read(path) else ""
    if oldContents == newContents then false
    else
      val oldLines = oldContents.linesIterator.toVector.asJava
      val newLines = newContents.linesIterator.toVector.asJava
      UnifiedDiffUtils
        .generateUnifiedDiff(
          loggingUtilities.relativePath(path).toString,
          "<expected fix>",
          oldLines,
          DiffUtils.diff(oldLines, newLines),
          3
        )
        .asScala
        .foreach(line => loggingUtilities.logger.message(line))
      true

  private def getProjectSources(inputs: Inputs, logger: Logger)(using
    ScalaCliInvokeData
  ): Either[::[BuildException], (Sources, Sources)] = {
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
      exclude = buildOptions.internal.exclude,
      download = buildOptions.downloader
    ).orExit(logger)

    val sharedOptions = crossSources.sharedOptions(buildOptions)
    val scopedSources = crossSources.scopedSources(sharedOptions).orExit(logger)

    val mainSources = scopedSources.sources(Scope.Main, sharedOptions, inputs.workspace, logger)
    val testSources = scopedSources.sources(Scope.Test, sharedOptions, inputs.workspace, logger)

    (mainSources, testSources).traverseN
  }

  private def getExtractedDirectives(
    sources: Sources,
    suppressWarningOptions: SuppressWarningOptions
  )(
    using loggingUtilities: LoggingUtilities
  ): Seq[ExtractedDirectives] = {
    val logger = loggingUtilities.logger

    val fromPaths = sources.paths.map { (path, _) =>
      val (_, content, _) = SheBang.partitionOnShebangSection(os.read(path))
      logger.debug(s"Extracting directives from ${loggingUtilities.relativePath(path)}")
      ExtractedDirectives.from(
        contentChars = content.toIndexedSeq,
        path = Right(path),
        suppressWarningOptions = suppressWarningOptions,
        logger = logger,
        maybeRecoverOnError = _ => None
      ).orExit(logger)
    }

    val fromInMemory = sources.inMemory.map { inMem =>
      val originOrPath = inMem.originalPath.map((_, path) => path)
      val content      = originOrPath match {
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

      val (_, contentWithNoShebang, _) = SheBang.partitionOnShebangSection(content)

      ExtractedDirectives.from(
        contentChars = contentWithNoShebang.toIndexedSeq,
        path = originOrPath,
        suppressWarningOptions = suppressWarningOptions,
        logger = logger,
        maybeRecoverOnError = _ => None
      ).orExit(logger)
    }

    fromPaths ++ fromInMemory
  }

  private def hasTargetDirectives(extractedDirectives: ExtractedDirectives): Boolean = {
    // Filter out all elements that contain using target directives
    val directivesInElement = extractedDirectives.directives.map(_.key)
    directivesInElement.exists(key => targetDirectivesKeysSet.contains(key))
  }

  private def unifyCorrespondingNameAliases(extractedDirectives: Seq[ExtractedDirectives]) =
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
  private def maybeTransformIntoTestEquivalent(extractedDirectives: Seq[ExtractedDirectives])
    : Seq[TransformedTestDirectives] =
    for {
      extractedFromSingleElement <- extractedDirectives
      directives = extractedFromSingleElement.directives
    } yield {
      val (withInitialTestPrefix, noInitialTestPrefix) = directives.partition(_.hasTestPrefix)
      val (withTestEquivalent, noTestEquivalent)       =
        noInitialTestPrefix.partition(_.existsTestEquivalent)
      val transformedToTestEquivalents = withTestEquivalent.map {
        case StrictDirective(key, values, _, _) => StrictDirective("test." + key, values)
      }

      TransformedTestDirectives(
        withTestPrefix = transformedToTestEquivalents ++ withInitialTestPrefix,
        noTestPrefixAvailable = noTestEquivalent,
        positions = extractedFromSingleElement.position
      )
    }

  private def removeDirectivesFrom(
    position: Option[Position.File],
    check: Boolean,
    toKeep: Seq[StrictDirective] = Nil
  )(
    using loggingUtilities: LoggingUtilities
  ): Boolean =
    position match {
      case Some(Position.File(Right(path), _, _, offset)) =>
        val (shebangSection, strippedContent, newLine) =
          SheBang.partitionOnShebangSection(os.read(path))

        def ignoreOrAddNewLine(str: String) = if str.isBlank then "" else str + newLine

        val keepLines = ignoreOrAddNewLine(shebangSection) + ignoreOrAddNewLine(toKeep.mkString(
          "",
          newLine,
          newLine
        ))
        val newContents  = (keepLines + strippedContent.drop(offset).stripLeading()).stripLeading()
        val relativePath = loggingUtilities.relativePath(path)

        if check then
          reportCheckFailure(path, newContents)
        else
          loggingUtilities.logger.message(s"Removing directives from $relativePath")
          if toKeep.nonEmpty then
            loggingUtilities.logger.message("  Keeping:")
            toKeep.foreach(d => loggingUtilities.logger.message(s"    $d"))
          os.write.over(path, newContents)
          false
      case _ => false
    }

  private def createFormattedLinesAndAppend(
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
        .groupBy(dir =>
          (if (isTest) dir.key.stripPrefix(directiveTestPrefix) else dir.key).takeWhile(_ != '.')
        )
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

  private case class TransformedTestDirectives(
    withTestPrefix: Seq[StrictDirective],
    noTestPrefixAvailable: Seq[StrictDirective],
    positions: Option[Position.File]
  )

  private case class LoggingUtilities(
    logger: Logger,
    workspacePath: os.Path
  ) {
    def relativePath(path: os.Path): FilePath & BasePathImpl =
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
        Wasm.handler.keys,
        ScalacOptions.handler.keys,
        JavaOptions.handler.keys,
        JavacOptions.handler.keys,
        JavaProps.handler.keys,
        MainClass.handler.keys,
        scala.build.preprocessing.directives.Sources.handler.keys,
        ObjectWrapper.handler.keys,
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
