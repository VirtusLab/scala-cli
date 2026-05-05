package scala.cli.commands.listtargets

import caseapp.*

import scala.cli.commands.shared.{HasSharedOptions, SharedOptions}

@HelpMessage(ListTargetsOptions.helpMessage, "", ListTargetsOptions.detailedHelpMessage)
final case class ListTargetsOptions(
  @Recurse
  shared: SharedOptions = SharedOptions()
) extends HasSharedOptions

object ListTargetsOptions {
  implicit lazy val parser: Parser[ListTargetsOptions] = Parser.derive
  implicit lazy val help: Help[ListTargetsOptions]     = Help.derive

  private val helpHeader =
    "Print the full matrix of declared build targets (platform x Scala version) as JSON."
  val helpMessage: String         = helpHeader
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |Reads `using` directives and CLI options from the inputs and emits one entry per
       |declared target, so external tools can enumerate the matrix without parsing
       |directives themselves.
       |
       |Each entry has shape `{ "platform": "JVM"|"JS"|"Native", "scalaVersion": "..." }`.
       |The `scalaVersion` field is omitted for pure-Java projects.""".stripMargin
}
