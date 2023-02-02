package scala.cli.commands.publish

import caseapp.*

import scala.cli.commands.pgp.PgpScalaSigningOptions
import scala.cli.commands.shared._

// format: off
@HelpMessage(
  s"""Publishes build artifacts to the local Ivy2 repository.
     |
     |The local Ivy2 repository usually lives under `~/.ivy2/local`.
     |It is taken into account most of the time by most Scala tools when fetching artifacts.
     |
     |${HelpMessages.commandConfigurations("publish local")}
     |
     |${HelpMessages.acceptedInputs}
     |
     |${HelpMessages.docsWebsiteReference}""".stripMargin)
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
}
