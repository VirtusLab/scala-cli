package scala.cli.commands.publish

import caseapp._

import scala.cli.commands.{CompileCrossOptions, MainClassOptions, SharedOptions, SharedWatchOptions}
import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.ArgParsers._

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
    sharedPublish: SharedPublishOptions = SharedPublishOptions(),

  @Group("Publishing")
  @HelpMessage("Directory where temporary files for publishing should be written")
  @Hidden
    workingDir: Option[String] = None,

  @Group("Publishing")
  @Hidden
  @HelpMessage("Scala version suffix to append to the module name, like \"_2.13\" or \"_3\"")
  @ValueDescription("suffix")
    scalaVersionSuffix: Option[String] = None,
  @Group("Publishing")
  @Hidden
  @HelpMessage("Scala platform suffix to append to the module name, like \"_sjs1\" or \"_native0.4\"")
  @ValueDescription("suffix")
    scalaPlatformSuffix: Option[String] = None,

  @Group("Publishing")
  @HelpMessage("Whether to build and publish source JARs")
    sources: Option[Boolean] = None,

  @Group("Publishing")
  @HelpMessage("Whether to build and publish doc JARs")
  @ExtraName("scaladoc")
  @ExtraName("javadoc")
    doc: Option[Boolean] = None,

  @Group("Publishing")
  @HelpMessage("ID of the GPG key to use to sign artifacts")
  @ValueDescription("key-id")
  @ExtraName("K")
    gpgKey: Option[String] = None,

  @Group("Publishing")
  @HelpMessage("Method to use to sign artifacts")
  @ValueDescription("gpg|bc")
    signer: Option[String] = None,

  @Group("Publishing")
  @HelpMessage("gpg command-line options")
  @ValueDescription("argument")
  @ExtraName("G")
  @ExtraName("gpgOpt")
    gpgOption: List[String] = Nil,

  @Group("Publishing")
  @Hidden
    ivy2LocalLike: Option[Boolean] = None
)
// format: on

object PublishOptions {
  implicit lazy val parser: Parser[PublishOptions] = Parser.derive
  implicit lazy val help: Help[PublishOptions]     = Help.derive
}
