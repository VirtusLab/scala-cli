package scala.cli.commands.publish

import caseapp.*

import scala.cli.commands.pgp.PgpScalaSigningOptions
import scala.cli.commands.publish.PublishSetupOptions.{cmdName, docWebsiteSuffix, helpHeader}
import scala.cli.commands.shared.*

// format: off
@HelpMessage(PublishLocalOptions.helpMessage, "", PublishLocalOptions.detailedHelpMessage)
final case class PublishLocalOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    watch: SharedWatchOptions = SharedWatchOptions(),
  @Recurse
    compileCross: CrossOptions = CrossOptions(),
  @Recurse
    mainClass: MainClassOptions = MainClassOptions(),
  @Recurse
    publishParams: PublishParamsOptions = PublishParamsOptions(),
  @Recurse
    sharedPublish: SharedPublishOptions = SharedPublishOptions(),
  @Recurse
    scalaSigning: PgpScalaSigningOptions = PgpScalaSigningOptions(),
) extends HasSharedOptions
// format: on

object PublishLocalOptions {
  implicit lazy val parser: Parser[PublishLocalOptions] = Parser.derive
  implicit lazy val help: Help[PublishLocalOptions]     = Help.derive
  val cmdName                                           = "publish local"
  private val helpHeader       = "Publishes build artifacts to the local Ivy2 repository."
  private val docWebsiteSuffix = "publishing/publish-local"
  val helpMessage: String =
    s"""$helpHeader
       |
       |${HelpMessages.commandFullHelpReference(cmdName)}
       |${HelpMessages.commandDocWebsiteReference(docWebsiteSuffix)}""".stripMargin
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |${HelpMessages.commandDocWebsiteReference(docWebsiteSuffix)}""".stripMargin
}
