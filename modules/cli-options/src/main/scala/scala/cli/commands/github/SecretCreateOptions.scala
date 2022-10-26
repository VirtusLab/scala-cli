package scala.cli.commands.github

import caseapp.*

import scala.cli.commands.CoursierOptions

// format: off
final case class SecretCreateOptions(
  @Recurse
    shared: SharedSecretOptions = SharedSecretOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions(),
  @ExtraName("pubKey")
    publicKey: Option[String] = None,
  @ExtraName("n")
    dummy: Boolean = false,
  @Hidden
    printRequest: Boolean = false
) extends HasSharedSecretOptions
// format: on

object SecretCreateOptions {
  implicit lazy val parser: Parser[SecretCreateOptions] = Parser.derive
  implicit lazy val help: Help[SecretCreateOptions]     = Help.derive
}
