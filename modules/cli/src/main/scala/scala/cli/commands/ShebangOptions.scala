package scala.cli.commands

import caseapp._

final case class ShebangOptions(
  @Recurse
  runOptions: RunOptions = RunOptions()
)

object ShebangOptions {
  implicit lazy val parser: Parser[ShebangOptions] = Parser.derive
  implicit lazy val help: Help[ShebangOptions]     = Help.derive
}
