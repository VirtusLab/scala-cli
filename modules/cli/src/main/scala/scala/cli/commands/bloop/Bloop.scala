package scala.cli.commands.bloop

import caseapp.core.RemainingArgs

import scala.build.Logger
import scala.build.bloop.BloopThreads
import scala.build.blooprifle.{BloopRifle, BloopRifleConfig}
import scala.build.blooprifle.internal.{Constants, Operations}
import scala.build.internal.OsLibc
import scala.cli.CurrentParams
import scala.cli.commands.{ScalaCommand, SharedOptions}
import scala.cli.commands.util.CommonOps._
import scala.cli.commands.util.SharedCompilationServerOptionsUtil._
import scala.cli.commands.util.SharedOptionsUtil._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Bloop extends ScalaCommand[BloopOptions] {
  override def hidden     = true
  override def isRestricted = true
  override def stopAtFirstUnrecognized = true

  private def bloopRifleConfig0(opts: BloopOptions): BloopRifleConfig = {

    // FIXME Basically a tweaked copy of SharedOptionsUtil.bloopRifleConfig
    // Some in progress BuildOptions / JavaOptions refactoring of mine should allow
    // to stop using SharedOptions and BuildOptions here, and deal with JavaOptions
    // directly.

    val sharedOptions = SharedOptions(
      logging = opts.logging,
      compilationServer = opts.compilationServer,
      directories = opts.directories,
      jvm = opts.jvm,
      coursier = opts.coursier
    )
    val options = sharedOptions.buildOptions(false, None)
    lazy val defaultJvmCmd =
      sharedOptions.downloadJvm(OsLibc.baseDefaultJvm(OsLibc.jvmIndexOs, "17"), options)
    val javaCmd = opts.compilationServer.bloopJvm
      .map(sharedOptions.downloadJvm(_, options))
      .orElse {
        for (javaHome <- options.javaHomeLocationOpt()) yield {
          val (javaHomeVersion, javaHomeCmd) = OsLibc.javaHomeVersion(javaHome.value)
          if (javaHomeVersion >= 17) javaHomeCmd
          else defaultJvmCmd
        }
      }
      .getOrElse(defaultJvmCmd)

    opts.compilationServer.bloopRifleConfig(
      opts.logging.logger,
      sharedOptions.coursierCache,
      opts.logging.verbosity,
      javaCmd,
      opts.directories.directories,
      Some(17)
    )
  }

  def run(options: BloopOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.logging.verbosity

    val threads          = BloopThreads.create()
    val logger           = options.logging.logger
    val bloopRifleConfig = bloopRifleConfig0(options)

    val isRunning = BloopRifle.check(bloopRifleConfig, logger.bloopRifleLogger)

    if (isRunning)
      logger.debug("Found running Bloop server")
    else {
      logger.debug("No running Bloop server found, starting one")
      val f = BloopRifle.startServer(
        bloopRifleConfig,
        threads.startServerChecks,
        logger.bloopRifleLogger,
        bloopRifleConfig.retainedBloopVersion.version.raw,
        bloopRifleConfig.javaPath
      )
      Await.result(f, Duration.Inf)
      logger.message("Bloop server started.")
    }

    val args0 = args.all

    args0 match {
      case Seq() =>
        // FIXME Give more details?
        logger.message("Bloop server is running.")
      case Seq(cmd, args @ _*) =>
        val assumeTty = System.console() != null
        val workingDir = options.workDirOpt.getOrElse(os.pwd).toNIO
        Operations.run(
          command = cmd,
          args = args.toArray,
          workingDir = workingDir,
          address = bloopRifleConfig.address,
          inOpt = Some(System.in),
          out = System.out,
          err = System.err,
          logger = logger.bloopRifleLogger,
          assumeInTty = assumeTty,
          assumeOutTty = assumeTty,
          assumeErrTty = assumeTty
        )
    }
  }
}
