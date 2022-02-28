package scala.build

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.options.{BuildOptions, BuildRequirements, HasBuildRequirements, Scope}
import scala.build.preprocessing._

final case class CrossSources(
  paths: Seq[HasBuildRequirements[(os.Path, os.RelPath)]],
  inMemory: Seq[HasBuildRequirements[Sources.InMemory]],
  mainClass: Option[String],
  resourceDirs: Seq[HasBuildRequirements[os.Path]],
  buildOptions: Seq[HasBuildRequirements[BuildOptions]]
) {

  def sharedOptions(baseOptions: BuildOptions): BuildOptions =
    buildOptions
      .filter(_.requirements.isEmpty)
      .map(_.value)
      .foldLeft(baseOptions)(_ orElse _)

  def scopedSources(baseOptions: BuildOptions): Either[BuildException, ScopedSources] = either {

    val sharedOptions0 = sharedOptions(baseOptions)

    val retainedScalaVersion = value(sharedOptions0.scalaParams).scalaVersion

    val buildOptionsWithScalaVersion = buildOptions
      .flatMap(_.withScalaVersion(retainedScalaVersion).toSeq)
      .filter(_.requirements.isEmpty)
      .map(_.value)
      .foldLeft(sharedOptions0)(_ orElse _)

    val platform = buildOptionsWithScalaVersion.platform

    // FIXME Not 100% sure the way we compute the intermediate and final BuildOptions
    // is consistent (we successively filter out / retain options to compute a scala
    // version and platform, which might not be the version and platform of the final
    // BuildOptions).

    val defaultScope: Scope = Scope.Main
    ScopedSources(
      paths
        .flatMap(_.withScalaVersion(retainedScalaVersion).toSeq)
        .flatMap(_.withPlatform(platform.value).toSeq)
        .map(_.scopedValue(defaultScope)),
      inMemory
        .flatMap(_.withScalaVersion(retainedScalaVersion).toSeq)
        .flatMap(_.withPlatform(platform.value).toSeq)
        .map(_.scopedValue(defaultScope)),
      mainClass,
      resourceDirs
        .flatMap(_.withScalaVersion(retainedScalaVersion).toSeq)
        .flatMap(_.withPlatform(platform.value).toSeq)
        .map(_.scopedValue(defaultScope)),
      buildOptions
        .filter(!_.requirements.isEmpty)
        .flatMap(_.withScalaVersion(retainedScalaVersion).toSeq)
        .flatMap(_.withPlatform(platform.value).toSeq)
        .map(_.scopedValue(defaultScope))
    )
  }

}

object CrossSources {

  private def withinTestSubDirectory(p: ScopePath, inputs: Inputs): Boolean =
    p.root.exists { path =>
      val fullPath = path / p.path
      inputs.elements.exists {
        case Inputs.Directory(path) =>
          // Is this file subdirectory of given dir and if we have a subdiretory 'test' on the way
          fullPath.startsWith(path) &&
            fullPath.relativeTo(path).segments.contains("test")
        case _ => false
      }
    }

  def forInputs(
    inputs: Inputs,
    preprocessors: Seq[Preprocessor],
    logger: Logger
  ): Either[BuildException, CrossSources] = either {

    val preprocessedSources = value {
      inputs.flattened()
        .map { elem =>
          preprocessors
            .iterator
            .flatMap(p => p.preprocess(elem, logger).iterator)
            .take(1)
            .toList
            .headOption
            .getOrElse(Right(Nil)) // FIXME Warn about unprocessed stuff?
        }
        .sequence
        .left.map(CompositeBuildException(_))
        .map(_.flatten)
    }

    val scopedRequirements       = preprocessedSources.flatMap(_.scopedRequirements)
    val scopedRequirementsByRoot = scopedRequirements.groupBy(_.path.root)
    def baseReqs(path: ScopePath): BuildRequirements = {
      val fromDirectives =
        scopedRequirementsByRoot
          .getOrElse(path.root, Nil)
          .flatMap(_.valueFor(path).toSeq)
          .foldLeft(BuildRequirements())(_ orElse _)

      // Scala-cli treats all `.test.scala` files tests as well as
      // files from witin `test` subdirectory from provided input directories
      // If file has `using target <scope>` directive this take precendeces.
      if (
        fromDirectives.scope.isEmpty &&
        (path.path.last.endsWith(".test.scala") || withinTestSubDirectory(path, inputs))
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

    val mainClassOpt = for {
      mainClassPath      <- inputs.mainClassElement.map(_.path).map(ScopePath.fromPath(_).path)
      processedMainClass <- preprocessedSources.find(_.scopePath.path == mainClassPath)
      mainClass          <- processedMainClass.mainClassOpt
    } yield mainClass

    val paths = preprocessedSources.collect {
      case d: PreprocessedSource.OnDisk =>
        val baseReqs0 = baseReqs(d.scopePath)
        HasBuildRequirements(
          d.requirements.fold(baseReqs0)(_ orElse baseReqs0),
          (d.path, d.path.relativeTo(inputs.workspace))
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

    val resourceDirs = inputs.elements.collect {
      case r: Inputs.ResourceDirectory =>
        HasBuildRequirements(BuildRequirements(), r.path)
    } ++ preprocessedSources.flatMap(_.options).flatMap(_.classPathOptions.resourcesDir).map(
      HasBuildRequirements(BuildRequirements(), _)
    )

    CrossSources(paths, inMemory, mainClassOpt, resourceDirs, buildOptions)
  }
}
