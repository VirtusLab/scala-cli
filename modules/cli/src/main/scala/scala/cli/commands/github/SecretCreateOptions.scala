package scala.cli.commands.github

import caseapp.*

import scala.cli.ScalaCli.progName
import scala.cli.commands.shared.{CoursierOptions, HelpGroup, HelpMessages}
import scala.cli.commands.tags

// format: off
@HelpMessage(
  s"""Creates or updates a GitHub repository secret.
    |  $progName --power github secret create --repo repo-org/repo-name SECRET_VALUE=value:secret""".stripMargin
)
final case class SecretCreateOptions(
  @Recurse
    shared: SharedSecretOptions = SharedSecretOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions(),
  @Group(HelpGroup.Secret.toString)
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
  @ExtraName("pubKey")
    publicKey: Option[String] = None,
  @Tag(tags.implementation)
  @ExtraName("n")
    dummy: Boolean = false,
  @Hidden
  @Tag(tags.implementation)
  @Group(HelpGroup.Secret.toString)
    printRequest: Boolean = false
) extends HasSharedSecretOptions
// format: on

object SecretCreateOptions {
  implicit lazy val parser: Parser[SecretCreateOptions] = Parser.derive
  implicit lazy val help: Help[SecretCreateOptions]     = Help.derive
}
