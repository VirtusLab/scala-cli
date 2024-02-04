package scala.build

import coursier.cache.FileCache
import coursier.core.{Repository, Version}
import coursier.util.Task
import dependency._

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.CsLoggerUtil._

final case class ReplArtifacts(
  replArtifacts: Seq[(String, os.Path)],
  depArtifacts: Seq[(String, os.Path)],
  extraClassPath: Seq[os.Path],
  extraSourceJars: Seq[os.Path],
  replMainClass: String,
  replJavaOpts: Seq[String],
  addSourceJars: Boolean,
  includeExtraCpOnReplCp: Boolean = false
) {
  private lazy val fullExtraClassPath: Seq[os.Path] =
    if addSourceJars then extraClassPath ++ extraSourceJars else extraClassPath
  lazy val replClassPath: Seq[os.Path] =
    (if includeExtraCpOnReplCp then fullExtraClassPath ++ replArtifacts.map(_._2).distinct
     else replArtifacts.map(_._2))
      .distinct
  lazy val depsClassPath: Seq[os.Path] = (fullExtraClassPath ++ depArtifacts.map(_._2)).distinct
}

object ReplArtifacts {
  // TODO In order to isolate more Ammonite dependencies, we'd need to get two class paths:
  //      - a shared one, with ammonite-repl-api, ammonite-compiler, and dependencies
  //      - an Ammonite-specific one, with the other ammonite JARs
  // Then, use the coursier-bootstrap library to generate a launcher creating to class loaders,
  // with each of those class paths, and run Ammonite with this launcher.
  // This requires to change this line in Ammonite, https://github.com/com-lihaoyi/Ammonite/blob/0f0d597f04e62e86cbf76d3bd16deb6965331470/amm/src/main/scala/ammonite/Main.scala#L99,
  // to
  //     val contextClassLoader = classOf[ammonite.repl.api.ReplAPI].getClassLoader
  // so that only the first loader is exposed to users in Ammonite.
  def ammonite(
    scalaParams: ScalaParameters,
    ammoniteVersion: String,
    dependencies: Seq[AnyDependency],
    extraClassPath: Seq[os.Path],
    extraSourceJars: Seq[os.Path],
    extraRepositories: Seq[Repository],
    logger: Logger,
    cache: FileCache[Task],
    directories: Directories,
    addScalapy: Option[String]
  ): Either[BuildException, ReplArtifacts] = either {
    val scalapyDeps =
      addScalapy.map(ver => dep"${Artifacts.scalaPyOrganization(ver)}::scalapy-core::$ver").toSeq
    val allDeps = dependencies ++ Seq(dep"com.lihaoyi:::ammonite:$ammoniteVersion") ++ scalapyDeps
    val replArtifacts = Artifacts.artifacts(
      allDeps.map(Positioned.none),
      extraRepositories,
      Some(scalaParams),
      logger,
      cache.withMessage(s"Downloading Ammonite $ammoniteVersion")
    )
    val replSourceArtifacts = Artifacts.artifacts(
      allDeps.map(Positioned.none),
      extraRepositories,
      Some(scalaParams),
      logger,
      cache.withMessage(s"Downloading Ammonite $ammoniteVersion sources"),
      classifiersOpt = Some(Set("sources"))
    )
    ReplArtifacts(
      replArtifacts = value(replArtifacts) ++ value(replSourceArtifacts),
      depArtifacts =
        Nil, // amm does not support a -cp option, deps are passed directly to Ammonite cp
      extraClassPath = extraClassPath,
      extraSourceJars = extraSourceJars,
      replMainClass = "ammonite.Main",
      replJavaOpts = Nil,
      addSourceJars = true,
      includeExtraCpOnReplCp =
        true // extra cp & source jars have to be passed directly to Ammonite cp
    )
  }

  def default(
    scalaParams: ScalaParameters,
    dependencies: Seq[AnyDependency],
    extraClassPath: Seq[os.Path],
    logger: Logger,
    cache: FileCache[Task],
    repositories: Seq[Repository],
    addScalapy: Option[String]
  ): Either[BuildException, ReplArtifacts] = either {
    val isScala2 = scalaParams.scalaVersion.startsWith("2.")
    val replDep =
      if (isScala2) dep"org.scala-lang:scala-compiler:${scalaParams.scalaVersion}"
      else dep"org.scala-lang::scala3-compiler:${scalaParams.scalaVersion}"
    val scalapyDeps =
      addScalapy.map(ver => dep"${Artifacts.scalaPyOrganization(ver)}::scalapy-core::$ver").toSeq
    val externalDeps = dependencies ++ scalapyDeps
    val replArtifacts =
      Artifacts.artifacts(
        Seq(replDep).map(Positioned.none),
        repositories,
        Some(scalaParams),
        logger,
        cache.withMessage(s"Downloading Scala compiler ${scalaParams.scalaVersion}")
      )
    val depArtifacts = Artifacts.artifacts(
      externalDeps.map(Positioned.none),
      repositories,
      Some(scalaParams),
      logger,
      cache.withMessage(s"Downloading REPL dependencies")
    )
    val mainClass =
      if (isScala2) "scala.tools.nsc.MainGenericRunner"
      else "dotty.tools.repl.Main"
    ReplArtifacts(
      replArtifacts = value(replArtifacts),
      depArtifacts = value(depArtifacts),
      extraClassPath = extraClassPath,
      extraSourceJars = Nil,
      replMainClass = mainClass,
      replJavaOpts = Seq("-Dscala.usejavacp=true"),
      addSourceJars = false
    )
  }
}
