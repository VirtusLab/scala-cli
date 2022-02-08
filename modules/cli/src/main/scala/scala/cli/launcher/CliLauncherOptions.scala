package scala.cli.launcher

import caseapp._

/** It is used for running another $1 (why another??? what's the default then???) Scala CLI version
  *
  * @param cliVersion
  *   The Scala CLI version
  * @param cliScalaVersion
  *   The version of Scala on which Scala CLI was published.
  * @param power
  *   whether to enable the use of power commands when called as ''scala''
  */
@HelpMessage("Run another Scala CLI version")
final case class CliLauncherOptions(
  @Group("Launcher")
  @HelpMessage("Set the Scala CLI version")
  cliVersion: Option[String] = None,
  @Group("Launcher")
  @HelpMessage("The version of Scala on which Scala CLI was published")
  @ValueDescription("2.12|2.13|3")
  cliScalaVersion: Option[String] = None,
  @Group("Launcher")
  @HelpMessage("When called as 'scala', allow to use power commands too")
  @Hidden
  power: Boolean = false
)

/** It provides a CaseApp Parser with the [[CliLauncherOptions]] that parses the arguments of the
  * command line $1 ??
  *
  * Is it OK to rename this to ScalaCLILauncherOptions?? $2
  */
object CliLauncherOptions {
  lazy val parser: Parser[CliLauncherOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[CliLauncherOptions, parser.D] = parser
  implicit lazy val help: Help[CliLauncherOptions]                      = Help.derive
}
