package scala.cli.commands.publish

import caseapp.*

import scala.cli.commands.tags

// format: off
final case class SharedPublishOptions(

  @Group("Publishing")
  @HelpMessage("Directory where temporary files for publishing should be written")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
  @Hidden
    workingDir: Option[String] = None,

  @Group("Publishing")
  @Hidden
  @HelpMessage("Scala version suffix to append to the module name, like \"_2.13\" or \"_3\"")
  @ValueDescription("suffix")
  @Tag(tags.restricted)
    scalaVersionSuffix: Option[String] = None,
  @Group("Publishing")
  @Hidden
  @HelpMessage("Scala platform suffix to append to the module name, like \"_sjs1\" or \"_native0.4\"")
  @ValueDescription("suffix")
  @Tag(tags.restricted)
    scalaPlatformSuffix: Option[String] = None,

  @Group("Publishing")
  @HelpMessage("Whether to build and publish source JARs")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    sources: Option[Boolean] = None,

  @Group("Publishing")
  @HelpMessage("Whether to build and publish doc JARs")
  @ExtraName("scaladoc")
  @ExtraName("javadoc")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    doc: Option[Boolean] = None,

  @Group("Publishing")
  @HelpMessage("ID of the GPG key to use to sign artifacts")
  @ValueDescription("key-id")
  @ExtraName("K")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    gpgKey: Option[String] = None,

  @Group("Publishing")
  @HelpMessage("Method to use to sign artifacts")
  @ValueDescription("gpg|bc|none")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    signer: Option[String] = None,

  @Group("Publishing")
  @HelpMessage("gpg command-line options")
  @ValueDescription("argument")
  @ExtraName("G")
  @ExtraName("gpgOpt")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    gpgOption: List[String] = Nil,

  @Group("Publishing")
  @HelpMessage("Set Ivy 2 home directory")
  @ValueDescription("path")
  @Tag(tags.restricted)
    ivy2Home: Option[String] = None,

  @Group("Publishing")
  @Hidden
  @Tag(tags.restricted)
    forceSigningBinary: Boolean = false,

  @Group("Publishing")
  @Hidden
  @Tag(tags.restricted)
    checksum: List[String] = Nil,

  @Group("Publishing")
  @HelpMessage("Proceed as if publishing, but do not upload / write artifacts to the remote repository")
  @Tag(tags.implementation)
    dummy: Boolean = false
)
// format: on

object SharedPublishOptions {
  implicit lazy val parser: Parser[SharedPublishOptions] = Parser.derive
  implicit lazy val help: Help[SharedPublishOptions]     = Help.derive
}
