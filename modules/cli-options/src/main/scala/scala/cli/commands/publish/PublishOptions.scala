package scala.cli.commands.publish

import caseapp._

import scala.cli.commands.{
  CompileCrossOptions,
  MainClassOptions,
  SharedDirectoriesOptions,
  SharedOptions,
  SharedWatchOptions
}

// format: off
final case class PublishOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    watch: SharedWatchOptions = SharedWatchOptions(),
  @Recurse
    compileCross: CompileCrossOptions = CompileCrossOptions(),
  @Recurse
    mainClass: MainClassOptions = MainClassOptions(),
  @Recurse
    publishParams: PublishParamsOptions = PublishParamsOptions(),
  @Recurse
    publishRepo: PublishRepositoryOptions = PublishRepositoryOptions(),
  @Recurse
    sharedPublish: SharedPublishOptions = SharedPublishOptions(),

  @Group("Publishing")
  @Hidden
    ivy2LocalLike: Option[Boolean] = None,

  @Group("Publishing")
  @Hidden
    parallelUpload: Option[Boolean] = None
)
// format: on

object PublishOptions {
  implicit lazy val parser: Parser[PublishOptions] = Parser.derive
  implicit lazy val help: Help[PublishOptions]     = Help.derive
}
