package scala.cli.commands.publish

import caseapp.*

import scala.cli.commands.shared.HelpGroup
import scala.cli.commands.tags
import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.ArgParsers.*
import scala.cli.util.ArgParsers.*
import scala.cli.util.MaybeConfigPasswordOption

// format: off
final case class PublishParamsOptions(

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Organization to publish artifacts under")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    organization: Option[String] = None,
  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Name to publish artifacts as")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    name: Option[String] = None,
  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Final name to publish artifacts as, including Scala version and platform suffixes if any")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    moduleName: Option[String] = None,
  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Version to publish artifacts as")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    version: Option[String] = None,
  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("How to compute the version to publish artifacts as")
  @Tag(tags.restricted)
    computeVersion: Option[String] = None,
  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("URL to put in publishing metadata")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    url: Option[String] = None,
  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("License to put in publishing metadata")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
  @ValueDescription("name:URL")
    license: Option[String] = None,
  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("VCS information to put in publishing metadata")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    vcs: Option[String] = None,
  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Description to put in publishing metadata")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    description: Option[String] = None,
  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Developer(s) to add in publishing metadata, like \"alex|Alex|https://alex.info\" or \"alex|Alex|https://alex.info|alex@alex.me\"")
  @ValueDescription("id|name|URL|email")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    developer: List[String] = Nil,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Secret key to use to sign artifacts with Bouncy Castle")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    secretKey: Option[MaybeConfigPasswordOption] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Password of secret key to use to sign artifacts with Bouncy Castle")
  @ValueDescription("value:…")
  @ExtraName("secretKeyPass")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    secretKeyPassword: Option[MaybeConfigPasswordOption] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Use or setup publish parameters meant to be used on continuous integration")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    ci: Option[Boolean] = None

) {
  // format: on

  def setupCi: Boolean =
    ci.getOrElse(false)
  def isCi: Boolean =
    ci.getOrElse(System.getenv("CI") != null)
}

object PublishParamsOptions {
  implicit lazy val parser: Parser[PublishParamsOptions] = Parser.derive
  implicit lazy val help: Help[PublishParamsOptions]     = Help.derive
}
