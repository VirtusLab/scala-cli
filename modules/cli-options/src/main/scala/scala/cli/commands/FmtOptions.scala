package scala.cli.commands

import caseapp.*

import scala.cli.commands.Constants
import scala.cli.commands.common.HasSharedOptions

// format: off
@HelpMessage("Format Scala code")
final case class FmtOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),

  @Group("Format")
  @Tag(tags.should)
  @HelpMessage("Check if sources are well formatted")
    check: Boolean = false,

  @Group("Format")
  @HelpMessage("Use project filters defined in the configuration. Turned on by default, use `--respect-project-filters:false` to disable it.")
  @Tag(tags.implementation)
    respectProjectFilters: Boolean = true,

  @Group("Format")
  @HelpMessage("Saves .scalafmt.conf file if it was created or overwritten")
  @Tag(tags.implementation)
    saveScalafmtConf: Boolean = false,

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
  @Tag(tags.internal)
    scalafmtArg: List[String] = Nil,

  @Group("Format")
  @HelpMessage("Custom path to the scalafmt configuration file.")
  @Tag(tags.implementation)
  @Name("scalafmtConfig")
    scalafmtConf: Option[String] = None,
  @Group("Format")
  @HelpMessage("Pass configuration as a string.")
  @Name("scalafmtConfigStr")
  @Name("scalafmtConfSnippet")
  @Tag(tags.internal)
    scalafmtConfStr: Option[String] = None,
  @Tag(tags.implementation)
  @Group("Format")
  @HelpMessage("Pass a global dialect for scalafmt. This overrides whatever value is configured in the .scalafmt.conf file or inferred based on Scala version used.")
  @Tag(tags.implementation)
  @Name("dialect")
    scalafmtDialect: Option[String] = None,
  @Tag(tags.implementation)
  @Group("Format")
  @HelpMessage(s"Pass scalafmt version before running it (${Constants.defaultScalafmtVersion} by default). If passed, this overrides whatever value is configured in the .scalafmt.conf file.")
  @Name("fmtVersion")
    scalafmtVersion: Option[String] = None
) extends HasSharedOptions
// format: on
object FmtOptions {
  implicit lazy val parser: Parser[FmtOptions] = Parser.derive
  implicit lazy val help: Help[FmtOptions]     = Help.derive
}
