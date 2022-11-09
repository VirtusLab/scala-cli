package scala.cli.commands.publish

import caseapp.*

import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.ArgParsers.*

// format: off
final case class PublishRepositoryOptions(

  @Group("Publishing")
  @HelpMessage("Repository to publish to")
  @ValueDescription("URL or path")
  @ExtraName("R")
  @ExtraName("publishRepo")
    publishRepository: Option[String] = None,

  @Group("Publishing")
  @HelpMessage("User to use with publishing repository")
  @ValueDescription("user")
    user: Option[PasswordOption] = None,

  @Group("Publishing")
  @HelpMessage("Password to use with publishing repository")
  @ValueDescription("value:…")
    password: Option[PasswordOption] = None,

  @Group("Publishing")
  @HelpMessage("Realm to use when passing credentials to publishing repository")
  @ValueDescription("realm")
    realm: Option[String] = None

)
// format: on

object PublishRepositoryOptions {
  implicit lazy val parser: Parser[PublishRepositoryOptions] = Parser.derive
  implicit lazy val help: Help[PublishRepositoryOptions]     = Help.derive
}
