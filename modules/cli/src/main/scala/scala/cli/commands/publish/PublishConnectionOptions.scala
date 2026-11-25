package scala.cli.commands.publish

import caseapp.*

import scala.cli.commands.shared.HelpGroup
import scala.cli.commands.tags

// format: off
final case class PublishConnectionOptions(
  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Connection timeout, in seconds.")
  @Tag(tags.restricted)
  @Hidden
    connectionTimeoutSeconds: Option[Int] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("How many times to attempt Sonatype API requests failing with a transient error (connection failure, HTTP 5xx, or a timeout for requests that don't change anything). Client errors (HTTP 4xx) are not retried.")
  @Tag(tags.restricted)
  @Hidden
    connectionTimeoutRetries: Option[Int] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Waiting for response timeout, in seconds.")
  @Tag(tags.restricted)
  @Hidden
    responseTimeoutSeconds: Option[Int] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("How many times to attempt the Sonatype staging repository operations (close, promote, drop) failing with a transient error (connection failure, HTTP 5xx). Client errors (HTTP 4xx) are not retried.")
  @Tag(tags.restricted)
  @Hidden
    stagingRepoRetries: Option[Int] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Time to wait between staging repository operation retries, in milliseconds.")
  @Tag(tags.restricted)
  @Hidden
    stagingRepoWaitTimeMilis: Option[Int] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Time to wait before retrying a failed Sonatype API request, in milliseconds, doubled before each subsequent retry. 1000 by default.")
  @Tag(tags.experimental)
  @Hidden
    connectionRetryWaitTimeMillis: Option[Int] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("How many times to check whether the Sonatype staging repository got closed or released, before giving up. 20 by default.")
  @Tag(tags.experimental)
  @Hidden
    stagingRepoStatusPollAttempts: Option[Int] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Time to wait before checking the Sonatype staging repository status again, in milliseconds, multiplied by 1.5 before each subsequent check. 3000 by default.")
  @Tag(tags.experimental)
  @Hidden
    stagingRepoStatusPollWaitTimeMillis: Option[Int] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("How many times to attempt uploading a file to an HTTP repository, when that fails with a transient error (HTTP 5xx). 3 by default.")
  @Tag(tags.experimental)
  @Hidden
    uploadRetries: Option[Int] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Time to wait before retrying a failed upload, in milliseconds, doubled before each subsequent retry. 1000 by default.")
  @Tag(tags.experimental)
  @Hidden
    uploadRetryWaitTimeMillis: Option[Int] = None
)
  // format: on

object PublishConnectionOptions {
  implicit lazy val parser: Parser[PublishConnectionOptions] = Parser.derive
  implicit lazy val help: Help[PublishConnectionOptions]     = Help.derive
}
