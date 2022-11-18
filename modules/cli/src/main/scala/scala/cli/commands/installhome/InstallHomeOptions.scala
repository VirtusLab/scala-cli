package scala.cli.commands.installhome

import caseapp.*

import scala.cli.commands.shared.{HasLoggingOptions, LoggingOptions}
import scala.cli.commands.tags

// format: off
@HelpMessage("Install Scala CLI in a sub-directory of the home directory")
final case class InstallHomeOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Group("InstallHome")
  @Tag(tags.implementation)
    scalaCliBinaryPath: String,
  @Group("InstallHome")
  @Name("f")
  @Tag(tags.implementation)
  @HelpMessage("Overwrite if it exists")
    force: Boolean = false,
  @Hidden
  @Tag(tags.implementation)
  @HelpMessage("Binary name")
    binaryName: String = "scala-cli",
  @Tag(tags.implementation)
  @HelpMessage("Print the update to `env` variable")
    env: Boolean = false,
  @Hidden
  @Tag(tags.implementation)
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
