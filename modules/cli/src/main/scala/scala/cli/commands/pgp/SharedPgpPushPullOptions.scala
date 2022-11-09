package scala.cli.commands.pgp

import caseapp.*

import scala.cli.commands.shared.LoggingOptions

// format: off
final case class SharedPgpPushPullOptions(
  @Group("PGP")
  @HelpMessage("Key server to push / pull keys from")
  @ValueDescription("URL")
    keyServer: List[String] = Nil
)
// format: on

object SharedPgpPushPullOptions {
  implicit lazy val parser: Parser[SharedPgpPushPullOptions] = Parser.derive
  implicit lazy val help: Help[SharedPgpPushPullOptions]     = Help.derive
}
