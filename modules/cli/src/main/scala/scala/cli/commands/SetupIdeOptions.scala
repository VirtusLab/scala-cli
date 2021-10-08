package scala.cli.commands

import caseapp._

import scala.build.options.BuildOptions

// format: off
final case class SetupIdeOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Name("bspDir")
    bspDirectory: Option[String] = None,
  @Name("name")
    bspName: Option[String] = None,
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
