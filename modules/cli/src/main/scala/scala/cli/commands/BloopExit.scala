package scala.cli.commands

import caseapp._

import scala.build.Os
import scala.build.blooprifle.BloopRifle

object BloopExit extends ScalaCommand[BloopExitOptions] {
  override def hidden = true
  override def names: List[List[String]] = List(
    List("bloop", "exit")
  )
  def run(options: BloopExitOptions, args: RemainingArgs): Unit = {
    val bloopRifleConfig = options.bloopRifleConfig
    val logger           = options.logging.logger

    val isRunning = BloopRifle.check(bloopRifleConfig, logger.bloopRifleLogger)

    if (isRunning) {
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
    else
      logger.message("No running Bloop server found.")
  }
}
