package scala.cli.commands

import caseapp._

import scala.build.internal.{Constants, FetchExternalBinary}
import scala.build.options.BuildOptions
import scala.cli.commands.util.SharedOptionsUtil._
import scala.util.Properties

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
) {
  // format: on

  def binaryUrl(versionMaybe: Option[String]): (String, Boolean) = {
    val defaultVersion = versionMaybe.getOrElse(Constants.defaultScalafmtVersion)
    val osArchSuffix0 = osArchSuffix.map(_.trim).filter(_.nonEmpty)
      .getOrElse(FetchExternalBinary.platformSuffix())
    val tag0           = scalafmtTag.getOrElse("v" + defaultVersion)
    val gitHubOrgName0 = scalafmtGithubOrgName.getOrElse("alexarchambault/scalafmt-native-image")
    val extension0     = if (Properties.isWin) ".zip" else ".gz"
    val url =
      s"https://github.com/$gitHubOrgName0/releases/download/$tag0/scalafmt-$osArchSuffix0$extension0"
    (url, !tag0.startsWith("v"))
  }

  def buildOptions: BuildOptions =
    shared.buildOptions(enableJmh = false, jmhVersion = None, ignoreErrors = false)

  def scalafmtCliOptions: List[String] =
    scalafmtArg ::: (if (check) List("--check") else Nil)

}

object FmtOptions {
  implicit lazy val parser: Parser[FmtOptions] = Parser.derive
  implicit lazy val help: Help[FmtOptions]     = Help.derive
}
