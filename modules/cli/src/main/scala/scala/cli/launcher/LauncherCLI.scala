package scala.cli.launcher

import caseapp.core.RemainingArgs
import dependency._

import scala.build.internal.Runner
import scala.build.options.BuildOptions
import scala.build.{Artifacts, Positioned}
import scala.cli.commands.{LoggingOptions, ScalaCommand}

object LauncherCLI extends ScalaCommand[LauncherOptions] {

  override def ignoreUnrecognized: Boolean = true
  override def hasHelp: Boolean            = false
  override def hasFullHelp: Boolean        = false

  def run(version: String, options: LauncherOptions, remainingArgs: RemainingArgs): Unit = {

    val logger       = LoggingOptions().logger
    val scalaVersion = options.publishedScalaVersion.getOrElse("2.12")

    val scalaCliDependency = Seq(dep"org.virtuslab.scala-cli:cli_$scalaVersion:$version")
    val snapshotsRepo =
      Seq(coursier.Repositories.sonatype("snapshots").root, coursier.Repositories.central.root)

    val fetchedScalaCli =
      Artifacts.fetch(
        Positioned.none(scalaCliDependency),
        snapshotsRepo,
        ScalaParameters(scalaVersion),
        logger,
        None
      ) match {
        case Right(value) => value
        case Left(value) =>
          System.err.println(value.message)
          sys.exit(1)
      }

    val scalaCli =
      fetchedScalaCli.fullDetailedArtifacts.collect { case (_, _, _, Some(f)) =>
        f.toPath.toFile
      }

    val buildOptions = BuildOptions()

    val exitCode =
      Runner.runJvm(
        buildOptions.javaHome().value.javaCommand,
        buildOptions.javaOptions.javaOpts.map(_.value),
        scalaCli,
        "scala.cli.ScalaCli",
        remainingArgs.remaining ++ Seq("--") ++ remainingArgs.unparsed,
        logger
      )

    sys.exit(exitCode)
  }

  override def run(options: LauncherOptions, remainingArgs: RemainingArgs): Unit = {
    for {
      scv <- options.scalaCliVersion
    } run(scv, options, remainingArgs)
  }
}
