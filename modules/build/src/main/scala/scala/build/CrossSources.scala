package scala.build

import java.io.File

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.Positioned
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  ExcludeDefinitionError,
  MalformedDirectiveError
}
import scala.build.input.ElementsUtils.*
import scala.build.input.*
import scala.build.internal.Constants
import scala.build.internal.util.RegexUtils
import scala.build.options.{
  BuildOptions,
  BuildRequirements,
  MaybeScalaVersion,
  Scope,
  SuppressWarningOptions,
  WithBuildRequirements
}
import scala.build.preprocessing.*
import scala.build.testrunner.DynamicTestRunner.globPattern
import scala.util.Try
import scala.util.chaining.*

/** CrossSources with unwrapped scripts, use [[withWrappedScripts]] to wrap them and obtain an
  * instance of CrossSources
  *
  * See [[CrossSources]] for more information
  *
  * @param paths
  *   paths and realtive paths to sources on disk, wrapped in their build requirements
  * @param inMemory
  *   in memory sources (e.g. snippets) wrapped in their build requirements
  * @param defaultMainClass
  * @param resourceDirs
  * @param buildOptions
  *   build options from sources
  * @param unwrappedScripts
  *   in memory script sources, their code must be wrapped before compiling
  */
sealed class UnwrappedCrossSources(
  paths: Seq[WithBuildRequirements[(os.Path, os.RelPath)]],
  inMemory: Seq[WithBuildRequirements[Sources.InMemory]],
  defaultMainClass: Option[String],
  resourceDirs: Seq[WithBuildRequirements[os.Path]],
  buildOptions: Seq[WithBuildRequirements[BuildOptions]],
  unwrappedScripts: Seq[WithBuildRequirements[Sources.UnwrappedScript]]
) {

  /** For all unwrapped script sources contained in this object wrap them according to provided
    * BuildOptions
    *
    * @param buildOptions
    *   options used to choose the script wrapper
    * @return
    *   CrossSources with all the scripts wrapped
    */
  def withWrappedScripts(buildOptions: BuildOptions): CrossSources = {
    val codeWrapper = ScriptPreprocessor.getScriptWrapper(buildOptions)

    val wrappedScripts = unwrappedScripts.map { unwrapppedWithRequirements =>
      unwrapppedWithRequirements.map(_.wrap(codeWrapper))
    }

    CrossSources(
      paths,
      inMemory ++ wrappedScripts,
      defaultMainClass,
      resourceDirs,
      this.buildOptions
    )
  }

  def sharedOptions(baseOptions: BuildOptions): BuildOptions =
    buildOptions
      .filter(_.requirements.isEmpty)
      .map(_.value)
      .foldLeft(baseOptions)(_ orElse _)

  protected def needsScalaVersion =
    paths.exists(_.needsScalaVersion) ||
    inMemory.exists(_.needsScalaVersion) ||
    resourceDirs.exists(_.needsScalaVersion) ||
    buildOptions.exists(_.needsScalaVersion)
}

/** Information gathered from preprocessing command inputs - sources and build options from using
  * directives
  *
  * @param paths
  *   paths and realtive paths to sources on disk, wrapped in their build requirements
  * @param inMemory
  *   in memory sources (e.g. snippets and wrapped scripts) wrapped in their build requirements
  * @param defaultMainClass
  * @param resourceDirs
  * @param buildOptions
  *   build options from sources
  */
