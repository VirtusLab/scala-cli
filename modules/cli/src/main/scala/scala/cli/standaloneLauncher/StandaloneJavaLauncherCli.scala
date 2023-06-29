package scala.cli.standaloneLauncher

import java.io.File

import scala.build.Positioned
import scala.build.internal.{OsLibc, Runner}
import scala.build.options.{BuildOptions, JavaOptions}
import scala.cli.commands.shared.LoggingOptions

object StandaloneJavaLauncherCli {

  def runAndExit(remainingArgs: Seq[String]): Nothing = {
    val logger = LoggingOptions().logger
    val scalaCli =
      System.getProperty("java.class.path").split(File.pathSeparator).iterator.toList.map { f =>
        os.Path(f, os.pwd)
      }

    val buildOptions = BuildOptions(
      javaOptions = JavaOptions(
        jvmIdOpt = Some(OsLibc.baseDefaultJvm(OsLibc.jvmIndexOs, "17")).map(Positioned.none)
      )
    )

    val exitCode =
      Runner.runJvm(
        buildOptions.javaHome().value.javaCommand,
        buildOptions.javaOptions.javaOpts.toSeq.map(_.value.value),
        scalaCli.headOption.toList,
        "coursier.bootstrap.launcher.ResourcesLauncher",
        remainingArgs,
        logger,
        allowExecve = true
      ).waitFor()

    sys.exit(exitCode)
  }

}
