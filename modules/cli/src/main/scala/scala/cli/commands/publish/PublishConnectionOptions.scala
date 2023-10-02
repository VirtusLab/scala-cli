package scala.cli.commands.publish

import caseapp.*

import scala.cli.commands.shared.{HelpGroup, SharedVersionOptions}
import scala.cli.commands.tags

// format: off
final case class PublishConnectionOptions(
  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Connection timeout, in seconds.")
  @Tag(tags.restricted)
  @Hidden
    connectionTimeoutSeconds: Option[Int] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("How many times to retry establishing the connection on timeout.")
  @Tag(tags.restricted)
  @Hidden
    connectionTimeoutRetries: Option[Int] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Waiting for response timeout, in seconds.")
  @Tag(tags.restricted)
  @Hidden
    responseTimeoutSeconds: Option[Int] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("How many times to retry the staging repository operations on failure.")
  @Tag(tags.restricted)
  @Hidden
    stagingRepoRetries: Option[Int] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Time to wait between staging repository operation retries, in milliseconds.")
  @Tag(tags.restricted)
  @Hidden
    stagingRepoWaitTimeMilis: Option[Int] = None
)
  // format: on

object PublishConnectionOptions {
  implicit lazy val parser: Parser[PublishConnectionOptions] = Parser.derive
  implicit lazy val help: Help[PublishConnectionOptions]     = Help.derive
}