final case class CrossSources(
  paths: Seq[WithBuildRequirements[(os.Path, os.RelPath)]],
  inMemory: Seq[WithBuildRequirements[Sources.InMemory]],
  defaultMainClass: Option[String],
  resourceDirs: Seq[WithBuildRequirements[os.Path]],
  buildOptions: Seq[WithBuildRequirements[BuildOptions]]
) extends UnwrappedCrossSources(
      paths,
      inMemory,
      defaultMainClass,
      resourceDirs,
      buildOptions,
      Nil
    ) {
  def scopedSources(baseOptions: BuildOptions): Either[BuildException, ScopedSources] = either {

    val sharedOptions0 = sharedOptions(baseOptions)

    // FIXME Not 100% sure the way we compute the intermediate and final BuildOptions
    // is consistent (we successively filter out / retain options to compute a scala
    // version and platform, which might not be the version and platform of the final
    // BuildOptions).

    val crossSources0 =
      if (needsScalaVersion) {

        val retainedScalaVersion = value(sharedOptions0.scalaParams)
          .map(p => MaybeScalaVersion(p.scalaVersion))
          .getOrElse(MaybeScalaVersion.none)

        val buildOptionsWithScalaVersion = buildOptions
          .flatMap(_.withScalaVersion(retainedScalaVersion).toSeq)
          .filter(_.requirements.isEmpty)
          .map(_.value)
          .foldLeft(sharedOptions0)(_ orElse _)

        val platform = buildOptionsWithScalaVersion.platform

        copy(
          paths = paths
            .flatMap(_.withScalaVersion(retainedScalaVersion).toSeq)
            .flatMap(_.withPlatform(platform.value).toSeq),
          inMemory = inMemory
            .flatMap(_.withScalaVersion(retainedScalaVersion).toSeq)
            .flatMap(_.withPlatform(platform.value).toSeq),
          resourceDirs = resourceDirs
            .flatMap(_.withScalaVersion(retainedScalaVersion).toSeq)
            .flatMap(_.withPlatform(platform.value).toSeq),
          buildOptions = buildOptions
            .filter(!_.requirements.isEmpty)
            .flatMap(_.withScalaVersion(retainedScalaVersion).toSeq)
            .flatMap(_.withPlatform(platform.value).toSeq)
        )
      }
      else {

        val platform = sharedOptions0.platform

        copy(
          paths = paths
            .flatMap(_.withPlatform(platform.value).toSeq),
          inMemory = inMemory
            .flatMap(_.withPlatform(platform.value).toSeq),
          resourceDirs = resourceDirs
            .flatMap(_.withPlatform(platform.value).toSeq),
          buildOptions = buildOptions
            .filter(!_.requirements.isEmpty)
            .flatMap(_.withPlatform(platform.value).toSeq)
        )
      }

    val defaultScope: Scope = Scope.Main
    ScopedSources(
      crossSources0.paths.map(_.scopedValue(defaultScope)),
      crossSources0.inMemory.map(_.scopedValue(defaultScope)),
      defaultMainClass,
      crossSources0.resourceDirs.map(_.scopedValue(defaultScope)),
      crossSources0.buildOptions.map(_.scopedValue(defaultScope))
    )
  }
}

object CrossSources {

  private def withinTestSubDirectory(p: ScopePath, inputs: Inputs): Boolean =
    p.root.exists { path =>
      val fullPath = path / p.subPath
      inputs.elements.exists {
        case Directory(path) =>
          // Is this file subdirectory of given dir and if we have a subdiretory 'test' on the way
          fullPath.startsWith(path) &&
          fullPath.relativeTo(path).segments.contains("test")
        case _ => false
      }
    }

