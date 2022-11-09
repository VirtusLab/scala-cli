package scala.cli.commands

import caseapp.*

import scala.cli.commands.common.HasSharedOptions

// format: off
@HelpMessage("Export current project to sbt or Mill")
final case class ExportOptions(
  // FIXME There might be too many options for 'scala-cli export' there
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    mainClass: MainClassOptions = MainClassOptions(),
  
  @Group("Build Tool export options")
  sbt: Option[Boolean] = None,
  @Group("Build Tool export options")
  mill: Option[Boolean] = None,

  @Name("setting")
  @Group("Build Tool export options")
    sbtSetting: List[String] = Nil,
  @Name("p")
  @Group("Build Tool export options")
  @HelpMessage("Project name to be used on Mill build file")
  project: Option[String] = None,
  @Group("Build Tool export options")
  sbtVersion: Option[String] = None,
  @Name("o")
  @Group("Build Tool export options")
    output: Option[String] = None
) extends HasSharedOptions
// format: on
object ExportOptions {
  implicit lazy val parser: Parser[ExportOptions] = Parser.derive
  implicit lazy val help: Help[ExportOptions]     = Help.derive
}
