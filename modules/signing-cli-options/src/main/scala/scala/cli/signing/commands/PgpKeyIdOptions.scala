package scala.cli.signing.commands

import caseapp._

final case class PgpKeyIdOptions(
  fingerprint: Boolean = false
)

object PgpKeyIdOptions {
  implicit lazy val parser: Parser[PgpKeyIdOptions] = Parser.derive
  implicit lazy val help: Help[PgpKeyIdOptions]     = Help.derive
}
