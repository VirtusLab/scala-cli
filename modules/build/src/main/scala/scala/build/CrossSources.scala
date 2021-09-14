package scala.build

import scala.build.internal.CodeWrapper
import scala.build.options.{BuildOptions, BuildRequirements, HasBuildRequirements}
import scala.build.preprocessing._

final case class CrossSources(
  paths: Seq[HasBuildRequirements[(os.Path, os.RelPath)]],
  inMemory: Seq[HasBuildRequirements[(Either[String, os.Path], os.RelPath, String, Int)]],
  mainClass: Option[String],
  resourceDirs: Seq[os.Path],
  buildOptions: Seq[HasBuildRequirements[(Either[String, os.Path], BuildOptions)]]
) {

  def sources(baseOptions: BuildOptions): Sources = {

    val sharedOptions = buildOptions
      .filter(_.requirements.isEmpty)
      .map(_.value._2)
      .foldLeft(baseOptions)(_ orElse _)

    val retainedScalaVersion = sharedOptions.scalaParams.scalaVersion

    val buildOptionsWithScalaVersion = buildOptions
      .flatMap(_.withScalaVersion(retainedScalaVersion).toSeq)
      .filter(_.requirements.isEmpty)
      .map(_.value._2)
      .foldLeft(sharedOptions)(_ orElse _)

    val platform =
      if (buildOptionsWithScalaVersion.scalaJsOptions.enable)
        BuildRequirements.Platform.JS
      else if (buildOptionsWithScalaVersion.scalaNativeOptions.enable)
        BuildRequirements.Platform.Native
      else
        BuildRequirements.Platform.JVM

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
        .map(_.value._2)
        .foldLeft(BuildOptions() /* not baseOptions */ )(_ orElse _)
    )
  }

}

object CrossSources {

  def forInputs(
    inputs: Inputs,
    preprocessors: Seq[Preprocessor]
  ): CrossSources = {

    val preprocessedSources = inputs.flattened().flatMap { elem =>
      preprocessors.iterator.flatMap(p => p.preprocess(elem).iterator).toStream.headOption
        .getOrElse(Nil) // FIXME Warn about unprocessed stuff?
    }

    val buildOptions = preprocessedSources.flatMap {
      case d: PreprocessedSource.OnDisk =>
        d.options.toSeq.map { opt =>
          HasBuildRequirements(
            d.requirements.getOrElse(BuildRequirements()),
            (Right(d.path), opt)
          )
        }
      case m: PreprocessedSource.InMemory =>
        m.options.toSeq.map { opt =>
          HasBuildRequirements(
            m.requirements.getOrElse(BuildRequirements()),
            (m.reportingPath, opt)
          )
        }
      case n: PreprocessedSource.NoSourceCode =>
        val elem = HasBuildRequirements(
          n.requirements.getOrElse(BuildRequirements()),
          (Right(n.path), n.options.getOrElse(BuildOptions()))
        )
        Seq(elem)
      case _ =>
        Nil
    }

    val mainClassOpt = inputs.mainClassElement
      .collect {
        case elem: Inputs.SingleElement =>
          preprocessors.iterator
            .flatMap(p => p.preprocess(elem).iterator)
            .toStream.headOption
            .getOrElse(Nil)
            .flatMap(_.mainClassOpt.toSeq)
            .headOption
      }
      .flatten

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
