package scala.cli.commands.pgp

import caseapp.*

import scala.build.options as bo
import scala.cli.commands.shared.{CoursierOptions, HelpGroup, LoggingOptions, SharedJvmOptions}
import scala.cli.commands.tags

// format: off
final case class PgpScalaSigningOptions(
  @Group(HelpGroup.Signing.toString)
  @Tag(tags.restricted)
  @Hidden
    signingCliVersion: Option[String] = None,
  @Group(HelpGroup.Signing.toString)
  @Tag(tags.restricted)
  @ValueDescription("option")
  @Hidden
    signingCliJavaArg: List[String] = Nil,
  @Group(HelpGroup.Signing.toString)
  @Tag(tags.restricted)
  @HelpMessage("Whether to run the Scala Signing CLI on the JVM or using a native executable")
  @Hidden
    forceJvmSigningCli: Option[Boolean] = None
) { // format: on
  def cliOptions(): bo.ScalaSigningCliOptions =
    bo.ScalaSigningCliOptions(
      javaArgs = signingCliJavaArg,
      useJvm = forceJvmSigningCli,
      signingCliVersion = signingCliVersion
    )
}

object PgpScalaSigningOptions {
  implicit lazy val parser: Parser[PgpScalaSigningOptions] = Parser.derive
  implicit lazy val help: Help[PgpScalaSigningOptions]     = Help.derive
}
