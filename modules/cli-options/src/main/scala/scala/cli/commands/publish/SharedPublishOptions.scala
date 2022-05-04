package scala.cli.commands.publish

import caseapp._

import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.ArgParsers._

// format: off
final case class SharedPublishOptions(

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
  @HelpMessage("Repository to publish to")
  @ValueDescription("URL or path")
  @ExtraName("R")
  @ExtraName("publishRepo")
    publishRepository: Option[String] = None,

  @Group("Publishing")
  @HelpMessage("Secret key to use to sign artifacts with BouncyCastle")
    secretKey: Option[String] = None,

  @Group("Publishing")
  @HelpMessage("Password of secret key to use to sign artifacts with BouncyCastle")
  @ValueDescription("value:…")
  @ExtraName("secretKeyPass")
    secretKeyPassword: Option[PasswordOption] = None

)
// format: on

object SharedPublishOptions {
  lazy val parser: Parser[SharedPublishOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[SharedPublishOptions, parser.D] = parser
  implicit lazy val help: Help[SharedPublishOptions]                      = Help.derive
}
