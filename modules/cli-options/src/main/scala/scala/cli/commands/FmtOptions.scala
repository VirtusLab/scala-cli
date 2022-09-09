package scala.cli.commands

import caseapp._

// format: off
@HelpMessage("Format Scala code")
final case class FmtOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),

  @Group("Format")
  @HelpMessage("Check if sources are well formatted")
    check: Boolean = false,

  @Group("Format")
  @HelpMessage("Use project filters defined in the configuration. Turned on by default, use `--respect-project-filters:false` to disable it.")
    respectProjectFilters: Boolean = true,

  @Group("Format")
  @HelpMessage("Saves .scalafmt.conf file if it was created or overwritten")
    saveScalafmtConf: Boolean = false,

  @Group("Format")
  @HelpMessage("Show help for scalafmt. This is an alias for --scalafmt-arg -help")
  @Name("fmtHelp")
    scalafmtHelp: Boolean = false,

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
  @HelpMessage("Pass argument to scalafmt.")
    scalafmtArg: List[String] = Nil,

  @Group("Format")
  @HelpMessage("Custom path to the scalafmt configuration file.")
  @Name("scalafmtConfig")
    scalafmtConf: Option[String] = None,
  @Group("Format")
  @HelpMessage("Pass configuration as a string.")
  @Name("scalafmtConfigStr")
  @Name("scalafmtConfSnippet")
    scalafmtConfStr: Option[String] = None,
  @Group("Format")
  @HelpMessage("Pass a global dialect for scalafmt. This overrides whatever value is configured in the .scalafmt.conf file or inferred based on Scala version used.")
  @Name("dialect")
    scalafmtDialect: Option[String] = None,
  @Group("Format")
  @HelpMessage("Pass scalafmt version before running it. This overrides whatever value is configured in the .scalafmt.conf file.")
  @Name("fmtVersion")
    scalafmtVersion: Option[String] = None
)
// format: on
object FmtOptions {
  implicit lazy val parser: Parser[FmtOptions] = Parser.derive
  implicit lazy val help: Help[FmtOptions]     = Help.derive
}
