package scala.cli.commands.publish

import caseapp.*

import scala.cli.commands.common.HasSharedOptions
import scala.cli.commands.{
  CrossOptions,
  MainClassOptions,
  SharedDirectoriesOptions,
  SharedOptions,
  SharedPythonOptions,
  SharedWatchOptions,
  tags
}

// format: off
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
    sharedPython: SharedPythonOptions = SharedPythonOptions(),

  @Group("Publishing")
  @Hidden
  @Tag(tags.experimental)
    ivy2LocalLike: Option[Boolean] = None,

  @Group("Publishing")
  @Hidden
  @Tag(tags.experimental)
    parallelUpload: Option[Boolean] = None
) extends HasSharedOptions
// format: on

object PublishOptions {
  implicit lazy val parser: Parser[PublishOptions] = Parser.derive
  implicit lazy val help: Help[PublishOptions]     = Help.derive
}
