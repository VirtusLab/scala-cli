package scala.cli.signing.commands

import caseapp._

@HelpMessage("Verify PGP signatures")
final case class PgpVerifyOptions(
  key: String
) {
  def keyPath = os.Path(key, os.pwd)
}

object PgpVerifyOptions {
  implicit lazy val parser: Parser[PgpVerifyOptions] = Parser.derive
  implicit lazy val help: Help[PgpVerifyOptions]     = Help.derive
}
