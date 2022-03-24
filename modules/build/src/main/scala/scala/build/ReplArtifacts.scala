package scala.build

import coursier.cache.FileCache
import coursier.util.Task
import dependency._

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.CsLoggerUtil._

final case class ReplArtifacts(
  replArtifacts: Seq[(String, os.Path)],
  extraClassPath: Seq[os.Path],
  extraSourceJars: Seq[os.Path],
  replMainClass: String,
  replJavaOpts: Seq[String],
  addSourceJars: Boolean
) {
  lazy val replClassPath: Seq[os.Path] =
    if (addSourceJars)
      extraClassPath ++ extraSourceJars ++ replArtifacts.map(_._2)
    else
      extraClassPath ++ replArtifacts.map(_._2)
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
    logger: Logger,
    cache: FileCache[Task],
    directories: Directories
  ): Either[BuildException, ReplArtifacts] = either {
    val localRepoOpt = LocalRepo.localRepo(directories.localRepoDir)
    val allDeps      = dependencies ++ Seq(dep"com.lihaoyi:::ammonite:$ammoniteVersion")
    val replArtifacts = Artifacts.artifacts(
      Positioned.none(allDeps),
      localRepoOpt.toSeq,
      scalaParams,
      logger,
      cache.withMessage(s"Downloading Ammonite $ammoniteVersion")
    )
    val replSourceArtifacts = Artifacts.artifacts(
      Positioned.none(allDeps),
      localRepoOpt.toSeq,
      scalaParams,
      logger,
      cache.withMessage(s"Downloading Ammonite $ammoniteVersion sources"),
      classifiersOpt = Some(Set("sources"))
    )
    ReplArtifacts(
      value(replArtifacts) ++ value(replSourceArtifacts),
      extraClassPath,
      extraSourceJars,
      "ammonite.Main",
      Nil,
      addSourceJars = true
    )
  }

  def default(
    scalaParams: ScalaParameters,
    dependencies: Seq[AnyDependency],
    extraClassPath: Seq[os.Path],
    logger: Logger,
    cache: FileCache[Task],
    repositories: Seq[String]
  ): Either[BuildException, ReplArtifacts] = either {
    val isScala2 = scalaParams.scalaVersion.startsWith("2.")
    val replDep =
      if (isScala2) dep"org.scala-lang:scala-compiler:${scalaParams.scalaVersion}"
      else dep"org.scala-lang::scala3-compiler:${scalaParams.scalaVersion}"
    val allDeps = dependencies ++ Seq(replDep)
    val replArtifacts =
      Artifacts.artifacts(
        Positioned.none(allDeps),
        repositories,
        scalaParams,
        logger,
        cache.withMessage(s"Downloading Scala compiler ${scalaParams.scalaVersion}")
      )
    val mainClass =
      if (isScala2) "scala.tools.nsc.MainGenericRunner"
      else "dotty.tools.repl.Main"
    ReplArtifacts(
      value(replArtifacts),
      extraClassPath,
      Nil,
      mainClass,
      Seq("-Dscala.usejavacp=true"),
      addSourceJars = false
    )
  }
}
