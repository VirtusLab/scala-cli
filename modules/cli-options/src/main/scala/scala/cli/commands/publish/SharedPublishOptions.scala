package scala.cli.commands.publish

import caseapp.*

// format: off
final case class SharedPublishOptions(

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
  @ValueDescription("gpg|bc|none")
    signer: Option[String] = None,

  @Group("Publishing")
  @HelpMessage("gpg command-line options")
  @ValueDescription("argument")
  @ExtraName("G")
  @ExtraName("gpgOpt")
    gpgOption: List[String] = Nil,

  @Group("Publishing")
  @HelpMessage("Set Ivy 2 home directory")
  @ValueDescription("path")
    ivy2Home: Option[String] = None,

  @Group("Publishing")
  @Hidden
    forceSigningBinary: Boolean = false,

  @Group("Publishing")
  @Hidden
    checksum: List[String] = Nil,

  @Group("Publishing")
  @HelpMessage("Proceed as if publishing, but do not upload / write artifacts to the remote repository")
    dummy: Boolean = false
)
// format: on

object SharedPublishOptions {
  lazy val parser: Parser[SharedPublishOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[SharedPublishOptions, parser.D] = parser
  implicit lazy val help: Help[SharedPublishOptions]                      = Help.derive
}