  /** @return
    *   a CrossSources and Inputs which contains element processed from using directives
    */
  def forInputs(
    inputs: Inputs,
    preprocessors: Seq[Preprocessor],
    logger: Logger,
    suppressWarningOptions: SuppressWarningOptions,
    exclude: Seq[Positioned[String]] = Nil,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e)
  )(using ScalaCliInvokeData): Either[BuildException, (UnwrappedCrossSources, Inputs)] = either {

    def preprocessSources(elems: Seq[SingleElement])
      : Either[BuildException, Seq[PreprocessedSource]] =
      elems
        .map { elem =>
          preprocessors
            .iterator
            .flatMap(p =>
              p.preprocess(
                elem,
                logger,
                maybeRecoverOnError,
                inputs.allowRestrictedFeatures,
                suppressWarningOptions
              ).iterator
            )
            .take(1)
            .toList
            .headOption
            .getOrElse(Right(Nil)) // FIXME Warn about unprocessed stuff?
        }
        .sequence
        .left.map(CompositeBuildException(_))
        .map(_.flatten)

    val flattenedInputs = inputs.flattened()
    val allExclude = { // supports only one exclude directive in one source file, which should be the project file.
      val projectScalaFileOpt = flattenedInputs.collectFirst {
        case f: ProjectScalaFile => f
      }
      val excludeFromProjectFile =
        value(preprocessSources(projectScalaFileOpt.toSeq))
          .flatMap(_.options).flatMap(_.internal.exclude)
      exclude ++ excludeFromProjectFile
    }

    val preprocessedInputFromArgs: Seq[PreprocessedSource] =
      value(
        preprocessSources(value(excludeSources(flattenedInputs, inputs.workspace, allExclude)))
      )

    val sourcesFromDirectives =
      preprocessedInputFromArgs
        .flatMap(_.options)
        .flatMap(_.internal.extraSourceFiles)
        .distinct
    val inputsElemFromDirectives: Seq[SingleFile] =
      value(resolveInputsFromSources(sourcesFromDirectives, inputs.enableMarkdown))
    val preprocessedSourcesFromDirectives: Seq[PreprocessedSource] =
      value(preprocessSources(inputsElemFromDirectives.pipe(elements =>
        value(excludeSources(elements, inputs.workspace, allExclude))
      )))
    val allInputs = inputs.add(inputsElemFromDirectives).pipe(inputs =>
      val filteredElements = value(excludeSources(inputs.elements, inputs.workspace, allExclude))
      inputs.withElements(elements = filteredElements)
    )

    val preprocessedSources =
      (preprocessedInputFromArgs ++ preprocessedSourcesFromDirectives).distinct
        .pipe(sources => value(validateExcludeDirectives(sources, allInputs.workspace)))

    val scopedRequirements       = preprocessedSources.flatMap(_.scopedRequirements)
    val scopedRequirementsByRoot = scopedRequirements.groupBy(_.path.root)
    def baseReqs(path: ScopePath): BuildRequirements = {
      val fromDirectives =
        scopedRequirementsByRoot
          .getOrElse(path.root, Nil)
          .flatMap(_.valueFor(path).toSeq)
          .foldLeft(BuildRequirements())(_ orElse _)

      // Scala CLI treats all `.test.scala` files tests as well as
      // files from within `test` subdirectory from provided input directories
      // If file has `using target <scope>` directive this take precendeces.
      if (
        fromDirectives.scope.isEmpty &&
        (path.subPath.last.endsWith(".test.scala") || withinTestSubDirectory(path, allInputs))
      )
        fromDirectives.copy(scope = Some(BuildRequirements.ScopeRequirement(Scope.Test)))
      else fromDirectives
    }

    val buildOptions: Seq[WithBuildRequirements[BuildOptions]] = (for {
      preprocessedSource <- preprocessedSources
      opts               <- preprocessedSource.options.toSeq
      if opts != BuildOptions() || preprocessedSource.optionsWithTargetRequirements.nonEmpty
    } yield {
      val baseReqs0 = baseReqs(preprocessedSource.scopePath)
      preprocessedSource.optionsWithTargetRequirements :+ WithBuildRequirements(
        preprocessedSource.requirements.fold(baseReqs0)(_ orElse baseReqs0),
        opts
      )
    }).flatten

    val defaultMainClassOpt: Option[String] = for {
      mainClassPath <- allInputs.defaultMainClassElement
        .map(s => ScopePath.fromPath(s.path).subPath)
      processedMainClass <- preprocessedSources.find(_.scopePath.subPath == mainClassPath)
      mainClass          <- processedMainClass.mainClassOpt
    } yield mainClass

    val pathsWithDirectivePositions
      : Seq[(WithBuildRequirements[(os.Path, os.RelPath)], Option[Position.File])] =
      preprocessedSources.collect {
        case d: PreprocessedSource.OnDisk =>
          val baseReqs0 = baseReqs(d.scopePath)
          WithBuildRequirements(
            d.requirements.fold(baseReqs0)(_ orElse baseReqs0),
            (d.path, d.path.relativeTo(allInputs.workspace))
          ) -> d.directivesPositions
      }
    val inMemoryWithDirectivePositions
      : Seq[(WithBuildRequirements[Sources.InMemory], Option[Position.File])] =
      preprocessedSources.collect {
        case m: PreprocessedSource.InMemory =>
          val baseReqs0 = baseReqs(m.scopePath)
          WithBuildRequirements(
            m.requirements.fold(baseReqs0)(_ orElse baseReqs0),
            Sources.InMemory(m.originalPath, m.relPath, m.code, m.ignoreLen)
          ) -> m.directivesPositions
      }
    val unwrappedScriptsWithDirectivePositions
      : Seq[(WithBuildRequirements[Sources.UnwrappedScript], Option[DirectivesPositions])] =
      preprocessedSources.collect {
        case m: PreprocessedSource.UnwrappedScript =>
          val baseReqs0 = baseReqs(m.scopePath)
          WithBuildRequirements(
            m.requirements.fold(baseReqs0)(_ orElse baseReqs0),
            Sources.UnwrappedScript(m.originalPath, m.relPath, m.wrapScriptFun)
          ) -> m.directivesPositions
      }

    val resourceDirs: Seq[WithBuildRequirements[os.Path]] = allInputs.elements.collect {
      case r: ResourceDirectory =>
        WithBuildRequirements(BuildRequirements(), r.path)
    } ++ preprocessedSources.flatMap(_.options).flatMap(_.classPathOptions.resourcesDir).map(
      WithBuildRequirements(BuildRequirements(), _)
    )

    lazy val allPathsWithDirectivesByScope: Map[Scope, Seq[(os.Path, Position.File)]] =
      (pathsWithDirectivePositions ++ inMemoryWithDirectivePositions ++ unwrappedScriptsWithDirectivePositions)
        .flatMap { (withBuildRequirements, directivesPositions) =>
          val scope = withBuildRequirements.scopedValue(Scope.Main).scope
          val path: os.Path = withBuildRequirements.value match
            case im: Sources.InMemory =>
              im.originalPath match
                case Right((_, p: os.Path)) => p
                case _                      => inputs.workspace / im.generatedRelPath
            case us: Sources.UnwrappedScript =>
              us.originalPath match
                case Right((_, p: os.Path)) => p
                case _                      => inputs.workspace / us.generatedRelPath
            case (p: os.Path, _) => p
          directivesPositions.map((path, scope, _))
        }
        .groupBy((_, scope, _) => scope)
        .view
        .mapValues(_.map((path, _, directivesPositions) => path -> directivesPositions))
        .toMap
    lazy val anyScopeHasMultipleSourcesWithDirectives =
      Scope.all.exists(allPathsWithDirectivesByScope.get(_).map(_.length).getOrElse(0) > 1)
    val shouldSuppressWarning =
      suppressWarningOptions.suppressDirectivesInMultipleFilesWarning.getOrElse(false)
    if !shouldSuppressWarning && anyScopeHasMultipleSourcesWithDirectives then {
      val projectFilePath = inputs.elements.projectSettingsFiles.headOption match
        case Some(s) => s.path
        case _       => inputs.workspace / Constants.projectFileName
      allPathsWithDirectivesByScope
        .values
        .flatten
        .filter((path, _) => ScopePath.fromPath(path) != ScopePath.fromPath(projectFilePath))
        .foreach { (_, directivesPositions) =>
          logger.diagnostic(
            s"Using directives detected in multiple files. It is recommended to keep them centralized in the $projectFilePath file.",
            positions = Seq(directivesPositions)
          )
        }
    }

    val paths            = pathsWithDirectivePositions.map(_._1)
    val inMemory         = inMemoryWithDirectivePositions.map(_._1)
    val unwrappedScripts = unwrappedScriptsWithDirectivePositions.map(_._1)
    (
      UnwrappedCrossSources(
        paths,
        inMemory,
        defaultMainClassOpt,
        resourceDirs,
        buildOptions,
        unwrappedScripts
      ),
      allInputs
    )
  }

  private def resolveInputsFromSources(sources: Seq[Positioned[os.Path]], enableMarkdown: Boolean) =
    sources.map { source =>
      val sourcePath   = source.value
      lazy val dir     = sourcePath / os.up
      lazy val subPath = sourcePath.subRelativeTo(dir)
      if (os.isDir(sourcePath))
        Right(Directory(sourcePath).singleFilesFromDirectory(enableMarkdown))
      else if (sourcePath == os.sub / Constants.projectFileName)
        Right(Seq(ProjectScalaFile(dir, subPath)))
      else if (sourcePath.ext == "scala") Right(Seq(SourceScalaFile(dir, subPath)))
      else if (sourcePath.ext == "sc") Right(Seq(Script(dir, subPath, None)))
      else if (sourcePath.ext == "java") Right(Seq(JavaFile(dir, subPath)))
      else if (sourcePath.ext == "jar") Right(Seq(JarFile(dir, subPath)))
      else if (sourcePath.ext == "md") Right(Seq(MarkdownFile(dir, subPath)))
      else {
        val msg =
          if (os.exists(sourcePath))
            s"$sourcePath: unrecognized source type (expected .scala, .sc, .java extension or directory) in using directive."
          else s"$sourcePath: not found path defined in using directive."
        Left(new MalformedDirectiveError(msg, source.positions))
      }
    }.sequence
      .left.map(CompositeBuildException(_))
      .map(_.flatten)

  /** Filters out the sources from the input sequence based on the provided 'exclude' patterns. The
    * exclude patterns can be absolute paths, relative paths, or glob patterns.
    *
    * @throws BuildException
    *   If multiple 'exclude' patterns are defined across the input sources.
    */
  private def excludeSources[E <: Element](
    elements: Seq[E],
    workspaceDir: os.Path,
    exclude: Seq[Positioned[String]]
  ): Either[BuildException, Seq[E]] = either {
    val excludePatterns = exclude.map(_.value).flatMap { p =>
      val maybeRelPath = Try(os.RelPath(p)).toOption
      maybeRelPath match {
        case Some(relPath) if os.isDir(workspaceDir / relPath) =>
          // exclude relative directory paths, add * to exclude all files in the directory
          Seq(p, (workspaceDir / relPath / "*").toString)
        case Some(relPath) =>
          Seq(p, (workspaceDir / relPath).toString) // exclude relative paths
        case None => Seq(p)
      }
    }

    def isSourceIncluded(path: String, excludePatterns: Seq[String]): Boolean =
      excludePatterns
        .forall(pattern => !RegexUtils.globPattern(pattern).matcher(path).matches())

    elements.filter {
      case e: OnDisk => isSourceIncluded(e.path.toString, excludePatterns)
      case _         => true
    }
  }

  /** Validates that exclude directives are defined only in the one source.
    */
  def validateExcludeDirectives(
    sources: Seq[PreprocessedSource],
    workspaceDir: os.Path
  ): Either[BuildException, Seq[PreprocessedSource]] = {
    val excludeDirectives = sources.flatMap(_.options).map(_.internal.exclude).toList.flatten

    excludeDirectives match {
      case Nil | Seq(_) =>
        Right(sources)
      case _ =>
        val expectedProjectFilePath = workspaceDir / Constants.projectFileName
        Left(new ExcludeDefinitionError(
          excludeDirectives.flatMap(_.positions),
          expectedProjectFilePath
        ))
    }
  }
}
