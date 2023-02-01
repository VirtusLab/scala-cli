package scala.cli.commands.setupide

import caseapp.*

import scala.cli.ScalaCli.{baseRunnerName, fullRunnerName}
import scala.cli.commands.shared.{
  HasSharedOptions,
  HelpMessages,
  SharedBspFileOptions,
  SharedOptions
}
import scala.cli.commands.tags

// format: off
@HelpMessage(
  s"""Generates a BSP file that you can import into your IDE.
     |
     |The `setup-ide` sub-command allows to pre-configure a $fullRunnerName project to import to an IDE with BSP support.
     |It is also ran implicitly when `compile`, `run`, `shebang` or `test` sub-commands are called.
     |
     |The pre-configuration should be saved in a BSP json connection file under the path:
     |    {project-root}/.bsp/$baseRunnerName.json
     |
     |${HelpMessages.commandConfigurations("setup-ide")}
     |
     |${HelpMessages.docsWebsiteReference}""".stripMargin)
final case class SetupIdeOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    bspFile: SharedBspFileOptions = SharedBspFileOptions(),
  @Hidden
  @Tag(tags.implementation)
  charset: Option[String] = None
) extends HasSharedOptions
// format: on

object SetupIdeOptions {
  implicit lazy val parser: Parser[SetupIdeOptions] = Parser.derive
  implicit lazy val help: Help[SetupIdeOptions]     = Help.derive
}
