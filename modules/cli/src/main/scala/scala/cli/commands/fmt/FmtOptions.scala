package scala.cli.commands.fmt

import caseapp.*
import coursier.core.Version

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.FetchExternalBinary
import scala.build.options.BuildOptions
import scala.cli.ScalaCli.fullRunnerName
import scala.cli.commands.shared.{HasSharedOptions, HelpMessages, SharedOptions}
import scala.cli.commands.{Constants, tags}
import scala.util.Properties

// format: off
@HelpMessage(FmtOptions.helpMessage, "", FmtOptions.detailedHelpMessage)
final case class FmtOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),

  @Group("Format")
  @Tag(tags.should)
  @Tag(tags.inShortHelp)
  @HelpMessage("Check if sources are well formatted")
    check: Boolean = false,

  @Group("Format")
  @Tag(tags.implementation)
  @HelpMessage("Use project filters defined in the configuration. Turned on by default, use `--respect-project-filters:false` to disable it.")
    respectProjectFilters: Boolean = true,

  @Group("Format")
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
  @HelpMessage("Saves .scalafmt.conf file if it was created or overwritten")
    saveScalafmtConf: Boolean = false,

  @Group("Format")
  @Tag(tags.implementation)
  @Hidden
    osArchSuffix: Option[String] = None,
  @Group("Format")
  @Tag(tags.implementation)
  @Hidden
    scalafmtTag: Option[String] = None,
  @Group("Format")
  @Tag(tags.implementation)
  @Hidden
    scalafmtGithubOrgName: Option[String] = None,
  @Group("Format")
  @Tag(tags.implementation)
  @Hidden
    scalafmtExtension: Option[String] = None,
  @Group("Format")
  @Tag(tags.implementation)
  @Hidden
    scalafmtLauncher: Option[String] = None,

  @Group("Format")
  @Name("F")
  @Tag(tags.implementation)
  @HelpMessage("Pass an argument to scalafmt.")
  @Tag(tags.inShortHelp)
    scalafmtArg: List[String] = Nil,

  @Group("Format")
  @HelpMessage("Custom path to the scalafmt configuration file.")
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
  @Name("scalafmtConfig")
    scalafmtConf: Option[String] = None,
  @Group("Format")
  @Tag(tags.implementation)
  @HelpMessage("Pass configuration as a string.")
  @Name("scalafmtConfigStr")
  @Name("scalafmtConfSnippet")
    scalafmtConfStr: Option[String] = None,
  @Tag(tags.implementation)
  @Group("Format")
  @HelpMessage("Pass a global dialect for scalafmt. This overrides whatever value is configured in the .scalafmt.conf file or inferred based on Scala version used.")
  @Tag(tags.implementation)
  @Name("dialect")
  @Tag(tags.inShortHelp)
    scalafmtDialect: Option[String] = None,
  @Tag(tags.implementation)
  @Group("Format")
  @HelpMessage(s"Pass scalafmt version before running it (${Constants.defaultScalafmtVersion} by default). If passed, this overrides whatever value is configured in the .scalafmt.conf file.")
  @Name("fmtVersion")
  @Tag(tags.inShortHelp)
    scalafmtVersion: Option[String] = None
) extends HasSharedOptions {
  // format: on

  def binaryUrl(version: String): (String, Boolean) = {
    val osArchSuffix0 = osArchSuffix.map(_.trim).filter(_.nonEmpty)
      .getOrElse(FetchExternalBinary.platformSuffix())
    val tag0 = scalafmtTag.getOrElse("v" + version)
    val gitHubOrgName0 = scalafmtGithubOrgName.getOrElse {
      if (Version(version) < Version("3.5.9"))
        "scala-cli/scalafmt-native-image"
      else // from version 3.5.9 scalafmt-native-image repository was moved to VirtusLab organisation
        "virtuslab/scalafmt-native-image"
    }
    val extension0 = if (Properties.isWin) ".zip" else ".gz"
    val url =
      s"https://github.com/$gitHubOrgName0/releases/download/$tag0/scalafmt-$osArchSuffix0$extension0"
    (url, !tag0.startsWith("v"))
  }

  def buildOptions: Either[BuildException, BuildOptions] = shared.buildOptions()

  def scalafmtCliOptions: List[String] =
    scalafmtArg :::
      (if (check && !scalafmtArg.contains("--check")) List("--check") else Nil) :::
      (if (respectProjectFilters && !scalafmtArg.contains("--respect-project-filters"))
         List("--respect-project-filters")
       else Nil)

}
object FmtOptions {
  implicit lazy val parser: Parser[FmtOptions] = Parser.derive
  implicit lazy val help: Help[FmtOptions]     = Help.derive

  val cmdName             = "fmt"
  private val helpHeader  = "Formats Scala code."
  val helpMessage: String = HelpMessages.shortHelpMessage(cmdName, helpHeader)
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |`scalafmt` is used to perform the formatting under the hood.
       |
       |The `.scalafmt.conf` configuration file is optional.
       |Default configuration values will be assumed by $fullRunnerName.
       |
       |All standard $fullRunnerName inputs are accepted, but only Scala sources will be formatted (.scala and .sc files).
       |
       |${HelpMessages.commandDocWebsiteReference(cmdName)}""".stripMargin
}
