package scala.cli.javaLauncher

import java.io.File

import scala.build.Positioned
import scala.build.internal.{OsLibc, Runner}
import scala.build.options.{BuildOptions, JavaOptions}
import scala.cli.commands.shared.LoggingOptions
import scala.cli.javaLauncher.JavaLauncherCli.LauncherKind.*
object JavaLauncherCli {

  def runAndExit(remainingArgs: Seq[String]): Nothing = {
    val logger       = LoggingOptions().logger
    val scalaCliPath =
      System.getProperty("java.class.path").split(File.pathSeparator).iterator.toList.map { f =>
        os.Path(f, os.pwd)
      }

    val buildOptions = BuildOptions(
      javaOptions = JavaOptions(
        jvmIdOpt = Some(OsLibc.defaultJvm(OsLibc.jvmIndexOs)).map(Positioned.none)
      )
    )
    val launcherKind = sys.props.get("scala-cli.kind") match {
      case Some("jvm.bootstrapped")       => Bootstrapped
      case Some("jvm.standaloneLauncher") => StandaloneLauncher
      case _                              => sys.error("should not happen")
    }
    val classPath = launcherKind match {
      case Bootstrapped       => scalaCliPath
      case StandaloneLauncher => scalaCliPath.headOption.toList
    }
    val mainClass = launcherKind match {
      case Bootstrapped       => "scala.cli.ScalaCli"
      case StandaloneLauncher => "coursier.bootstrap.launcher.ResourcesLauncher"
    }

    val exitCode =
      Runner.runJvm(
        buildOptions.javaHome().value.javaCommand,
        buildOptions.javaOptions.javaOpts.toSeq.map(_.value.value),
        classPath,
        mainClass,
        remainingArgs,
        logger,
        allowExecve = true
      ).waitFor()

    sys.exit(exitCode)
  }

  enum LauncherKind {
    case Bootstrapped, StandaloneLauncher
  }
}
