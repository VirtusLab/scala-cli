package scala.build

import coursier.cache.FileCache
import coursier.cache.loggers.RefreshLogger
import coursier.parse.RepositoryParser
import _root_.dependency._
import scala.build.internal.Constants
import scala.build.internal.Constants._

import java.nio.file.Path

import scala.build.internal.Util.ScalaDependencyOps
import scala.collection.JavaConverters._

final case class Artifacts(
  compilerDependencies: Seq[AnyDependency],
  compilerArtifacts: Seq[(String, Path)],
  compilerPlugins: Seq[(AnyDependency, String, Path)],
  dependencies: Seq[AnyDependency],
  artifacts: Seq[(String, Path)],
  sourceArtifacts: Seq[(String, Path)],
  extraJars: Seq[Path],
  params: ScalaParameters
) {
  lazy val compilerClassPath: Seq[Path] =
    compilerArtifacts.map(_._2)
  lazy val classPath: Seq[Path] =
    artifacts.map(_._2) ++ extraJars
  lazy val sourcePath: Seq[Path] =
    sourceArtifacts.map(_._2)
}

object Artifacts {

  def apply(
    javaHomeOpt: Option[String],
    jvmIdOpt: Option[String],
    params: ScalaParameters,
    compilerPlugins: Seq[AnyDependency],
    dependencies: Seq[AnyDependency],
    extraJars: Seq[Path],
    fetchSources: Boolean,
    addStubs: Boolean,
    addJvmRunner: Boolean,
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
      if (addJvmRunner) Seq(dep"$runnerOrganization::$runnerModuleName:$runnerVersion")
      else Nil
    val jvmTestRunnerDependencies =
      if (addJvmTestRunner) Seq(dep"$testRunnerOrganization::$testRunnerModuleName:$testRunnerVersion")
      else Nil
    val jsTestBridgeDependencies = addJsTestBridge.toSeq.map { scalaJsVersion =>
      dep"org.scala-js::scalajs-test-bridge:$scalaJsVersion"
    }

    val jmhDependencies = addJmhDependencies.toSeq.map { version =>
      dep"org.openjdk.jmh:jmh-generator-bytecode:$version"
    }

    val maybeSnapshotRepo =
      if ((jvmRunnerDependencies ++ jvmTestRunnerDependencies).exists(_.version.endsWith("SNAPSHOT")))
        Seq(coursier.Repositories.sonatype("snapshots").root)
      else Nil

    val allExtraRepositories = maybeSnapshotRepo ++ extraRepositories

    val updatedDependencies = dependencies ++
      jvmRunnerDependencies ++
      jvmTestRunnerDependencies ++
      jsTestBridgeDependencies ++
      jmhDependencies

    val compilerArtifacts = artifacts(compilerDependencies, allExtraRepositories, params, logger)
    val artifacts0 = artifacts(updatedDependencies, allExtraRepositories, params, logger)

    val sourceArtifacts =
      if (fetchSources) artifacts(updatedDependencies, allExtraRepositories, params, logger, classifiersOpt = Some(Set("sources")))
      else Nil

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
      artifacts0,
      sourceArtifacts,
      extraJars ++ extraStubsJars,
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
    for (classifiers <- classifiersOpt)
      fetcher = fetcher.addClassifiers(classifiers.toSeq.map(coursier.Classifier(_)): _*)

    val result = fetcher.runResult()
      .artifacts
      .iterator
      .map { case (a, f) => (a.url, f.toPath) }
      .toList
    logger.debug((Seq(s"Found ${result.length} artifacts:") ++ result.map("  " + _._2) ++ Seq("")).mkString(System.lineSeparator()))
    result
  }

}
