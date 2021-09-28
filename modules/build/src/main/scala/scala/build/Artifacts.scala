package scala.build

import coursier.cache.FileCache
import coursier.core.Classifier
import coursier.parse.RepositoryParser
import coursier.{Dependency => CsDependency, Fetch, core => csCore, util => csUtil}
import dependency._

import java.nio.file.Path

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.internal.Constants
import scala.build.internal.Constants._
import scala.build.internal.Util.ScalaDependencyOps
import scala.util.control.NonFatal

final case class Artifacts(
  compilerDependencies: Seq[AnyDependency],
  compilerArtifacts: Seq[(String, Path)],
  compilerPlugins: Seq[(AnyDependency, String, Path)],
  dependencies: Seq[AnyDependency],
  detailedArtifacts: Seq[(CsDependency, csCore.Publication, csUtil.Artifact, Path)],
  extraJars: Seq[Path],
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
    artifacts.map(_._2) ++ extraJars
  lazy val compileClassPath: Seq[Path] =
    artifacts.map(_._2) ++ extraJars ++ extraCompileOnlyJars
  lazy val sourcePath: Seq[Path] =
    sourceArtifacts.map(_._2) ++ extraSourceJars
}

object Artifacts {

  def apply(
    params: ScalaParameters,
    compilerPlugins: Seq[AnyDependency],
    dependencies: Seq[AnyDependency],
    extraJars: Seq[Path],
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

    val updatedDependencies = dependencies ++
      jvmRunnerDependencies ++
      jvmTestRunnerDependencies ++
      jsTestBridgeDependencies ++
      jmhDependencies

    val compilerArtifacts = value {
      artifacts(compilerDependencies, allExtraRepositories, params, logger)
    }

    val fetchRes = value {
      fetch(
        updatedDependencies,
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
            Seq(dep"$stubsOrganization:$stubsModuleName:$stubsVersion"),
            allExtraRepositories,
            params,
            logger
          ).map(_.map(_._2))
        }
      else
        Nil

    val compilerPlugins0 = value {
      compilerPlugins
        .map { dep =>
          val dep0 = dep.copy(userParams = dep.userParams + ("intransitive" -> None))
          artifacts(Seq(dep0), allExtraRepositories, params, logger)
            .map(_.map { case (url, path) => (dep0, url, path) })
        }
        .sequence
        .left.map(CompositeBuildException(_))
        .map(_.flatten)
    }

    Artifacts(
      compilerDependencies,
      compilerArtifacts,
      compilerPlugins0,
      updatedDependencies,
      fetchRes.fullDetailedArtifacts.collect { case (d, p, a, Some(f)) => (d, p, a, f.toPath) },
      extraJars ++ extraStubsJars,
      extraCompileOnlyJars,
      extraSourceJars,
      params
    )
  }

  private[build] def artifacts(
    dependencies: Seq[AnyDependency],
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

  private[build] def fetch(
    dependencies: Seq[AnyDependency],
    extraRepositories: Seq[String],
    params: ScalaParameters,
    logger: Logger,
    classifiersOpt: Option[Set[String]]
  ): Either[BuildException, Fetch.Result] = {
    logger.debug {
      s"Fetching $dependencies" +
        (if (extraRepositories.isEmpty) "" else s", adding $extraRepositories")
    }

    val extraRepositories0 = RepositoryParser.repositories(extraRepositories).either match {
      case Left(errors) => sys.error(s"Error parsing repositories: ${errors.mkString(", ")}")
      case Right(repos) => repos
    }

    val cache = FileCache().withLogger(logger.coursierLogger)

    // FIXME Many parameters that we could allow to customize here
    var fetcher = coursier.Fetch()
      .withCache(cache)
      .addRepositories(extraRepositories0: _*)
      .addDependencies(dependencies.map(_.toCs(params)): _*)
    for (classifiers <- classifiersOpt) {
      if (classifiers("_"))
        fetcher = fetcher.withMainArtifacts()
      fetcher = fetcher
        .addClassifiers(classifiers.toSeq.filter(_ != "_").map(coursier.Classifier(_)): _*)
    }

    try Right(fetcher.runResult())
    catch {
      case NonFatal(e) =>
        throw new Exception(e)
    }
  }

}
