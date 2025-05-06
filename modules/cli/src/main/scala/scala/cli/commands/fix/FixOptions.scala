package scala.cli.commands.fix

import caseapp._
import caseapp.core.help.Help

import scala.cli.ScalaCli
import scala.cli.ScalaCli.fullRunnerName
import scala.cli.commands.shared.{HasSharedOptions, HelpGroup, HelpMessages, SharedOptions}
import scala.cli.commands.tags

@HelpMessage(FixOptions.helpMessage, "", FixOptions.detailedHelpMessage)
final case class FixOptions(
  @Recurse
  shared: SharedOptions = SharedOptions(),
  @Recurse
  scalafix: ScalafixOptions = ScalafixOptions(),
  @Group(HelpGroup.Fix.toString)
  @Tag(tags.experimental)
  @HelpMessage("Fail the invocation if rewrites are needed")
  @Tag(tags.inShortHelp)
  check: Boolean = false,
  @Group(HelpGroup.Fix.toString)
  @Tag(tags.experimental)
  @HelpMessage("Enable running Scalafix rules (enabled by default)")
  @Tag(tags.inShortHelp)
  @Name("scalafix")
  enableScalafix: Boolean = true,
  @Group(HelpGroup.Fix.toString)
  @Tag(tags.experimental)
  @HelpMessage("Enable running built-in rules (enabled by default)")
  @Tag(tags.inShortHelp)
  @Name("enableBuiltIn")
  @Name("builtIn")
  @Name("builtInRules")
  enableBuiltInRules: Boolean = true
) extends HasSharedOptions {
  def areAnyRulesEnabled: Boolean = enableScalafix || enableBuiltInRules
}

object FixOptions {
  implicit lazy val parser: Parser[FixOptions] = Parser.derive
  implicit lazy val help: Help[FixOptions]     = Help.derive

  val cmdName = "fix"
  private val helpHeader =
    s"Run $fullRunnerName & Scalafix rules to lint, rewrite or otherwise rearrange a $fullRunnerName project."
  val helpMessage: String = HelpMessages.shortHelpMessage(cmdName, helpHeader)
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |`scalafix` is used to check project code or rewrite it under the hood with use of specified rules.
       |
       |All standard $fullRunnerName inputs are accepted, but only Scala sources will be refactored (.scala and .sc files).
       |
       |${HelpMessages.commandConfigurations(cmdName)}
       |
       |${HelpMessages.acceptedInputs}
       |
       |${HelpMessages.commandDocWebsiteReference(cmdName)}""".stripMargin
}
