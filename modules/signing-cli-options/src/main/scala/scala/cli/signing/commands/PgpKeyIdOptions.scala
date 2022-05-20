package scala.cli.signing.commands

import caseapp._

// format: off
final case class PgpKeyIdOptions(
  fingerprint: Boolean = false,
  @ExtraName("v")
    verbose: Int @@ Counter = Tag.of(0)
) {
  // format: on

  lazy val verbosity = Tag.unwrap(verbose)
}

object PgpKeyIdOptions {
  implicit lazy val parser: Parser[PgpKeyIdOptions] = Parser.derive
  implicit lazy val help: Help[PgpKeyIdOptions]     = Help.derive
}
