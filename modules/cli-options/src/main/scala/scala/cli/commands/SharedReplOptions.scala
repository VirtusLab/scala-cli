package scala.cli.commands

import caseapp.*
import caseapp.core.help.Help

// format: off
final case class SharedReplOptions(
  @Recurse
    sharedJava: SharedJavaOptions = SharedJavaOptions(),
  @Recurse
    watch: SharedWatchOptions = SharedWatchOptions(),
  @Recurse
    compileCross: CrossOptions = CrossOptions(),
  
  @Group("Repl")
  @HelpMessage("[restricted] Use Ammonite (instead of the default Scala REPL)")
  @Name("A")
  @Name("amm")
    ammonite: Option[Boolean] = None,

  @Group("Repl")
  @HelpMessage("[restricted] Set the Ammonite version")
  @Name("ammoniteVer")
    ammoniteVersion: Option[String] = None,

  @Group("Repl")
  @Name("a")
  @HelpMessage("[restricted] Provide arguments for ammonite repl")
  @Hidden
    ammoniteArg: List[String] = Nil,

  @Group("Repl")
  @Hidden
  @HelpMessage("Don't actually run the REPL, just fetch it")
    replDryRun: Boolean = false
)
// format: on

object SharedReplOptions {
  implicit lazy val parser: Parser[SharedReplOptions] = Parser.derive
  implicit lazy val help: Help[SharedReplOptions]     = Help.derive
  // Parser.Aux for using SharedReplOptions with @Recurse in other options
  implicit lazy val parserAux: Parser.Aux[SharedReplOptions, parser.D] = parser
}
