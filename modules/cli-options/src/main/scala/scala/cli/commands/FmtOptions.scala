package scala.cli.commands

import caseapp._

// format: off
@HelpMessage("Format Scala code")
final case class FmtOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),

  @Group("Format")
  @HelpMessage("Check that sources are well formatted")
    check: Boolean = false,

  @Group("Format")
  @Hidden
    osArchSuffix: Option[String] = None,
  @Group("Format")
  @Hidden
    scalafmtTag: Option[String] = None,
  @Group("Format")
  @Hidden
    scalafmtGithubOrgName: Option[String] = None,
  @Group("Format")
  @Hidden
    scalafmtExtension: Option[String] = None,
  @Group("Format")
  @Hidden
    scalafmtLauncher: Option[String] = None,

  @Group("Format")
  @Name("F")
  @Hidden
    scalafmtArg: List[String] = Nil,

  @Group("Format")
    dialect: Option[String] = None
)
// format: on
object FmtOptions {
  implicit lazy val parser: Parser[FmtOptions] = Parser.derive
  implicit lazy val help: Help[FmtOptions]     = Help.derive
}
