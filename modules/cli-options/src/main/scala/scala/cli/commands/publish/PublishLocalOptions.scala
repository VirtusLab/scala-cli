package scala.cli.commands.publish

import caseapp._

import scala.cli.commands.{CrossOptions, MainClassOptions, SharedOptions, SharedWatchOptions}

// format: off
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
    sharedPublish: SharedPublishOptions = SharedPublishOptions()
)
// format: on

object PublishLocalOptions {
  implicit lazy val parser: Parser[PublishLocalOptions] = Parser.derive
  implicit lazy val help: Help[PublishLocalOptions]     = Help.derive
}
