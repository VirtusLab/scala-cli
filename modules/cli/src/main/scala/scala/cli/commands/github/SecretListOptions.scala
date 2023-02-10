package scala.cli.commands.github

import caseapp.*

// format: off
@HelpMessage("Lists secrets for a given GitHub repository.")
final case class SecretListOptions(
  @Recurse
    shared: SharedSecretOptions
) extends HasSharedSecretOptions
// format: on

object SecretListOptions {
  implicit lazy val parser: Parser[SecretListOptions] = Parser.derive
  implicit lazy val help: Help[SecretListOptions]     = Help.derive
}
