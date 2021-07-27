package scala.build

import coursier.cache.FileCache
import coursier.cache.loggers.RefreshLogger
import coursier.core.Classifier
import coursier.Fetch
import coursier.parse.RepositoryParser
import _root_.dependency._
import scala.build.internal.Constants
import scala.build.internal.Constants._

import java.nio.file.Path

import scala.build.internal.Util.ScalaDependencyOps
import scala.collection.JavaConverters._
import scala.util.control.NonFatal

final case class Artifacts(
  compilerDependencies: Seq[AnyDependency],
  compilerArtifacts: Seq[(String, Path)],
  compilerPlugins: Seq[(AnyDependency, String, Path)],
  dependencies: Seq[AnyDependency],
  detailedArtifacts: Seq[(coursier.Dependency, coursier.core.Publication, coursier.util.Artifact, Path)],
  extraJars: Seq[Path],
  extraCompileOnlyJars: Seq[Path],
  extraSourceJars: Seq[Path],
  params: ScalaParameters
) {
  lazy val artifacts: Seq[(String, Path)] =
    detailedArtifacts
      .iterator
      .collect {
        case (dep, pub, a, f) if pub.classifier != Classifier.sources =>
          (a.url, f)
      }
      .toVector
  lazy val sourceArtifacts: Seq[(String, Path)] =
    detailedArtifacts
      .iterator
      .collect {
        case (dep, pub, a, f) if pub.classifier == Classifier.sources =>
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
  ): Artifacts = {

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
      if (addJvmRunner.getOrElse(true)) Seq(dep"$runnerOrganization::$runnerModuleName:$runnerVersion")
      else Nil
    val jvmTestRunnerDependencies =
      if (addJvmTestRunner) Seq(dep"$testRunnerOrganization::$testRunnerModuleName:$testRunnerVersion")
      else Nil
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
      val hasSnapshots =
        (jvmRunnerDependencies ++ jvmTestRunnerDependencies).exists(_.version.endsWith("SNAPSHOT")) ||
          Constants.runnerNeedsSonatypeSnapshots(params.scalaVersion)
      if (hasSnapshots)
        Seq(coursier.Repositories.sonatype("snapshots").root)
      else Nil
    }

    val allExtraRepositories = maybeSnapshotRepo ++ extraRepositories

    val updatedDependencies = dependencies ++
      jvmRunnerDependencies ++
      jvmTestRunnerDependencies ++
      jsTestBridgeDependencies ++
      jmhDependencies

    val compilerArtifacts = artifacts(compilerDependencies, allExtraRepositories, params, logger)

    val fetchRes = fetch(
      updatedDependencies,
      allExtraRepositories,
      params,
      logger,
      classifiersOpt = Some(Set("_") ++ (if (fetchSources) Set("sources") else Set.empty))
    )
    val artifacts0 = {
      val a = fetchRes.fullExtraArtifacts.iterator.collect { case (a, Some(f)) => (None, a.url, f.toPath) }.toVector ++
        fetchRes.fullDetailedArtifacts.iterator.collect { case (dep, pub, a, Some(f)) if pub.classifier != Classifier.sources => (Some(dep.moduleVersion), a.url, f.toPath) }.toVector
      a.distinct
    }
    val sourceArtifacts =
      if (fetchSources) {
        val a = fetchRes.fullDetailedArtifacts.iterator.collect { case (dep, pub, a, Some(f)) if pub.classifier == Classifier.sources => (a.url, f.toPath) }.toVector
        a.distinct
      } else Nil

    val extraStubsJars =
      if (addStubs)
        artifacts(
          Seq(dep"$stubsOrganization:$stubsModuleName:$stubsVersion"),
          allExtraRepositories,
          params,
          logger
        ).map(_._2)
      else
        Nil

    val compilerPlugins0 = compilerPlugins.flatMap { dep =>
      val dep0 = dep.copy(userParams = dep.userParams + ("intransitive" -> None))
      artifacts(Seq(dep0), allExtraRepositories, params, logger)
        .map { case (url, path) => (dep0, url, path) }
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
  ): Seq[(String, Path)] = {
    val result = fetch(dependencies, extraRepositories, params, logger, classifiersOpt)
      .artifacts
      .iterator
      .map { case (a, f) => (a.url, f.toPath) }
      .toList
    logger.debug((Seq(s"Found ${result.length} artifacts:") ++ result.map("  " + _._2) ++ Seq("")).mkString(System.lineSeparator()))
    result
  }

  private[build] def fetch(
    dependencies: Seq[AnyDependency],
    extraRepositories: Seq[String],
    params: ScalaParameters,
    logger: Logger,
    classifiersOpt: Option[Set[String]]
  ): Fetch.Result = {
    logger.debug(s"Fetching $dependencies" + (if (extraRepositories.isEmpty) "" else s", adding $extraRepositories"))

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
      fetcher = fetcher.addClassifiers(classifiers.toSeq.filter(_ != "_").map(coursier.Classifier(_)): _*)
    }

    try fetcher.runResult()
    catch {
      case NonFatal(e) =>
        throw new Exception(e)
    }
  }

}
