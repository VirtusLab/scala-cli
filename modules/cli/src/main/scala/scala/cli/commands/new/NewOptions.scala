package scala.cli.commands.`new`

import caseapp.*

import scala.cli.commands.shared.*

@HelpMessage(NewOptions.newMessage, "", NewOptions.detailedNewMessage)
final case class NewOptions(
  @Recurse
  global: GlobalOptions = GlobalOptions(),
  @Recurse
  shared: SharedOptions = SharedOptions(),
  out: Option[String] = None,
) extends HasGlobalOptions

object NewOptions {
  implicit lazy val parser: Parser[NewOptions] = Parser.derive
  implicit lazy val help: Help[NewOptions]     = Help.derive
  val cmdName                                  = "new"
  private val newHeader                        = "New giter8 template."
  val newMessage: String                       = HelpMessages.shortHelpMessage(cmdName, newHeader)

  val detailedNewMessage: String =
    s"""$newHeader
       |
       | Creates a new project from a giter8 template.
       |
       |""".stripMargin
}
