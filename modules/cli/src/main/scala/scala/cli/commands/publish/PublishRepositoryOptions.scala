package scala.cli.commands.publish

import caseapp.*

import scala.cli.commands.shared.HelpGroup
import scala.cli.commands.tags
import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.ArgParsers.*

// format: off
final case class PublishRepositoryOptions(

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Repository to publish to")
  @ValueDescription("URL or path")
  @ExtraName("R")
  @ExtraName("publishRepo")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    publishRepository: Option[String] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("User to use with publishing repository")
  @ValueDescription("user")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    user: Option[PasswordOption] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Password to use with publishing repository")
  @ValueDescription("value:…")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    password: Option[PasswordOption] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Realm to use when passing credentials to publishing repository")
  @ValueDescription("realm")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    realm: Option[String] = None

)
// format: on

object PublishRepositoryOptions {
  implicit lazy val parser: Parser[PublishRepositoryOptions] = Parser.derive
  implicit lazy val help: Help[PublishRepositoryOptions]     = Help.derive
}
