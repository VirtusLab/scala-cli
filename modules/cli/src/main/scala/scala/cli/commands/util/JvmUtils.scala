package scala.cli.commands
package util

import java.io.File

import scala.build.EitherCps.{either, value}
import scala.build.errors.{BuildException, UnrecognizedDebugModeError}
import scala.build.internal.CsLoggerUtil.*
import scala.build.internal.OsLibc
import scala.build.options.{JavaOpt, JavaOptions, ShadowingSeq}
import scala.build.{Os, Position, Positioned, options as bo}
import scala.cli.commands.shared.{CoursierOptions, SharedJvmOptions, SharedOptions}
import scala.concurrent.ExecutionContextExecutorService
import scala.util.Properties
import scala.util.control.NonFatal

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

  def downloadJvm(jvmId: String, options: bo.BuildOptions): String = {
    implicit val ec: ExecutionContextExecutorService = options.finalCache.ec
    val javaHomeManager = options.javaHomeManager
      .withMessage(s"Downloading JVM $jvmId")
    val logger = javaHomeManager.cache
      .flatMap(_.archiveCache.cache.loggerOpt)
      .getOrElse(_root_.coursier.cache.CacheLogger.nop)
    val command = {
      val path = logger.use {
        try javaHomeManager.get(jvmId).unsafeRun()
        catch {
          case NonFatal(e) => throw new Exception(e)
        }
      }
      os.Path(path)
    }
    val ext     = if (Properties.isWin) ".exe" else ""
    val javaCmd = (command / "bin" / s"java$ext").toString
    javaCmd
  }

  def getJavaCmdVersionOrHigher(
    javaVersion: Int,
    options: bo.BuildOptions
  ): String = {
    val javaHomeCmdOpt = for {
      javaHome <- options.javaHomeLocationOpt()
      (javaHomeVersion, javaHomeCmd) = OsLibc.javaHomeVersion(javaHome.value)
      if javaHomeVersion >= javaVersion
    } yield javaHomeCmd

    javaHomeCmdOpt.getOrElse(downloadJvm(javaVersion.toString, options))
  }

  def getJavaCmdVersionOrHigher(
    javaVersion: Int,
    jvmOpts: SharedJvmOptions,
    coursierOpts: CoursierOptions
  ): Either[BuildException, String] = {
    val sharedOpts = SharedOptions(jvm = jvmOpts, coursier = coursierOpts)

    for {
      options <- sharedOpts.buildOptions()
    } yield getJavaCmdVersionOrHigher(javaVersion, options)
  }
}
