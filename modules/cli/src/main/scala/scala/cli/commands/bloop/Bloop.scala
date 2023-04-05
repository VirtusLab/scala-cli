package scala.cli.commands.bloop

import bloop.rifle.internal.{Constants, Operations}
import bloop.rifle.{BloopRifle, BloopRifleConfig, BloopThreads}
import caseapp.core.RemainingArgs

import scala.build.internal.OsLibc
import scala.build.{Directories, Logger}
import scala.cli.CurrentParams
import scala.cli.commands.ScalaCommand
import scala.cli.commands.shared.{LoggingOptions, SharedOptions}
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Bloop extends ScalaCommand[BloopOptions] {
  override def hidden                  = true

  override def scalaSpecificationLevel = SpecificationLevel.RESTRICTED

  override def stopAtFirstUnrecognized = true

  private def bloopRifleConfig0(opts: BloopOptions): BloopRifleConfig = {

    // FIXME Basically a tweaked copy of SharedOptions.bloopRifleConfig
    // Some in progress BuildOptions / JavaOptions refactoring of mine should allow
    // to stop using SharedOptions and BuildOptions here, and deal with JavaOptions
    // directly.

    val sharedOptions = SharedOptions(
      logging = opts.global.logging,
      compilationServer = opts.compilationServer,
      jvm = opts.jvm,
      coursier = opts.coursier
    )
    val options = sharedOptions.buildOptions(false, None).orExit(opts.global.logging.logger)
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
      opts.global.logging.logger,
      sharedOptions.coursierCache,
      opts.global.logging.verbosity,
      javaCmd,
      Directories.directories,
      Some(17)
    )
  }

  override def runCommand(options: BloopOptions, args: RemainingArgs, logger: Logger): Unit = {
    val threads          = BloopThreads.create()
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
        val assumeTty  = System.console() != null
        val workingDir = options.workDirOpt.getOrElse(os.pwd).toNIO
        val retCode = Operations.run(
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
        if (retCode == 0)
          logger.debug(s"Bloop command $cmd ran successfully (return code 0)")
        else {
          logger.debug(s"Got return code $retCode from Bloop server when running $cmd, exiting with it")
          sys.exit(retCode)
        }
    }
  }
}
