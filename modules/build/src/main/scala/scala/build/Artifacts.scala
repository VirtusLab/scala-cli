package scala.build

import coursier.cache.FileCache
import coursier.core.Classifier
import coursier.parse.RepositoryParser
import coursier.{Dependency => CsDependency, Fetch, core => csCore, util => csUtil}
import dependency._

import java.nio.file.Path

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  FetchingDependenciesError,
  RepositoryFormatError
}
import scala.build.internal.Constants
import scala.build.internal.Constants._
import scala.build.internal.Util.ScalaDependencyOps

final case class Artifacts(
  compilerDependencies: Seq[AnyDependency],
  compilerArtifacts: Seq[(String, Path)],
  compilerPlugins: Seq[(AnyDependency, String, Path)],
  dependencies: Seq[AnyDependency],
  detailedArtifacts: Seq[(CsDependency, csCore.Publication, csUtil.Artifact, Path)],
  extraClassPath: Seq[Path],
  extraCompileOnlyJars: Seq[Path],
  extraSourceJars: Seq[Path],
  params: ScalaParameters
) {
  lazy val artifacts: Seq[(String, Path)] =
    detailedArtifacts
      .iterator
      .collect {
        case (_, pub, a, f) if pub.classifier != Classifier.sources =>
          (a.url, f)
      }
      .toVector
  lazy val sourceArtifacts: Seq[(String, Path)] =
    detailedArtifacts
      .iterator
      .collect {
        case (_, pub, a, f) if pub.classifier == Classifier.sources =>
          (a.url, f)
      }
      .toVector
  lazy val compilerClassPath: Seq[Path] =
    compilerArtifacts.map(_._2)
  lazy val classPath: Seq[Path] =
    artifacts.map(_._2) ++ extraClassPath
  lazy val compileClassPath: Seq[Path] =
    artifacts.map(_._2) ++ extraClassPath ++ extraCompileOnlyJars
  lazy val sourcePath: Seq[Path] =
    sourceArtifacts.map(_._2) ++ extraSourceJars
}

object Artifacts {

  def apply(
    params: ScalaParameters,
    compilerPlugins: Seq[Positioned[AnyDependency]],
    dependencies: Seq[Positioned[AnyDependency]],
    extraClassPath: Seq[Path],
    extraCompileOnlyJars: Seq[Path],
    extraSourceJars: Seq[Path],
    fetchSources: Boolean,
    addStubs: Boolean,
    addJvmRunner: Option[Boolean],
    addJvmTestRunner: Boolean,
    addJsTestBridge: Option[String],
    addJmhDependencies: Option[String],
    extraRepositories: Seq[String],
    logger: Logger
  ): Either[BuildException, Artifacts] = either {

    val compilerDependencies =
      if (params.scalaVersion.startsWith("3."))
        Seq(
          dep"org.scala-lang::scala3-compiler:${params.scalaVersion}"
        )
      else
        Seq(
          dep"org.scala-lang:scala-compiler:${params.scalaVersion}"
        )

    val jvmRunnerDependencies =
      if (addJvmRunner.getOrElse(true))
        Seq(dep"$runnerOrganization::$runnerModuleName:$runnerVersion")
      else
        Nil
    val jvmTestRunnerDependencies =
      if (addJvmTestRunner)
        Seq(dep"$testRunnerOrganization::$testRunnerModuleName:$testRunnerVersion")
      else
        Nil
    val jsTestBridgeDependencies = addJsTestBridge.toSeq.map { scalaJsVersion =>
      if (params.scalaVersion.startsWith("2."))
        dep"org.scala-js::scalajs-test-bridge:$scalaJsVersion"
      else
        dep"org.scala-js:scalajs-test-bridge_2.13:$scalaJsVersion"
    }

    val jmhDependencies = addJmhDependencies.toSeq.map { version =>
      dep"org.openjdk.jmh:jmh-generator-bytecode:$version"
    }

    val maybeSnapshotRepo = {
      val hasSnapshots = (jvmRunnerDependencies ++ jvmTestRunnerDependencies)
        .exists(_.version.endsWith("SNAPSHOT"))
      val runnerNeedsSonatypeSnapshots = Constants.runnerNeedsSonatypeSnapshots(params.scalaVersion)
      if (hasSnapshots || runnerNeedsSonatypeSnapshots)
        Seq(coursier.Repositories.sonatype("snapshots").root)
      else
        Nil
    }

    val allExtraRepositories = maybeSnapshotRepo ++ extraRepositories

    val updatedDependencies =
      dependencies ++
        jvmRunnerDependencies.map(Positioned.none(_)) ++
        jvmTestRunnerDependencies.map(Positioned.none(_)) ++
        jsTestBridgeDependencies.map(Positioned.none(_)) ++
        jmhDependencies.map(Positioned.none(_))

    val compilerArtifacts = value {
      artifacts(Positioned.none(compilerDependencies), allExtraRepositories, params, logger)
    }

    val fetchRes = value {
      fetch(
        Positioned.sequence(updatedDependencies),
        allExtraRepositories,
        params,
        logger,
        classifiersOpt = Some(Set("_") ++ (if (fetchSources) Set("sources") else Set.empty))
      )
    }

    val extraStubsJars =
      if (addStubs)
        value {
          artifacts(
            Positioned.none(Seq(dep"$stubsOrganization:$stubsModuleName:$stubsVersion")),
            allExtraRepositories,
            params,
            logger
          ).map(_.map(_._2))
        }
      else
        Nil

    val compilerPlugins0 = value {
      compilerPlugins
        .map { posDep =>
          val posDep0 =
            posDep.map(dep => dep.copy(userParams = dep.userParams + ("intransitive" -> None)))
          artifacts(posDep0.map(Seq(_)), allExtraRepositories, params, logger)
            .map(_.map { case (url, path) => (posDep0.value, url, path) })
        }
        .sequence
        .left.map(CompositeBuildException(_))
        .map(_.flatten)
    }

    Artifacts(
      compilerDependencies,
      compilerArtifacts,
      compilerPlugins0,
      updatedDependencies.map(_.value),
      fetchRes.fullDetailedArtifacts.collect { case (d, p, a, Some(f)) => (d, p, a, f.toPath) },
      extraClassPath ++ extraStubsJars,
      extraCompileOnlyJars,
      extraSourceJars,
      params
    )
  }

