package scala.cli.commands

import caseapp._

import scala.build.options.BuildOptions

// format: off
final case class ExportOptions(
  // FIXME There might be too many options for 'scala-cli export' there
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    mainClass: MainClassOptions = MainClassOptions(),

  sbt: Option[Boolean] = None,
  mill: Option[Boolean] = None,

  @Name("setting")
    sbtSetting: List[String] = Nil,

  @Name("o")
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
