package scala.cli.commands.publish

import caseapp.*

import scala.cli.commands.pgp.PgpScalaSigningOptions
import scala.cli.commands.shared.{
  CrossOptions,
  HasSharedOptions,
  MainClassOptions,
  SharedOptions,
  SharedPythonOptions,
  SharedWatchOptions
}

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
    sharedPublish: SharedPublishOptions = SharedPublishOptions(),
  @Recurse
    scalaSigning: PgpScalaSigningOptions = PgpScalaSigningOptions(),
) extends HasSharedOptions
// format: on

object PublishLocalOptions {
  implicit lazy val parser: Parser[PublishLocalOptions] = Parser.derive
  implicit lazy val help: Help[PublishLocalOptions]     = Help.derive
}
