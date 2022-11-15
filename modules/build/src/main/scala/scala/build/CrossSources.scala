package scala.build

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.Positioned
import scala.build.errors.{BuildException, CompositeBuildException, MalformedDirectiveError}
import scala.build.input.ElementsUtils.*
import scala.build.input.*
import scala.build.internal.Constants
import scala.build.options.{
  BuildOptions,
  BuildRequirements,
  HasBuildRequirements,
  MaybeScalaVersion,
  Scope
}
import scala.build.preprocessing.*

final case class CrossSources(
  paths: Seq[HasBuildRequirements[(os.Path, os.RelPath)]],
  inMemory: Seq[HasBuildRequirements[Sources.InMemory]],
  defaultMainClass: Option[String],
  resourceDirs: Seq[HasBuildRequirements[os.Path]],
  buildOptions: Seq[HasBuildRequirements[BuildOptions]]
) {

  def sharedOptions(baseOptions: BuildOptions): BuildOptions =
    buildOptions
      .filter(_.requirements.isEmpty)
      .map(_.value)
      .foldLeft(baseOptions)(_ orElse _)

  private def needsScalaVersion =
    paths.exists(_.needsScalaVersion) ||
    inMemory.exists(_.needsScalaVersion) ||
    resourceDirs.exists(_.needsScalaVersion) ||
    buildOptions.exists(_.needsScalaVersion)

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
      val fullPath = path / p.path
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
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e)
  ): Either[BuildException, (CrossSources, Inputs)] = either {

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
                inputs.allowRestrictedFeatures
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

    val preprocessedInputFromArgs = value(preprocessSources(inputs.flattened()))

    val sourcesFromDirectives =
      preprocessedInputFromArgs
        .flatMap(_.options)
        .flatMap(_.internal.extraSourceFiles)
        .distinct
    val inputsElemFromDirectives =
      value(resolveInputsFromSources(sourcesFromDirectives, inputs.enableMarkdown))
    val preprocessedSourcesFromDirectives = value(preprocessSources(inputsElemFromDirectives))
    val allInputs                         = inputs.add(inputsElemFromDirectives)

    val preprocessedSources =
      (preprocessedInputFromArgs ++ preprocessedSourcesFromDirectives).distinct

    val preprocessedWithUsingDirs = preprocessedSources.filter(_.directivesPositions.isDefined)
    if (preprocessedWithUsingDirs.length > 1) {
      val projectFilePath = inputs.elements.projectSettingsFiles.headOption match
        case Some(s) => s.path
        case _       => inputs.workspace / Constants.projectFileName
      preprocessedWithUsingDirs
        .filter(_.scopePath != ScopePath.fromPath(projectFilePath))
        .foreach { source =>
          source.directivesPositions match
            case Some(positions) =>
              val pos = Seq(
                positions.codeDirectives,
                positions.specialCommentDirectives,
                positions.plainCommentDirectives
              ).maxBy(p => p.endPos._1)
              logger.diagnostic(
                s"Using directives detected in multiple files. It is recommended to keep them centralized in the $projectFilePath file.",
                positions = Seq(pos)
              )
            case _ => ()
        }
    }

    val scopedRequirements       = preprocessedSources.flatMap(_.scopedRequirements)
    val scopedRequirementsByRoot = scopedRequirements.groupBy(_.path.root)
    def baseReqs(path: ScopePath): BuildRequirements = {
      val fromDirectives =
        scopedRequirementsByRoot
          .getOrElse(path.root, Nil)
          .flatMap(_.valueFor(path).toSeq)
          .foldLeft(BuildRequirements())(_ orElse _)

      // Scala CLI treats all `.test.scala` files tests as well as
      // files from witin `test` subdirectory from provided input directories
      // If file has `using target <scope>` directive this take precendeces.
      if (
        fromDirectives.scope.isEmpty &&
        (path.path.last.endsWith(".test.scala") || withinTestSubDirectory(path, allInputs))
      )
        fromDirectives.copy(scope = Some(BuildRequirements.ScopeRequirement(Scope.Test)))
      else fromDirectives
    }

    val buildOptions = for {
      s   <- preprocessedSources
      opt <- s.options.toSeq
      if opt != BuildOptions()
    } yield {
      val baseReqs0 = baseReqs(s.scopePath)
      HasBuildRequirements(
        s.requirements.fold(baseReqs0)(_ orElse baseReqs0),
        opt
      )
    }

    val defaultMainClassOpt = for {
      mainClassPath <- allInputs.defaultMainClassElement.map(s => ScopePath.fromPath(s.path).path)
      processedMainClass <- preprocessedSources.find(_.scopePath.path == mainClassPath)
      mainClass          <- processedMainClass.mainClassOpt
    } yield mainClass

    val paths = preprocessedSources.collect {
      case d: PreprocessedSource.OnDisk =>
        val baseReqs0 = baseReqs(d.scopePath)
        HasBuildRequirements(
          d.requirements.fold(baseReqs0)(_ orElse baseReqs0),
          (d.path, d.path.relativeTo(allInputs.workspace))
        )
    }
    val inMemory = preprocessedSources.collect {
      case m: PreprocessedSource.InMemory =>
        val baseReqs0 = baseReqs(m.scopePath)
        HasBuildRequirements(
          m.requirements.fold(baseReqs0)(_ orElse baseReqs0),
          Sources.InMemory(m.originalPath, m.relPath, m.code, m.ignoreLen)
        )
    }

    val resourceDirs = allInputs.elements.collect {
      case r: ResourceDirectory =>
        HasBuildRequirements(BuildRequirements(), r.path)
    } ++ preprocessedSources.flatMap(_.options).flatMap(_.classPathOptions.resourcesDir).map(
      HasBuildRequirements(BuildRequirements(), _)
    )

    (CrossSources(paths, inMemory, defaultMainClassOpt, resourceDirs, buildOptions), allInputs)
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
      else if (sourcePath.ext == "sc") Right(Seq(Script(dir, subPath)))
      else if (sourcePath.ext == "java") Right(Seq(JavaFile(dir, subPath)))
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

}
