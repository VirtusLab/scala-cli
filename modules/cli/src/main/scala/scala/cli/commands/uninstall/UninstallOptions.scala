package scala.cli.commands.uninstall

import caseapp.*

import java.nio.file.Path

import scala.cli.ScalaCli.{baseRunnerName, fullRunnerName}
import scala.cli.commands.bloop.BloopExitOptions
import scala.cli.commands.shared.{HasLoggingOptions, HelpMessages, LoggingOptions}
import scala.cli.commands.tags
import scala.cli.commands.uninstallcompletions.SharedUninstallCompletionsOptions

// format: off
@HelpMessage(
  s"""Uninstalls $fullRunnerName.
     |Works only when installed with the installation script.
     |${HelpMessages.installationDocsWebsiteReference}""".stripMargin)
final case class UninstallOptions(
  @Recurse
    bloopExit: BloopExitOptions = BloopExitOptions(),
  @Recurse
    sharedUninstallCompletions: SharedUninstallCompletionsOptions = SharedUninstallCompletionsOptions(),
  @Group("Uninstall")
  @Name("f")
  @HelpMessage(s"Force $baseRunnerName uninstall")
  @Tag(tags.implementation)
    force: Boolean = false,
  @Hidden
  @HelpMessage(s"Don't clear $fullRunnerName cache")
  @Tag(tags.implementation)
    skipCache: Boolean = false,
  @Hidden
  @HelpMessage("Binary name")
  @Tag(tags.implementation)
    binaryName: String = baseRunnerName,
  @Hidden
  @HelpMessage("Binary directory")
  @Tag(tags.implementation)
    binDir: Option[String] = None
) extends HasLoggingOptions {
  override def logging: LoggingOptions = bloopExit.logging
  // format: on
  lazy val binDirPath: Option[os.Path] = binDir.map(os.Path(_, os.pwd))
}

object UninstallOptions {
  implicit lazy val parser: Parser[UninstallOptions] = Parser.derive
  implicit lazy val help: Help[UninstallOptions]     = Help.derive
}
