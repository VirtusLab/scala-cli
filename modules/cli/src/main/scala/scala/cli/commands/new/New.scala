package scala.cli.commands.`new`

import caseapp.core.RemainingArgs
import dependency._

import scala.build.internal.{Constants, OsLibc, Runner}
import scala.build.options.{BuildOptions, JavaOptions}
import scala.build.{Artifacts, Logger, Positioned}
import scala.cli.commands.ScalaCommand
import scala.cli.commands.shared.{CoursierOptions, HelpCommandGroup}

object New extends ScalaCommand[NewOptions] {
  override def group: String = HelpCommandGroup.Main.toString

  override def scalaSpecificationLevel = SpecificationLevel.EXPERIMENTAL

  private def giter8Dependency =
    Seq(dep"${Constants.giter8Organization}::${Constants.giter8Name}:${Constants.giter8Version}")

  override def runCommand(options: NewOptions, remainingArgs: RemainingArgs, logger: Logger): Unit =
    val scalaParameters = ScalaParameters(Constants.defaultScala213Version)
    val fetchedGiter8 = Artifacts.fetchAnyDependencies(
      giter8Dependency.map(Positioned.none),
      Seq.empty,
      Some(scalaParameters),
      logger,
      CoursierOptions().coursierCache(logger.coursierLogger("")),
      None
    ) match {
      case Right(value) => value
      case Left(value) =>
        System.err.println(value.message)
        sys.exit(1)
    }

    val giter8 = fetchedGiter8.fullDetailedArtifacts.collect {
      case (_, _, _, Some(f)) => os.Path(f, os.pwd)
    }

    val buildOptions = BuildOptions(
      javaOptions = JavaOptions(
        jvmIdOpt = Some(OsLibc.defaultJvm(OsLibc.jvmIndexOs)).map(Positioned.none)
      )
    )

    val exitCode =
      Runner.runJvm(
        buildOptions.javaHome().value.javaCommand,
        buildOptions.javaOptions.javaOpts.toSeq.map(_.value.value),
        giter8,
        "giter8.Giter8",
        remainingArgs.remaining,
        logger,
        allowExecve = true
      ).waitFor()

    sys.exit(exitCode)
}
