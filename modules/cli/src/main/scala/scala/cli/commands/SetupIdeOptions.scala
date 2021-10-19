package scala.cli.commands

import caseapp._

import scala.build.options.BuildOptions

// format: off
@HelpMessage("Generate BSP file required for successful IDE import")
final case class SetupIdeOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Name("bspDir")
  @HelpMessage("Custom BSP configuration location")
  @Hidden
    bspDirectory: Option[String] = None,
  @Name("name")
  @HelpMessage("Name of BSP")
  @Hidden
    bspName: Option[String] = None,
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
