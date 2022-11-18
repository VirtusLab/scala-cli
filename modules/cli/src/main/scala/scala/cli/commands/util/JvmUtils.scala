package scala.cli.commands
package util

import java.io.File

import scala.build.EitherCps.{either, value}
import scala.build.errors.{BuildException, UnrecognizedDebugModeError}
import scala.build.options.{JavaOpt, JavaOptions, ShadowingSeq}
import scala.build.{Os, Position, Positioned}
import scala.cli.commands.shared.{SharedJvmOptions, SharedOptions}
import scala.util.Properties

object JvmUtils {
  def javaOptions(opts: SharedJvmOptions): Either[BuildException, JavaOptions] = either {
    import opts._

    val (javacFilePlugins, javacPluginDeps) =
      javacPlugin
        .filter(_.trim.nonEmpty)
        .partition { input =>
          input.contains(File.separator) ||
          (Properties.isWin && input.contains("/")) ||
          input.count(_ == ':') < 2
        }

    val javaOptsSeq = {
      val isDebug =
        opts.sharedDebug.debug ||
        opts.sharedDebug.debugMode.nonEmpty ||
        opts.sharedDebug.debugPort.nonEmpty
      if (isDebug) {
        val server = value {
          opts.sharedDebug.debugMode match {
            case Some("attach") | Some("a") | None => Right("y")
            case Some("listen") | Some("l")        => Right("n")
            case Some(m)                           => Left(new UnrecognizedDebugModeError(m))
          }
        }
        val port = opts.sharedDebug.debugPort.getOrElse("5005")
        Seq(Positioned.none(
          JavaOpt(s"-agentlib:jdwp=transport=dt_socket,server=$server,suspend=y,address=$port")
        ))
      }
      else
        Seq.empty
    }

    JavaOptions(
      javaHomeOpt = javaHome.filter(_.nonEmpty).map(v =>
        Positioned(Seq(Position.CommandLine("--java-home")), os.Path(v, Os.pwd))
      ),
      jvmIdOpt = jvm.filter(_.nonEmpty).map(Positioned.commandLine),
      jvmIndexOpt = jvmIndex.filter(_.nonEmpty),
      jvmIndexOs = jvmIndexOs.map(_.trim).filter(_.nonEmpty),
      jvmIndexArch = jvmIndexArch.map(_.trim).filter(_.nonEmpty),
      javaOpts = ShadowingSeq.from(javaOptsSeq),
      javacPluginDependencies = SharedOptions.parseDependencies(
        javacPluginDeps.map(Positioned.none(_)),
        ignoreErrors = false
      ),
      javacPlugins = javacFilePlugins.map(s => Positioned.none(os.Path(s, Os.pwd))),
      javacOptions = javacOption.map(Positioned.commandLine)
    )
  }
}
