package scala.cli.commands.pgp

import caseapp._

import scala.build.internal.Constants
import scala.build.{options => bo}
import scala.cli.commands.shared.{CoursierOptions, HelpGroup, LoggingOptions, SharedJvmOptions}
import scala.cli.commands.tags

// format: off
final case class PgpScalaSigningOptions(
  @Group(HelpGroup.Signing.toString)
  @Tag(tags.restricted)
  @HelpMessage(s"scala-cli-signing version when running externally (${Constants.scalaCliSigningVersion} by default)")
  @Hidden
    signingCliVersion: Option[String] = None,
  @Group(HelpGroup.Signing.toString)
  @Tag(tags.restricted)
  @HelpMessage("Pass arguments to the Java command when running scala-cli-singing externally on JVM")
  @ValueDescription("option")
  @Hidden
    signingCliJavaArg: List[String] = Nil,
  @Group(HelpGroup.Signing.toString)
  @HelpMessage("When running Scala CLI on the JVM, force running scala-cli-singing externally")
  @Hidden
  @Tag(tags.restricted)
    forceSigningExternally: Option[Boolean] = None,
  @Group(HelpGroup.Signing.toString)
  @Tag(tags.restricted)
  @HelpMessage("When running Scala CLI on the JVM, force running scala-cli-singing using a native launcher or a JVM launcher")
  @Hidden
    forceJvmSigningCli: Option[Boolean] = None
) { // format: on
  def cliOptions(): bo.ScalaSigningCliOptions =
    bo.ScalaSigningCliOptions(
      javaArgs = signingCliJavaArg,
      forceExternal = forceSigningExternally,
      forceJvm = forceJvmSigningCli,
      signingCliVersion = signingCliVersion
    )
}

object PgpScalaSigningOptions {
  implicit lazy val parser: Parser[PgpScalaSigningOptions] = Parser.derive
  implicit lazy val help: Help[PgpScalaSigningOptions]     = Help.derive
}
