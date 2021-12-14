package scala.cli.commands

import caseapp._

// format: off
final case class AddPathOptions(
  @Recurse
    verbosityOptions: VerbosityOptions = VerbosityOptions(),
  @Group("Logging")
  @Name("q")
    quiet: Boolean = false,
  title: String = ""
) {
  // format: on
  lazy val verbosity = verbosityOptions.verbosity - (if (quiet) 1 else 0)
}

object AddPathOptions {
  implicit lazy val parser: Parser[AddPathOptions] = Parser.derive
  implicit lazy val help: Help[AddPathOptions]     = Help.derive
}
