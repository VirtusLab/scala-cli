package scala.build

import coursier.cache.FileCache
import coursier.core.Repository
import coursier.util.Task
import dependency.*

import java.io.File

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.CsLoggerUtil.*

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
  def default(
    scalaParams: ScalaParameters,
    dependencies: Seq[AnyDependency],
    extraClassPath: Seq[os.Path],
    logger: Logger,
    cache: FileCache[Task],
    repositories: Seq[Repository],
    addScalapy: Option[String],
    javaVersion: Int
  ): Either[BuildException, ReplArtifacts] = either {
    val isScala2             = scalaParams.scalaVersion.startsWith("2.")
    val firstNewReplNightly  = "3.8.0-RC1-bin-20251101-389483e-NIGHTLY".coursierVersion
    val firstNewReplRc       = "3.8.0-RC1".coursierVersion
    val firstNewReplStable   = "3.8.0".coursierVersion
    val scalaCoursierVersion = scalaParams.scalaVersion.coursierVersion
    val shouldUseNewRepl     =
      !isScala2 &&
      ((scalaCoursierVersion >= firstNewReplNightly) || (scalaCoursierVersion >= firstNewReplRc) ||
      scalaCoursierVersion >= firstNewReplStable)
    val replDeps =
      if isScala2 then Seq(dep"org.scala-lang:scala-compiler:${scalaParams.scalaVersion}")
      else if shouldUseNewRepl then
        Seq(
          dep"org.scala-lang::scala3-compiler:${scalaParams.scalaVersion}",
          dep"org.scala-lang::scala3-repl:${scalaParams.scalaVersion}"
        )
      else Seq(dep"org.scala-lang::scala3-compiler:${scalaParams.scalaVersion}")
    val scalapyDeps =
      addScalapy.map(ver => dep"${Artifacts.scalaPyOrganization(ver)}::scalapy-core::$ver").toSeq
    val externalDeps                          = dependencies ++ scalapyDeps
    val replArtifacts: Seq[(String, os.Path)] = value {
      Artifacts.artifacts(
        replDeps.map(Positioned.none),
        repositories,
        Some(scalaParams),
        logger,
        cache.withMessage(s"Downloading Scala compiler ${scalaParams.scalaVersion}")
      )
    }
    val depArtifacts: Seq[(String, os.Path)] = value {
      Artifacts.artifacts(
        externalDeps.map(Positioned.none),
        repositories,
        Some(scalaParams),
        logger,
        cache.withMessage(s"Downloading REPL dependencies")
      )
    }
    val mainClass =
      if isScala2 then "scala.tools.nsc.MainGenericRunner"
      else "dotty.tools.repl.Main"
    val defaultReplJavaOpts = Seq("-Dscala.usejavacp=true")
    val jlineArtifacts      =
      replArtifacts
        .map(_._2.toString)
        .filter(_.contains("jline"))
    val jlineJavaOpts: Seq[String] =
      if javaVersion >= 24 && jlineArtifacts.nonEmpty then {
        val modulePath    = Seq("--module-path", jlineArtifacts.mkString(File.pathSeparator))
        val remainingOpts =
          if isScala2 then
            Seq(
              "--add-modules",
              "org.jline",
              "--enable-native-access=org.jline"
            )
          else
            Seq(
              "--add-modules",
              "org.jline.terminal",
              "--enable-native-access=org.jline.nativ"
            )
        modulePath ++ remainingOpts
      }
      else Seq.empty
    val replJavaOpts = defaultReplJavaOpts ++ jlineJavaOpts
    ReplArtifacts(
      replArtifacts = replArtifacts,
      depArtifacts = depArtifacts,
      extraClassPath = extraClassPath,
      extraSourceJars = Nil,
      replMainClass = mainClass,
      replJavaOpts = replJavaOpts,
      addSourceJars = false
    )
  }
}
