package scala.cli.commands.github

import caseapp.*

import scala.cli.commands.CoursierOptions
import scala.cli.commands.tags

// format: off
final case class SecretCreateOptions(
  @Recurse
    shared: SharedSecretOptions = SharedSecretOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions(),
  @ExtraName("pubKey")
  @Tag(tags.experimental)
    publicKey: Option[String] = None,
  @ExtraName("n")
  @Hidden
  @Tag(tags.experimental)
    dummy: Boolean = false,
  @Hidden
  @Tag(tags.experimental)
    printRequest: Boolean = false
) extends HasSharedSecretOptions
// format: on

object SecretCreateOptions {
  implicit lazy val parser: Parser[SecretCreateOptions] = Parser.derive
  implicit lazy val help: Help[SecretCreateOptions]     = Help.derive
}
