package scala.cli.commands.doc

import caseapp.*
import caseapp.core.help.Help

import scala.cli.ScalaCli.fullRunnerName
import scala.cli.commands.shared.{HasSharedOptions, HelpGroup, HelpMessages, SharedOptions}
import scala.cli.commands.tags

// format: off
@HelpMessage(DocOptions.helpMessage, DocOptions.messageMd, DocOptions.detailedHelpMessage)
final case class DocOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Group(HelpGroup.Doc.toString)
  @Tag(tags.must)
  @HelpMessage("Set the destination path")
  @Name("o")
    output: Option[String] = None,
  @Group(HelpGroup.Doc.toString)
  @HelpMessage("Overwrite the destination directory, if it exists")
  @Tag(tags.must)
  @Name("f")
    force: Boolean = false,
  @Group(HelpGroup.Doc.toString)
  @HelpMessage(s"Control if $fullRunnerName should use default options for scaladoc, true by default. Use `--default-scaladoc-opts:false` to not include default options.")
  @Tag(tags.should)
  @ExtraName("defaultScaladocOpts")
    defaultScaladocOptions: Option[Boolean] = None,
) extends HasSharedOptions
// format: on

object DocOptions {
  implicit lazy val parser: Parser[DocOptions] = Parser.derive
  implicit lazy val help: Help[DocOptions]     = Help.derive
  val cmdName                                  = "doc"
  private val helpHeader                       = "Generate Scaladoc documentation."
  val helpMessage: String                      = HelpMessages.shortHelpMessage(cmdName, helpHeader)
  val detailedHelpMessage: String =
    s"""Generate Scaladoc documentation.
       |
       |${HelpMessages.acceptedInputs}
       |
       |${HelpMessages.commandDocWebsiteReference(cmdName)}""".stripMargin
  val messageMd =
    s"By default, $fullRunnerName sets common `scaladoc` options and this mechanism can be disabled by using `--default-scaladoc-opts:false`."
}
