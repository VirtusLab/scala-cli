package scala.cli.commands.publish

import caseapp.*

import scala.cli.ScalaCli.baseRunnerName
import scala.cli.commands.pgp.PgpScalaSigningOptions
import scala.cli.commands.shared.*
import scala.cli.commands.tags

// format: off
@HelpMessage(PublishOptions.helpMessage, "", PublishOptions.detailedHelpMessage)
final case class PublishOptions(
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
    publishRepo: PublishRepositoryOptions = PublishRepositoryOptions(),
  @Recurse
    sharedPublish: SharedPublishOptions = SharedPublishOptions(),
  @Recurse
    signingCli: PgpScalaSigningOptions = PgpScalaSigningOptions(),

  @Group(HelpGroup.Publishing.toString)
  @Tag(tags.restricted)
  @Hidden
    ivy2LocalLike: Option[Boolean] = None,

  @Group(HelpGroup.Publishing.toString)
  @Tag(tags.restricted)
  @Hidden
    parallelUpload: Option[Boolean] = None
) extends HasSharedOptions
// format: on

object PublishOptions {
  implicit lazy val parser: Parser[PublishOptions] = Parser.derive
  implicit lazy val help: Help[PublishOptions]     = Help.derive

  val cmdName            = "publish"
  private val helpHeader = "Publishes build artifacts to Maven repositories."
  val helpMessage: String =
    s"""$helpHeader
       |
       |${HelpMessages.commandFullHelpReference(cmdName, needsPower = true)}
       |${HelpMessages.commandDocWebsiteReference(s"publishing/$cmdName")}""".stripMargin
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |We recommend running the `publish setup` sub-command once prior to
       |running `publish` in order to set missing `using` directives for publishing.
       |(but this is not mandatory)
       |    $baseRunnerName --power publish setup .
       |
       |${HelpMessages.commandConfigurations(cmdName)}
       |
       |${HelpMessages.acceptedInputs}
       |
       |${HelpMessages.commandDocWebsiteReference(s"publishing/$cmdName")}""".stripMargin
}
