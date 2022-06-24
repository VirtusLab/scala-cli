package scala.cli.commands.publish

import caseapp._

import scala.build.options.publish.MaybeConfigPasswordOption
import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.ArgParsers._
import scala.cli.util.ArgParsers._

// format: off
final case class PublishParamsOptions(

  @Group("Publishing")
  @HelpMessage("Organization to publish artifacts under")
    organization: Option[String] = None,
  @Group("Publishing")
  @HelpMessage("Name to publish artifacts as")
    name: Option[String] = None,
  @Group("Publishing")
  @HelpMessage("Final name to publish artifacts as, including Scala version and platform suffixes if any")
    moduleName: Option[String] = None,
  @Group("Publishing")
  @HelpMessage("Version to publish artifacts as")
    version: Option[String] = None,
  @Group("Publishing")
  @HelpMessage("How to compute the version to publish artifacts as")
    computeVersion: Option[String] = None,
  @Group("Publishing")
  @HelpMessage("URL to put in publishing metadata")
    url: Option[String] = None,
  @Group("Publishing")
  @HelpMessage("License to put in publishing metadata")
  @ValueDescription("name:URL")
    license: Option[String] = None,
  @Group("Publishing")
  @HelpMessage("VCS information to put in publishing metadata")
    vcs: Option[String] = None,
  @Group("Publishing")
  @HelpMessage("Description to put in publishing metadata")
    description: Option[String] = None,
  @Group("Publishing")
  @HelpMessage("Developer(s) to add in publishing metadata, like \"alex|Alex|https://alex.info\" or \"alex|Alex|https://alex.info|alex@alex.me\"")
  @ValueDescription("id|name|URL|email")
    developer: List[String] = Nil,

  @Group("Publishing")
  @HelpMessage("Secret key to use to sign artifacts with Bouncy Castle")
    secretKey: Option[MaybeConfigPasswordOption] = None,

  @Group("Publishing")
  @HelpMessage("Password of secret key to use to sign artifacts with Bouncy Castle")
  @ValueDescription("value:…")
  @ExtraName("secretKeyPass")
    secretKeyPassword: Option[MaybeConfigPasswordOption] = None,

  @Group("Publishing")
  @HelpMessage("Use or setup publish parameters meant to be used on continuous integration")
    ci: Option[Boolean] = None

) {
  // format: on

  def isCi: Boolean =
    ci.getOrElse(System.getenv("CI") != null)
}

object PublishParamsOptions {
  lazy val parser: Parser[PublishParamsOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[PublishParamsOptions, parser.D] = parser
  implicit lazy val help: Help[PublishParamsOptions]                      = Help.derive
}
