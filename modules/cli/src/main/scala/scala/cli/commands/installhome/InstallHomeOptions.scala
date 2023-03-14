package scala.cli.commands.installhome

import caseapp.*

import scala.cli.ScalaCli.{baseRunnerName, fullRunnerName}
import scala.cli.commands.shared.{
  GlobalSuppressWarningOptions,
  HasGlobalOptions,
  HelpGroup,
  LoggingOptions
}
import scala.cli.commands.tags

// format: off
@HelpMessage(s"Install $fullRunnerName in a sub-directory of the home directory")
final case class InstallHomeOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    globalSuppressWarning: GlobalSuppressWarningOptions = GlobalSuppressWarningOptions(),
  @Group(HelpGroup.Install.toString)
  @Tag(tags.implementation)
    scalaCliBinaryPath: String,
  @Group(HelpGroup.Install.toString)
  @Name("f")
  @Tag(tags.implementation)
  @HelpMessage("Overwrite if it exists")
    force: Boolean = false,
  @Group(HelpGroup.Install.toString)
  @Hidden
  @Tag(tags.implementation)
  @HelpMessage("Binary name")
    binaryName: String = baseRunnerName,
  @Group(HelpGroup.Install.toString)
  @Tag(tags.implementation)
  @HelpMessage("Print the update to `env` variable")
    env: Boolean = false,
  @Group(HelpGroup.Install.toString)
  @Hidden
  @Tag(tags.implementation)
  @HelpMessage("Binary directory")
    binDir: Option[String] = None
) extends HasGlobalOptions {
  // format: on
  lazy val binDirPath = binDir.map(os.Path(_, os.pwd))
}

object InstallHomeOptions {
  implicit lazy val parser: Parser[InstallHomeOptions] = Parser.derive
  implicit lazy val help: Help[InstallHomeOptions]     = Help.derive
}
