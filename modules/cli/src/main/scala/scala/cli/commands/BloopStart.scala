package scala.cli.commands

import caseapp._

import scala.build.Os
import scala.build.bloop.BloopThreads
import scala.build.blooprifle.BloopRifle
import scala.build.blooprifle.internal.Constants
import scala.cli.CurrentParams
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object BloopStart extends ScalaCommand[BloopStartOptions] {
  override def hidden     = true
  override def inSipScala = false
  override def names: List[List[String]] = List(
    List("bloop", "start")
  )
  def run(options: BloopStartOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.logging.verbosity
    val threads          = BloopThreads.create()
    val bloopRifleConfig = options.bloopRifleConfig()
    val logger           = options.logging.logger

    val isRunning = BloopRifle.check(bloopRifleConfig, logger.bloopRifleLogger)

    if (isRunning && options.force) {
      logger.message("Found Bloop server running, stopping it.")
      val ret = BloopRifle.exit(bloopRifleConfig, Os.pwd.toNIO, logger.bloopRifleLogger)
      logger.debug(s"Bloop exit returned code $ret")
      if (ret == 0)
        logger.message("Stopped Bloop server.")
      else {
        if (options.logging.verbosity >= 0)
          System.err.println(s"Error running bloop exit command (return code $ret)")
        sys.exit(1)
      }
    }

    if (isRunning && !options.force)
      logger.message("Bloop server already running.")
    else {

      val f = BloopRifle.startServer(
        bloopRifleConfig,
        threads.startServerChecks,
        logger.bloopRifleLogger,
        Constants.bloopVersion,
        options.buildOptions.javaHome().value.javaCommand
      )
      Await.result(f, Duration.Inf)
      logger.message("Bloop server started.")
    }
  }
}
