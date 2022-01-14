package scala.cli.launcher

import caseapp._
import upickle.default.{ReadWriter, macroRW}

@HelpMessage("Set the ScalaCLI version")
final case class LauncherOptions(
  @Group("Launcher")
  scalaCliVersion: Option[String] = None,
  @Group("Launcher")
  @HelpMessage("The version of Scala on which scalaCLI was published")
  @ValueDescription("2.12|2.13|3")
  publishedScalaVersion: Option[String] = None
)

object LauncherOptions {
  lazy val parser: Parser[LauncherOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[LauncherOptions, parser.D] = parser
  implicit lazy val help: Help[LauncherOptions]                      = Help.derive
  implicit lazy val jsonCodec: ReadWriter[LauncherOptions]           = macroRW
}
