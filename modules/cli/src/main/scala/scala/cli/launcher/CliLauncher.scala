package scala.cli.launcher
import dependency._

import scala.build.internal.{OsLibc, Runner}
import scala.build.options.{BuildOptions, JavaOptions}
import scala.build.{Artifacts, Positioned}
import scala.cli.commands.LoggingOptions

object CliLauncher {

  /**
    *
    * @param cliVersion the Scala CLI version at use
    * @param cliLauncherOptions
    * @param powArgsPlusPostDoubleDashArgs
    * @return
    */
  def runAndExit(cliVersion: String, cliLauncherOptions:  CliLauncherOptions, powArgsPlusPostDoubleDashArgs: Seq[String]): Nothing = {

    val logger       = LoggingOptions().logger
    val scalaVersion = cliLauncherOptions.cliScalaVersion.getOrElse("2.12")

    val scalaCliDependency = Seq(dep"org.virtuslab.scala-cli:cli_$scalaVersion:$cliVersion")
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

    val buildOptions = BuildOptions(
      javaOptions = JavaOptions(
        jvmIdOpt = Some(OsLibc.baseDefaultJvm(OsLibc.jvmIndexOs, "17"))
      )
    )

    val exitCode =
      Runner.runJvm(
        buildOptions.javaHome().value.javaCommand,
        buildOptions.javaOptions.javaOpts.map(_.value),
        scalaCli,
        "scala.cli.ScalaCli",
        powArgsPlusPostDoubleDashArgs,
        logger,
        allowExecve = true
      )

    sys.exit(exitCode)
  }

}
