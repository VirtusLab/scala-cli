package scala.cli.commands

import caseapp._

import scala.build.options.BuildOptions

// format: off
@HelpMessage("Generate a BSP file that you can import into your IDE")
final case class SetupIdeOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    bspFile: SharedBspFileOptions = SharedBspFileOptions(),
  @Hidden
  charset: Option[String] = None
) {
  // format: on
  def buildOptions: BuildOptions =
    shared.buildOptions(enableJmh = false, jmhVersion = None)

}

object SetupIdeOptions {
  implicit lazy val parser: Parser[SetupIdeOptions] = Parser.derive
  implicit lazy val help: Help[SetupIdeOptions]     = Help.derive
}
