package scala.cli.commands.pgp

import caseapp.*

import scala.build.Logger
import scala.build.internals.ConsoleUtils.ScalaCliConsole.warnPrefix
import scala.cli.commands.shared.HelpGroup
import scala.cli.commands.tags

// format: off
final case class PgpScalaSigningOptions(
  @Group(HelpGroup.Signing.toString)
  @Tag(tags.restricted)
  @HelpMessage("Ignored legacy option. Deprecated, signing now always runs within Scala CLI.")
  @Hidden
    signingCliVersion: Option[String] = None,
  @Group(HelpGroup.Signing.toString)
  @Tag(tags.restricted)
  @HelpMessage("Ignored legacy option. Deprecated, signing now always runs within Scala CLI.")
  @ValueDescription("option")
  @Hidden
    signingCliJavaArg: List[String] = Nil,
  @Group(HelpGroup.Signing.toString)
  @HelpMessage("Ignored legacy option. Deprecated, signing now always runs within Scala CLI.")
  @Hidden
  @Tag(tags.restricted)
    forceSigningExternally: Option[Boolean] = None,
  @Group(HelpGroup.Signing.toString)
  @Tag(tags.restricted)
  @HelpMessage("Ignored legacy option. Deprecated, signing now always runs within Scala CLI.")
  @Hidden
    forceJvmSigningCli: Option[Boolean] = None
) { // format: on
  def warnAboutIgnoredOptions(logger: Logger): Unit = {
    val passedOptions = Seq(
      signingCliVersion.map(_ => "--signing-cli-version"),
      Option.when(signingCliJavaArg.nonEmpty)("--signing-cli-java-arg"),
      forceSigningExternally.map(_ => "--force-signing-externally"),
      forceJvmSigningCli.map(_ => "--force-jvm-signing-cli")
    ).flatten
    for (option <- passedOptions)
      logger.message(
        s"$warnPrefix Deprecated option '$option' is ignored, signing now always runs within Scala CLI."
      )
  }
}

object PgpScalaSigningOptions {
  implicit lazy val parser: Parser[PgpScalaSigningOptions] = Parser.derive
  implicit lazy val help: Help[PgpScalaSigningOptions]     = Help.derive
}
