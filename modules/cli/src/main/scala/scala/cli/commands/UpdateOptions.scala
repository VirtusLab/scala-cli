package scala.cli.commands

import caseapp._

// format: off
@HelpMessage("Update scala-cli - it works only for installation script")
final case class UpdateOptions(
  @Group("Update")
  @HelpMessage("Binary name")
    binaryName: String = "scala-cli",
  @Group("Update")
  @HelpMessage("Binary directory")
    binDir: Option[String] = None,
  @Name("f")
  @HelpMessage("Update scala-cli if is outdated")
    force: Boolean = true,
  @HelpMessage("Update scala-cli to specified version")
    version: Option[String] = None,
) {
  // format: on
  lazy val binDirPath = binDir.map(os.Path(_, os.pwd))
}

object UpdateOptions {
  implicit lazy val parser: Parser[UpdateOptions] = Parser.derive
  implicit lazy val help: Help[UpdateOptions]     = Help.derive
}
