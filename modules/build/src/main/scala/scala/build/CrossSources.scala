package scala.build

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.options.{BuildOptions, BuildRequirements, HasBuildRequirements, Platform}
import scala.build.preprocessing._

final case class CrossSources(
  paths: Seq[HasBuildRequirements[(os.Path, os.RelPath)]],
  inMemory: Seq[HasBuildRequirements[(Either[String, os.Path], os.RelPath, String, Int)]],
  mainClass: Option[String],
  resourceDirs: Seq[os.Path],
  buildOptions: Seq[HasBuildRequirements[BuildOptions]]
) {

  def sources(baseOptions: BuildOptions): Sources = {

    val sharedOptions = buildOptions
      .filter(_.requirements.isEmpty)
      .map(_.value)
      .foldLeft(baseOptions)(_ orElse _)

    val retainedScalaVersion = sharedOptions.scalaParams.scalaVersion

    val buildOptionsWithScalaVersion = buildOptions
      .flatMap(_.withScalaVersion(retainedScalaVersion).toSeq)
      .filter(_.requirements.isEmpty)
      .map(_.value)
      .foldLeft(sharedOptions)(_ orElse _)

    val platform =
      if (buildOptionsWithScalaVersion.scalaJsOptions.enable)
        Platform.JS
      else if (buildOptionsWithScalaVersion.scalaNativeOptions.enable)
        Platform.Native
      else
        Platform.JVM

    // FIXME Not 100% sure the way we compute the intermediate and final BuildOptions
    // is consistent (we successively filter out / retain options to compute a scala
    // version and platform, which might not be the version and platform of the final
    // BuildOptions).

    Sources(
      paths
        .flatMap(_.withScalaVersion(retainedScalaVersion).toSeq)
        .flatMap(_.withPlatform(platform).toSeq)
        .map(_.value),
      inMemory
        .flatMap(_.withScalaVersion(retainedScalaVersion).toSeq)
        .flatMap(_.withPlatform(platform).toSeq)
        .map(_.value),
      mainClass,
      resourceDirs,
      buildOptions
        .flatMap(_.withScalaVersion(retainedScalaVersion).toSeq)
        .flatMap(_.withPlatform(platform).toSeq)
        .map(_.value)
        .foldLeft(BuildOptions() /* not baseOptions */ )(_ orElse _)
    )
  }

}

object CrossSources {

  def forInputs(
    inputs: Inputs,
    preprocessors: Seq[Preprocessor]
  ): Either[BuildException, CrossSources] = either {

    val preprocessedSources = value {
      inputs.flattened()
        .map { elem =>
          preprocessors.iterator.flatMap(p => p.preprocess(elem).iterator).toStream.headOption
            .getOrElse(Right(Nil)) // FIXME Warn about unprocessed stuff?
        }
        .sequence
        .left.map(CompositeBuildException(_))
        .map(_.flatten)
    }

    val buildOptions = for {
      s   <- preprocessedSources
      opt <- s.options.toSeq
      if opt != BuildOptions()
    } yield {
      HasBuildRequirements(
        s.requirements.getOrElse(BuildRequirements()),
        opt
      )
    }

    val mainClassOpt = value {
      inputs.mainClassElement
        .collect {
          case elem: Inputs.SingleElement =>
            preprocessors.iterator
              .flatMap(p => p.preprocess(elem).iterator)
              .toStream.headOption
              .getOrElse(Right(Nil))
              .map(_.flatMap(_.mainClassOpt.toSeq).headOption)
        }
        .sequence
        .map(_.flatten)
    }

    val paths = preprocessedSources.collect {
      case d: PreprocessedSource.OnDisk =>
        HasBuildRequirements(
          d.requirements.getOrElse(BuildRequirements()),
          (d.path, d.path.relativeTo(inputs.workspace))
        )
    }
    val inMemory = preprocessedSources.collect {
      case m: PreprocessedSource.InMemory =>
        HasBuildRequirements(
          m.requirements.getOrElse(BuildRequirements()),
          (m.reportingPath, m.relPath, m.code, m.ignoreLen)
        )
    }

    val resourceDirs = inputs.elements.collect {
      case r: Inputs.ResourceDirectory =>
        r.path
    }

    CrossSources(paths, inMemory, mainClassOpt, resourceDirs, buildOptions)
  }
}
