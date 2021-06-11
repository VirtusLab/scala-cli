package scala.build

import coursier.cache.FileCache
import coursier.cache.loggers.RefreshLogger
import coursier.jvm.{JvmCache, JavaHome, JvmIndex}
import _root_.dependency._
import scala.build.internal.Constants
import scala.build.internal.Constants._

import java.nio.file.Path

import scala.collection.JavaConverters._
import scala.build.internal.Util.{DependencyOps, ScalaDependencyOps}

final case class Artifacts(
  javaHome: os.Path,
  compilerDependencies: Seq[coursierapi.Dependency],
  compilerArtifacts: Seq[(String, Path)],
  compilerPlugins: Seq[(coursierapi.Dependency, String, Path)],
  dependencies: Seq[coursierapi.Dependency],
  artifacts: Seq[(String, Path)],
  sourceArtifacts: Seq[(String, Path)],
  extraJars: Seq[Path]
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
    scalaVersion: String,
    scalaBinaryVersion: String,
    compilerPlugins: Seq[coursierapi.Dependency],
    dependencies: Seq[coursierapi.Dependency],
    extraJars: Seq[Path],
    fetchSources: Boolean,
    addStubs: Boolean,
    addJvmRunner: Boolean,
    addJvmTestRunner: Boolean,
    addJsTestBridge: Option[String],
    addJmhDependencies: Option[String],
    logger: Logger
  ): Artifacts = {

    val localRepoOpt = LocalRepo.localRepo()

    // expecting Java home to be an absolute path (os.Path will throw else)
    val javaHome0 = os.Path(
      javaHomeOpt
        .orElse(if (jvmIdOpt.isEmpty) sys.props.get("java.home") else None)
        .getOrElse(javaHome(jvmIdOpt))
    )

    val params = ScalaParameters(scalaVersion, scalaBinaryVersion)

    val compilerDependencies =
      if (scalaVersion.startsWith("3."))
        Seq(
          dep"org.scala-lang::scala3-compiler:$scalaVersion".toApi(params)
        )
      else
        Seq(
          dep"org.scala-lang:scala-compiler:$scalaVersion".toApi
        )

    val jvmRunnerDependencies =
      if (addJvmRunner) Seq(dep"$runnerOrganization::$runnerModuleName:$runnerVersion".toApi(params))
      else Nil
    val jvmTestRunnerDependencies =
      if (addJvmTestRunner) Seq(dep"$testRunnerOrganization::$testRunnerModuleName:$testRunnerVersion".toApi(params))
      else Nil
    val jsTestBridgeDependencies = addJsTestBridge.toSeq.map { scalaJsVersion =>
      dep"org.scala-js::scalajs-test-bridge:$scalaJsVersion".toApi(params)
    }

    val jmhDependencies = addJmhDependencies.toSeq.map { version =>
      dep"org.openjdk.jmh:jmh-generator-bytecode:$version".toApi
    }

    val extraRepositories =
      if ((jvmRunnerDependencies ++ jvmTestRunnerDependencies).exists(_.getVersion.endsWith("SNAPSHOT")))
        Seq(coursierapi.MavenRepository.of(coursier.Repositories.sonatype("snapshots").root))
      else Nil

    val allExtraRepositories = extraRepositories ++ localRepoOpt.toSeq

    val updatedDependencies = dependencies ++
      jvmRunnerDependencies ++
      jvmTestRunnerDependencies ++
      jsTestBridgeDependencies ++
      jmhDependencies

    val compilerArtifacts = artifacts(compilerDependencies, allExtraRepositories, logger)
    val artifacts0 = artifacts(updatedDependencies, allExtraRepositories, logger)

    val sourceArtifacts =
      if (fetchSources) artifacts(updatedDependencies, allExtraRepositories, logger, classifiersOpt = Some(Set("sources")))
      else Nil

    val extraStubsJars =
      if (addStubs)
        artifacts(
          Seq(dep"$stubsOrganization:$stubsModuleName:$stubsVersion".toApi),
          allExtraRepositories,
          logger
        ).map(_._2)
      else
        Nil

    val compilerPlugins0 = compilerPlugins.flatMap { dep =>
      val dep0 = coursierapi.Dependency.of(dep)
        .withTransitive(false) // mutable API? :~
      artifacts(Seq(dep0), allExtraRepositories, logger)
        .map { case (url, path) => (dep0, url, path) }
    }

    Artifacts(
      javaHome0,
      compilerDependencies,
      compilerArtifacts,
      compilerPlugins0,
      updatedDependencies,
      artifacts0,
      sourceArtifacts,
      extraJars ++ extraStubsJars
    )
  }

  private def javaHome(idOpt: Option[String]): String = {
    import scala.concurrent.Await
    import scala.concurrent.duration.Duration
    import scala.concurrent.ExecutionContext.Implicits.global
    val cache = FileCache().withLogger(RefreshLogger.create())
    // FIXME JavaHome has many parameters that we may want to allow to customize
    val homeHandler = JavaHome()
      .withCache(Some(
        JvmCache().withCache(cache).withIndex(JvmIndex.coursierIndexUrl)
      ))
    val homeTask = idOpt match {
      case None => homeHandler.default()
      case Some(jvm) => homeHandler.get(jvm)
    }
    val home = Await.result(homeTask.future(), Duration.Inf)
    home.getAbsolutePath
  }

  private[build] def artifacts(
    dependencies: Seq[coursierapi.Dependency],
    extraRepositories: Seq[coursierapi.Repository],
    logger: Logger,
    classifiersOpt: Option[Set[String]] = None
  ): Seq[(String, Path)] = {
    logger.debug(s"Fetching $dependencies" + (if (extraRepositories.isEmpty) "" else s", adding $extraRepositories"))
    // FIXME Many parameters that we could allow to customize here
    val fetcher = coursierapi.Fetch.create()
      .addDependencies(dependencies: _*)
      .withCache(coursierapi.Cache.create().withLogger(logger.coursierInterfaceLogger))
      .addRepositories(extraRepositories: _*)
    for (classifiers <- classifiersOpt)
      fetcher.withClassifiers(classifiers.asJava)
    val result = fetcher
      .fetchResult()
      .getArtifacts()
      .asScala
      .iterator
      .map(e => (e.getKey.getUrl, e.getValue.toPath))
      .toList
    logger.debug((Seq(s"Found ${result.length} artifacts:") ++ result.map("  " + _._2) ++ Seq("")).mkString(System.lineSeparator()))
    result
  }

}