  private[build] def artifacts(
    dependencies: Positioned[Seq[AnyDependency]],
    extraRepositories: Seq[String],
    params: ScalaParameters,
    logger: Logger,
    classifiersOpt: Option[Set[String]] = None
  ): Either[BuildException, Seq[(String, Path)]] = either {
    val result = value(fetch(dependencies, extraRepositories, params, logger, classifiersOpt))
      .artifacts
      .iterator
      .map { case (a, f) => (a.url, f.toPath) }
      .toList
    logger.debug {
      val elems = Seq(s"Found ${result.length} artifacts:") ++
        result.map("  " + _._2) ++
        Seq("")
      elems.mkString(System.lineSeparator())
    }
    result
  }

  private def fetch(
    dependencies: Positioned[Seq[AnyDependency]],
    extraRepositories: Seq[String],
    params: ScalaParameters,
    logger: Logger,
    classifiersOpt: Option[Set[String]]
  ): Either[BuildException, Fetch.Result] = either {
    logger.debug {
      s"Fetching ${dependencies.value}" +
        (if (extraRepositories.isEmpty) "" else s", adding $extraRepositories")
    }

    val extraRepositories0 = value {
      RepositoryParser.repositories(extraRepositories)
        .either
        .left.map(errors => new RepositoryFormatError(errors))
    }

    val cache = FileCache().withLogger(logger.coursierLogger)

    // FIXME Many parameters that we could allow to customize here
    var fetcher = coursier.Fetch()
      .withCache(cache)
      .addRepositories(extraRepositories0: _*)
      .addDependencies(dependencies.value.map(_.toCs(params)): _*)
    for (classifiers <- classifiersOpt) {
      if (classifiers("_"))
        fetcher = fetcher.withMainArtifacts()
      fetcher = fetcher
        .addClassifiers(classifiers.toSeq.filter(_ != "_").map(coursier.Classifier(_)): _*)
    }

    value {
      fetcher.eitherResult()
        .left.map(ex => new FetchingDependenciesError(ex, dependencies.positions))
    }
  }

}
