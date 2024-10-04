package scala.cli.commands.scalafix

import caseapp.*
import coursier.core.Version

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.FetchExternalBinary
import scala.build.options.BuildOptions
import scala.cli.ScalaCli.fullRunnerName
import scala.cli.commands.shared.{HasSharedOptions, HelpGroup, HelpMessages, SharedOptions}
import scala.cli.commands.{Constants, tags}
import scala.util.Properties

// format: off
@HelpMessage(ScalafixOptions.helpMessage, "", ScalafixOptions.detailedHelpMessage)
final case class ScalafixOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),

  @Group(HelpGroup.Format.toString)
  @Tag(tags.experimental)
  @HelpMessage("Custom path to the scalafix configuration file.")
  @Tag(tags.inShortHelp)
  scalafixConf: Option[String] = None,

  @Group(HelpGroup.Format.toString)
  @Tag(tags.experimental)
  @HelpMessage("Pass extra argument(s) to scalafix.")
  @Tag(tags.inShortHelp)
  scalafixArg: List[String] = Nil,

  @Group(HelpGroup.Format.toString)
  @Tag(tags.experimental)
  @HelpMessage("Run rule(s) explicitly, overriding the configuration file default.")
  @Tag(tags.inShortHelp)
  rules: List[String] = Nil,

  @Group(HelpGroup.Format.toString)
  @Tag(tags.experimental)
  @HelpMessage("Fail the invocation instead of applying fixes")
  @Tag(tags.inShortHelp)
  check: Boolean = false,
) extends HasSharedOptions {
  def buildOptions: Either[BuildException, BuildOptions] = shared.buildOptions()

}
object ScalafixOptions {
  implicit lazy val parser: Parser[ScalafixOptions] = Parser.derive
  implicit lazy val help: Help[ScalafixOptions]     = Help.derive

  val cmdName             = "scalafix"
  private val helpHeader  = "Fixes Scala code according to scalafix rules."
  val helpMessage: String = HelpMessages.shortHelpMessage(cmdName, helpHeader)
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |`scalafix` is used to check project code or rewrite it under the hood with use of specified rules.
       |
       |The `.scalafix.conf` configuration file is optional.
       |Default configuration values will be assumed by $fullRunnerName.
       |
       |All standard $fullRunnerName inputs are accepted, but only Scala sources will be refactored (.scala and .sc files).
       |
       |${HelpMessages.commandDocWebsiteReference(cmdName)}""".stripMargin
}
