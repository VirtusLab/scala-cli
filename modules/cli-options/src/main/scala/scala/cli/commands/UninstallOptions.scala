package scala.cli.commands

import caseapp._
import java.nio.file.Path

// format: off
@HelpMessage("Unistall scala-cli - only works when installed by the installation script")
final case class UninstallOptions(
  @Recurse
    bloopExit: BloopExitOptions = BloopExitOptions(),
  @Group("Uninstall")
  @Name("f")
  @HelpMessage("Force scala-cli uninstall")
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
) {
  // format: on
  lazy val binDirPath = binDir.map(os.Path(_, os.pwd))
}

object UninstallOptions {
  implicit lazy val parser: Parser[UninstallOptions]  = Parser.derive
  implicit lazy val help: Help[UninstallOptions]      = Help.derive
}
