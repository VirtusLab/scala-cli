package scala.cli

import coursierapi.Dependency.{of => dependency}
import coursier.cache.FileCache
import coursier.cache.loggers.RefreshLogger
import coursier.jvm.{JvmCache, JavaHome, JvmIndex}
import scala.cli.internal.Constants

import java.nio.file.Path

import scala.collection.JavaConverters._

final case class Artifacts(
  javaHome: String,
  compilerDependencies: Seq[coursierapi.Dependency],
  compilerArtifacts: Seq[(String, Path)],
  compilerPlugins: Seq[(coursierapi.Dependency, String, Path)],
  dependencies: Seq[coursierapi.Dependency],
  artifacts: Seq[(String, Path)],
  extraJars: Seq[Path]
) {
  lazy val compilerClassPath: Seq[Path] =
    compilerArtifacts.map(_._2)
  lazy val classPath: Seq[Path] =
    artifacts.map(_._2) ++ extraJars
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
    addStubs: Boolean
  ): Artifacts = {

    val javaHome0 = javaHomeOpt
      .orElse(if (jvmIdOpt.isEmpty) sys.props.get("java.home") else None)
      .getOrElse(javaHome(jvmIdOpt))

    val compilerDependencies =
      if (scalaVersion.startsWith("3."))
        Seq(
          dependency("org.scala-lang", "scala3-compiler_" + scalaBinaryVersion, scalaVersion)
        )
      else
        Seq(
          dependency("org.scala-lang", "scala-compiler", scalaVersion)
        )

    val compilerArtifacts = artifacts(compilerDependencies)
    val artifacts0 = artifacts(dependencies)

    val extraStubsJars =
      if (addStubs)
        artifacts(Seq(dependency(Constants.stubsOrganization, Constants.stubsModuleName, Constants.stubsVersion))).map(_._2)
      else
        Nil

    val compilerPlugins0 = compilerPlugins.flatMap { dep =>
      val dep0 = dep.withTransitive(false) // mutable API? :~
      artifacts(Seq(dep0))
        .map { case (url, path) => (dep0, url, path) }
    }

    Artifacts(
      javaHome0,
      compilerDependencies,
      compilerArtifacts,
      compilerPlugins0,
      dependencies,
      artifacts0,
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

  private[cli] def artifacts(dependencies: Seq[coursierapi.Dependency]): Seq[(String, Path)] =
    // FIXME Many parameters that we could allow to customize here
    coursierapi.Fetch.create()
      .addDependencies(dependencies: _*)
      .withCache(coursierapi.Cache.create().withLogger(coursierapi.Logger.progressBars()))
      .fetchResult()
      .getArtifacts()
      .asScala
      .iterator
      .map(e => (e.getKey.getUrl, e.getValue.toPath))
      .toList

}
