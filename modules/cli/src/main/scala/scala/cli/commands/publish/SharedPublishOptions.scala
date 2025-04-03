package scala.cli.commands.publish

import caseapp.*

import scala.build.compiler.{ScalaCompilerMaker, SimpleScalaCompilerMaker}
import scala.cli.commands.shared.{HelpGroup, ScopeOptions}
import scala.cli.commands.tags

// format: off
final case class SharedPublishOptions(

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Directory where temporary files for publishing should be written")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
  @Hidden
    workingDir: Option[String] = None,

  @Group(HelpGroup.Publishing.toString)
  @Hidden
  @HelpMessage("Scala version suffix to append to the module name, like \"_2.13\" or \"_3\"")
  @ValueDescription("suffix")
  @Tag(tags.restricted)
    scalaVersionSuffix: Option[String] = None,
  @Group(HelpGroup.Publishing.toString)
  @Hidden
  @HelpMessage("Scala platform suffix to append to the module name, like \"_sjs1\" or \"_native0.4\"")
  @ValueDescription("suffix")
  @Tag(tags.restricted)
    scalaPlatformSuffix: Option[String] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Whether to build and publish source JARs")
  @Name("sourcesJar")
  @Name("jarSources")
  @Name("sources")
  @Tag(tags.deprecated("sources"))
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    withSources: Option[Boolean] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Whether to build and publish doc JARs")
  @ExtraName("scaladoc")
  @ExtraName("javadoc")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    doc: Option[Boolean] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("ID of the GPG key to use to sign artifacts")
  @ValueDescription("key-id")
  @ExtraName("K")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    gpgKey: Option[String] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Method to use to sign artifacts")
  @ValueDescription("gpg|bc|none")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    signer: Option[String] = None,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("gpg command-line options")
  @ValueDescription("argument")
  @ExtraName("G")
  @ExtraName("gpgOpt")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    gpgOption: List[String] = Nil,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Set Ivy 2 home directory")
  @ValueDescription("path")
  @Tag(tags.restricted)
    ivy2Home: Option[String] = None,

  @Group(HelpGroup.Publishing.toString)
  @Hidden
  @Tag(tags.restricted)
    checksum: List[String] = Nil,

  @Group(HelpGroup.Publishing.toString)
  @HelpMessage("Proceed as if publishing, but do not upload / write artifacts to the remote repository")
  @Tag(tags.implementation)
    dummy: Boolean = false
){
  // format: on

  def docCompilerMakerOpt: Option[ScalaCompilerMaker] =
    if (doc.contains(false)) // true by default
      None
    else
      Some(SimpleScalaCompilerMaker("java", Nil, scaladoc = true))
}

object SharedPublishOptions {
  implicit lazy val parser: Parser[SharedPublishOptions] = Parser.derive
  implicit lazy val help: Help[SharedPublishOptions]     = Help.derive
}
