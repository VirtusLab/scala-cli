package scala.cli.commands

import caseapp.*

import scala.cli.commands.common.HasLoggingOptions

// format: off
@HelpMessage("Install Scala CLI in a sub-directory of the home directory")
final case class InstallHomeOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Group("InstallHome")
    scalaCliBinaryPath: String,
  @Group("InstallHome")
  @Name("f")
  @HelpMessage("Overwrite if it exists")
    force: Boolean = false,
  @Hidden
  @HelpMessage("Binary name")
    binaryName: String = "scala-cli",
  @HelpMessage("Print the update to `env` variable")
    env: Boolean = false,
  @Hidden
  @HelpMessage("Binary directory")
    binDir: Option[String] = None
) extends HasLoggingOptions {
  // format: on
  lazy val binDirPath = binDir.map(os.Path(_, os.pwd))
}

object InstallHomeOptions {
  implicit lazy val parser: Parser[InstallHomeOptions] = Parser.derive
  implicit lazy val help: Help[InstallHomeOptions]     = Help.derive
}
