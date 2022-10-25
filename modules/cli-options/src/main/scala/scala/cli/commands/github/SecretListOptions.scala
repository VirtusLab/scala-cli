package scala.cli.commands.github

import caseapp._

// format: off
final case class ListSecretsOptions(
  @Recurse
    shared: SharedSecretOptions
) extends HasSharedSecretOptions
// format: on

object ListSecretsOptions {
  implicit lazy val parser: Parser[ListSecretsOptions] = Parser.derive
  implicit lazy val help: Help[ListSecretsOptions]     = Help.derive
}
