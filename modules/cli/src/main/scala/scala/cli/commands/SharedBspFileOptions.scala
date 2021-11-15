package scala.cli.commands

import caseapp._

import scala.build.Os

// format: off
final case class SharedBspFileOptions(
  @Name("bspDir")
  @HelpMessage("Custom BSP configuration location")
  @Hidden
    bspDirectory: Option[String] = None,
  @Name("name")
  @HelpMessage("Name of BSP")
  @Hidden
    bspName: Option[String] = None
) {
  // format: on

  def bspDetails(workspace: os.Path): (String, os.Path) = {
    val dir = bspDirectory
      .filter(_.nonEmpty)
      .map(os.Path(_, Os.pwd))
      .getOrElse(workspace / ".bsp")
    val bspName0 = bspName.map(_.trim).filter(_.nonEmpty).getOrElse("scala-cli")

    (bspName0, dir / s"$bspName0.json")
  }
}

object SharedBspFileOptions {
  lazy val parser: Parser[SharedBspFileOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[SharedBspFileOptions, parser.D] = parser
  implicit lazy val help: Help[SharedBspFileOptions]                      = Help.derive
}
