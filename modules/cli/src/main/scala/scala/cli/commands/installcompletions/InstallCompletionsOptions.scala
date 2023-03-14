package scala.cli.commands.installcompletions

import caseapp.*

import scala.cli.ScalaCli.fullRunnerName
import scala.cli.commands.shared.{
  GlobalSuppressWarningOptions,
  HasGlobalOptions,
  HelpGroup,
  HelpMessages,
  LoggingOptions
}
import scala.cli.commands.tags

// format: off
@HelpMessage(InstallCompletionsOptions.helpMessage, "", InstallCompletionsOptions.detailedHelpMessage)
final case class InstallCompletionsOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    globalSuppressWarning: GlobalSuppressWarningOptions = GlobalSuppressWarningOptions(),
  @Group(HelpGroup.Install.toString)
  @Name("shell")
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
  @HelpMessage("Name of the shell, either zsh or bash")
    format: Option[String] = None,

  @Tag(tags.implementation)
  @Group(HelpGroup.Install.toString)
  @Tag(tags.inShortHelp)
  @HelpMessage("Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell")
  rcFile: Option[String] = None,

  @Tag(tags.implementation)
  @HelpMessage("Completions output directory")
  @Group(HelpGroup.Install.toString)
  @Name("o")
  output: Option[String] = None,

  @Hidden
  @Tag(tags.implementation)
  @HelpMessage("Custom banner in comment placed in rc file")
  @Group(HelpGroup.Install.toString)
  banner: String = "{NAME} completions",

  @Hidden
  @Tag(tags.implementation)
  @HelpMessage("Custom completions name")
  @Group(HelpGroup.Install.toString)
  name: Option[String] = None,

  @Tag(tags.implementation)
  @HelpMessage("Print completions to stdout")
  @Group(HelpGroup.Install.toString)
    env: Boolean = false,
) extends HasGlobalOptions
// format: on

object InstallCompletionsOptions {
  implicit lazy val parser: Parser[InstallCompletionsOptions] = Parser.derive
  implicit lazy val help: Help[InstallCompletionsOptions]     = Help.derive

  private val helpHeader = s"Installs $fullRunnerName completions into your shell"
  val helpMessage: String =
    s"""$helpHeader
       |
       |${HelpMessages.commandFullHelpReference("install completions")}
       |${HelpMessages.commandDocWebsiteReference("completions")}""".stripMargin
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |${HelpMessages.commandDocWebsiteReference("completions")}""".stripMargin
}
