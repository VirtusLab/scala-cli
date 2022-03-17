package scala.cli.commands

import caseapp._
import caseapp.core.help.Help
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

import scala.cli.ScalaCliHelp

@HelpMessage("Print help message")
case class HelpGroupOptions(
  @HelpMessage("Show options for ScalaJS")
  helpJs: Boolean = false,
  @HelpMessage("Show options for ScalaNative")
  helpNative: Boolean = false
) {

  private def printHelpWithGroup(help: Help[_], group: String): Nothing = {
    println(help.help(ScalaCliHelp.helpFormat.withHiddenGroups(
      ScalaCliHelp.helpFormat.hiddenGroups.map(_.filterNot(_ == group))
    )))
    sys.exit(0)
  }

  def maybePrintGroupHelp(help: Help[_]): Unit = {
    if (helpJs) printHelpWithGroup(help, "Scala.JS")
    else if (helpNative) printHelpWithGroup(help, "Scala Native")
  }
}

object HelpGroupOptions {
  lazy val parser: Parser[HelpGroupOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[HelpGroupOptions, parser.D] = parser
  implicit lazy val help: Help[HelpGroupOptions]                      = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[HelpGroupOptions]       = JsonCodecMaker.make
}
