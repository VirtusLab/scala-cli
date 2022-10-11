package scala.cli.commands

import caseapp._

import java.nio.charset.Charset

import scala.cli.CurrentParams
import scala.cli.commands.util.CommonOps._
import scala.cli.internal.ProfileFileUpdater

object UninstallCompletions extends ScalaCommand[UninstallCompletionsOptions] {
  override def names = List(
    List("uninstall", "completions"),
    List("uninstall-completions")
  )
  override def loggingOptions(options: UninstallCompletionsOptions): Option[LoggingOptions] =
    Some(options.logging)
  override def runCommand(options: UninstallCompletionsOptions, args: RemainingArgs) = {
    val logger = options.logging.logger
    val name   = InstallCompletions.getName(options.shared.name)

    val zDotDir = Option(System.getenv("ZDOTDIR"))
      .map(os.Path(_, os.pwd))
      .getOrElse(os.home)
    val rcFiles = options.shared.rcFile.map(file => Seq(os.Path(file, os.pwd))).getOrElse(Seq(
      zDotDir / ".zshrc",
      os.home / ".bashrc"
    )).filter(os.exists(_))

    rcFiles.foreach { rcFile =>
      val banner = options.shared.banner.replace("{NAME}", name)

      val updated = ProfileFileUpdater.removeFromProfileFile(
        rcFile.toNIO,
        banner,
        Charset.defaultCharset()
      )

      if (options.logging.verbosity >= 0)
        if (updated) {
          logger.message(s"Updated $rcFile")
          logger.message("scala-cli completions uninstalled successfully")
        }
        else
          logger.error("Problem occurred while uninstalling scala-cli completions")
    }
  }
}
