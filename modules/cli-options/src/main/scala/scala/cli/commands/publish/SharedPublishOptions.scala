package scala.cli.commands.publish

import scala.cli.commands.tags

import caseapp.*

// format: off
final case class SharedPublishOptions(

  @Group("Publishing")
  @HelpMessage("Directory where temporary files for publishing should be written")
  @Tag(tags.experimental)
  @Hidden
    workingDir: Option[String] = None,

  @Group("Publishing")
  @Hidden
  @Tag(tags.experimental)
  @HelpMessage("Scala version suffix to append to the module name, like \"_2.13\" or \"_3\"")
  @ValueDescription("suffix")
    scalaVersionSuffix: Option[String] = None,
  @Group("Publishing")
  @Hidden
  @Tag(tags.experimental)
  @HelpMessage("Scala platform suffix to append to the module name, like \"_sjs1\" or \"_native0.4\"")
  @ValueDescription("suffix")
    scalaPlatformSuffix: Option[String] = None,

  @Group("Publishing")
  @Tag(tags.experimental)
  @HelpMessage("Whether to build and publish source JARs")
    sources: Option[Boolean] = None,

  @Group("Publishing")
  @HelpMessage("Whether to build and publish doc JARs")
  @ExtraName("scaladoc")
  @ExtraName("javadoc")
  @Tag(tags.experimental)
    doc: Option[Boolean] = None,

  @Group("Publishing")
  @HelpMessage("ID of the GPG key to use to sign artifacts")
  @ValueDescription("key-id")
  @ExtraName("K")
  @Tag(tags.experimental)
    gpgKey: Option[String] = None,

  @Group("Publishing")
  @HelpMessage("Method to use to sign artifacts")
  @ValueDescription("gpg|bc|none")
  @Tag(tags.experimental)
    signer: Option[String] = None,

  @Group("Publishing")
  @HelpMessage("gpg command-line options")
  @ValueDescription("argument")
  @ExtraName("G")
  @ExtraName("gpgOpt")
  @Tag(tags.experimental)
    gpgOption: List[String] = Nil,

  @Group("Publishing")
  @HelpMessage("Set Ivy 2 home directory")
  @ValueDescription("path")
  @Tag(tags.experimental)
  @Hidden
    ivy2Home: Option[String] = None,

  @Group("Publishing")
  @Hidden
  @Tag(tags.experimental)
    forceSigningBinary: Boolean = false,

  @Group("Publishing")
  @Hidden
  @Tag(tags.experimental)
    checksum: List[String] = Nil,

  @Group("Publishing")
  @HelpMessage("Proceed as if publishing, but do not upload / write artifacts to the remote repository")
  @ExtraName("dryRun")
  @Tag(tags.experimental)
    dummy: Boolean = false
)
// format: on

object SharedPublishOptions {
  lazy val parser: Parser[SharedPublishOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[SharedPublishOptions, parser.D] = parser
  implicit lazy val help: Help[SharedPublishOptions]                      = Help.derive
}
