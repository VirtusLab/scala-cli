package scala.cli.commands.bloop

import bloop.rifle.{BloopRifle, BloopRifleConfig}
import caseapp.*

import scala.build.{Directories, Logger, Os}
import scala.cli.CurrentParams
import scala.cli.commands.ScalaCommand

object BloopExit extends ScalaCommand[BloopExitOptions] {
  override def hidden                  = true
  override def scalaSpecificationLevel = SpecificationLevel.RESTRICTED
  override def names: List[List[String]] = List(
    List("bloop", "exit")
  )

  private def mkBloopRifleConfig(opts: BloopExitOptions): BloopRifleConfig = {
    import opts.*
    compilationServer.bloopRifleConfig(
      global.logging.logger,
      coursier.coursierCache(global.logging.logger.coursierLogger("Downloading Bloop")),
      global.logging.verbosity,
      "java", // shouldn't be used…
      Directories.directories
    )
  }

  override def runCommand(options: BloopExitOptions, args: RemainingArgs, logger: Logger): Unit = {
    val bloopRifleConfig = mkBloopRifleConfig(options)

    val isRunning = BloopRifle.check(bloopRifleConfig, logger.bloopRifleLogger)

    if (isRunning) {
      val ret = BloopRifle.exit(bloopRifleConfig, Os.pwd.toNIO, logger.bloopRifleLogger)
      logger.debug(s"Bloop exit returned code $ret")
      if (ret == 0)
        logger.message("Stopped Bloop server.")
      else {
        if (options.global.logging.verbosity >= 0)
          System.err.println(s"Error running bloop exit command (return code $ret)")
        sys.exit(1)
      }
    }
    else
      logger.message("No running Bloop server found.")
  }
}
