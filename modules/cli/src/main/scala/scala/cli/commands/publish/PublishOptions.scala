package scala.cli.commands.publish

import caseapp.*

import scala.cli.ScalaCli.baseRunnerName
import scala.cli.commands.pgp.PgpScalaSigningOptions
import scala.cli.commands.shared.*

// format: off
@HelpMessage(
  s"""Publishes build artifacts to Maven repositories.
     |
     |We recommend running the `publish setup` sub-command once prior to
     |running `publish` in order to set missing `using` directives for publishing.
     |(but this is not mandatory)
     |    $baseRunnerName ${HelpMessages.PowerString}publish setup .
     |
     |${HelpMessages.commandConfigurations("publish")}
     |
     |${HelpMessages.acceptedInputs}
     |
     |${HelpMessages.commandDocWebsiteReference("publishing/publish")}""".stripMargin)
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

  @Group("Publishing")
  @Hidden
    ivy2LocalLike: Option[Boolean] = None,

  @Group("Publishing")
  @Hidden
    parallelUpload: Option[Boolean] = None
) extends HasSharedOptions
// format: on

object PublishOptions {
  implicit lazy val parser: Parser[PublishOptions] = Parser.derive
  implicit lazy val help: Help[PublishOptions]     = Help.derive
}
