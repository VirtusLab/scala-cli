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
