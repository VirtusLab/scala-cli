package scala.cli.commands
package util

import java.io.File
import java.net.ServerSocket

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.{BuildException, UnrecognizedDebugModeError}
import scala.build.options.{JavaOpt, JavaOptions, ShadowingSeq}
import scala.build.{Os, Position, Positioned}
import scala.cli.commands.shared.{SharedJvmOptions, SharedOptions}
import scala.util.Properties

object JvmUtils {
  private def randomPort(): Int = {
    val s = new ServerSocket(0)
    try s.getLocalPort()
    finally s.close()
  }
  def javaOptions(opts: SharedJvmOptions, logger: Logger): Either[BuildException, JavaOptions] =
    either {
      import opts._

      val (javacFilePlugins, javacPluginDeps) =
        javacPlugin
          .filter(_.trim.nonEmpty)
          .partition { input =>
            input.contains(File.separator) ||
            (Properties.isWin && input.contains("/")) ||
            input.count(_ == ':') < 2
          }

      val javaDebugOptsSeq = {
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

      val jmxRemoteOpts =
        opts.jmxRemote.map(_.trim).filter(_.nonEmpty) match {
          case Some(value) =>
            val (host, port) =
              if (value.forall(_.isDigit)) ("localhost", value.toInt)
              else
                value.split(":", 2) match {
                  case Array(host, portStr) if portStr.nonEmpty && portStr.forall(_.isDigit) =>
                    (host, portStr.toInt)
                  case Array(host) =>
                    (host, 0)
                }
            val actualPort =
              if (port <= 0) {
                val p = randomPort()
                logger.log(s"Listening to JMX remote connections on $host:$p")
                p
              }
              else
                port
            val isLocalhost =
              host == "localhost" || host == "127.0.0.1" // Other values too?
            logger.debug(
              s"Enabling JMX remote on $host:$actualPort" +
                (if (isLocalhost) " (local only)" else "")
            )
            Seq(
              s"-Dcom.sun.management.jmxremote.port=$actualPort",
              "-Dcom.sun.management.jmxremote.authenticate=false",
              "-Dcom.sun.management.jmxremote.ssl=false",
              s"-Dcom.sun.management.jmxremote.local.only=$isLocalhost",
              s"-Dcom.sun.management.jmxremote.rmi.port=$actualPort",
              s"-Djava.rmi.server.hostname=$host"
            ).map(JavaOpt(_)).map(Positioned.none(_))
          case None =>
            Nil
        }

      JavaOptions(
        javaHomeOpt = javaHome.filter(_.nonEmpty).map(v =>
          Positioned(Seq(Position.CommandLine("--java-home")), os.Path(v, Os.pwd))
        ),
        jvmIdOpt = jvm.filter(_.nonEmpty).map(Positioned.commandLine),
        jvmIndexOpt = jvmIndex.filter(_.nonEmpty),
        jvmIndexOs = jvmIndexOs.map(_.trim).filter(_.nonEmpty),
        jvmIndexArch = jvmIndexArch.map(_.trim).filter(_.nonEmpty),
        javaOpts = ShadowingSeq.from(javaDebugOptsSeq ++ jmxRemoteOpts),
        javacPluginDependencies = SharedOptions.parseDependencies(
          javacPluginDeps.map(Positioned.none(_)),
          ignoreErrors = false
        ),
        javacPlugins = javacFilePlugins.map(s => Positioned.none(os.Path(s, Os.pwd))),
        javacOptions = javacOption.map(Positioned.commandLine)
      )
    }
}
