package scala.cli.commands

import caseapp.*

import java.nio.file.Path
import scala.cli.commands.common.HasLoggingOptions

// format: off
@HelpMessage("Uninstall scala-cli - only works when installed by the installation script")
final case class UninstallOptions(
  @Recurse
    bloopExit: BloopExitOptions = BloopExitOptions(),
  @Recurse
    sharedUninstallCompletions: SharedUninstallCompletionsOptions = SharedUninstallCompletionsOptions(),
  @Group("Uninstall")
  @Name("f")
  @HelpMessage("Force scala-cli uninstall")
  @Tag(tags.implementation)
    force: Boolean = false,
  @Hidden
  @HelpMessage("Don't clear scala-cli cache")
    skipCache: Boolean = false,
  @Hidden
  @HelpMessage("Binary name")
    binaryName: String = "scala-cli",
  @Hidden
  @HelpMessage("Binary directory")
    binDir: Option[String] = None
) extends HasLoggingOptions {
  override def logging: LoggingOptions = bloopExit.logging
  // format: on
  lazy val binDirPath = binDir.map(os.Path(_, os.pwd))
}

object UninstallOptions {
  implicit lazy val parser: Parser[UninstallOptions] = Parser.derive
  implicit lazy val help: Help[UninstallOptions]     = Help.derive
}
