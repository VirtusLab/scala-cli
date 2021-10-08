package scala.cli.commands

import caseapp._

import scala.build.internal.Constants
import scala.cli.internal.FetchExternalBinary
import scala.util.Properties

// format: off
@HelpMessage("Format Scala code")
final case class FmtOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @HelpMessage("Check that sources are well formatted")
    check: Boolean = false,

  @Hidden
    osArchSuffix: Option[String] = None,
  @Hidden
    scalafmtTag: Option[String] = None,
  @Hidden
    scalafmtGitHubOrgName: Option[String] = None,
  @Hidden
    scalafmtExtension: Option[String] = None,
  @Hidden
    scalafmtLauncher: Option[String] = None
) {
  // format: on

  def binaryUrl: (String, Boolean) = {
    val osArchSuffix0 = osArchSuffix.map(_.trim).filter(_.nonEmpty)
      .getOrElse(FetchExternalBinary.platformSuffix())
    val tag0           = scalafmtTag.getOrElse("v" + Constants.defaultScalafmtVersion)
    val gitHubOrgName0 = scalafmtGitHubOrgName.getOrElse("alexarchambault/scalafmt-native-image")
    val extension0     = if (Properties.isWin) ".zip" else ".gz"
    val url =
      s"https://github.com/$gitHubOrgName0/releases/download/$tag0/scalafmt-$osArchSuffix0$extension0"
    (url, !tag0.startsWith("v"))
  }

}

object FmtOptions {
  implicit lazy val parser: Parser[FmtOptions] = Parser.derive
  implicit lazy val help: Help[FmtOptions]     = Help.derive
}
