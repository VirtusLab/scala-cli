package scala.cli.commands

import caseapp._

import scala.build.options.BuildOptions

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

  @Name("o")
  @Group("Build Tool export options")
    output: Option[String] = None
) {
  // format: on

  def buildOptions: BuildOptions = {
    val baseOptions = shared.buildOptions(enableJmh = false, None, ignoreErrors = false)
    baseOptions.copy(
      mainClass = mainClass.mainClass.filter(_.nonEmpty)
    )
  }
}

object ExportOptions {
  implicit lazy val parser: Parser[ExportOptions] = Parser.derive
  implicit lazy val help: Help[ExportOptions]     = Help.derive
}
