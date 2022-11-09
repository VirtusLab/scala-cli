package scala.cli.commands.shared

import caseapp.*
import caseapp.core.Scala3Helpers.*
import caseapp.core.help.{Help, HelpFormat}
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.cli.commands.tags

@HelpMessage("Print help message")
case class HelpGroupOptions(
  @HelpMessage("Show options for ScalaJS")
  @Tag(tags.implementation)
  helpJs: Boolean = false,
  @HelpMessage("Show options for ScalaNative")
  @Tag(tags.implementation)
  helpNative: Boolean = false,
  @HelpMessage("Show options for Scaladoc")
  @Name("scaladocHelp")
  @Name("docHelp")
  @Name("helpDoc")
  @Tag(tags.implementation)
  helpScaladoc: Boolean = false,
  @HelpMessage("Show options for Scala REPL")
  @Name("replHelp")
  @Tag(tags.implementation)
  helpRepl: Boolean = false,
  @HelpMessage("Show options for Scalafmt")
  @Name("scalafmtHelp")
  @Name("fmtHelp")
  @Name("helpFmt")
  @Tag(tags.implementation)
  helpScalafmt: Boolean = false
) {

  private def printHelpWithGroup(help: Help[_], helpFormat: HelpFormat, group: String): Nothing = {
    println(help.help(helpFormat.withHiddenGroups(
      helpFormat.hiddenGroups.map(_.filterNot(_ == group))
    )))
    sys.exit(0)
  }

  def maybePrintGroupHelp(help: Help[_], helpFormat: HelpFormat): Unit = {
    if (helpJs) printHelpWithGroup(help, helpFormat, "Scala.js")
    else if (helpNative) printHelpWithGroup(help, helpFormat, "Scala Native")
  }
}

object HelpGroupOptions {
  implicit lazy val parser: Parser[HelpGroupOptions]            = Parser.derive
  implicit lazy val help: Help[HelpGroupOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[HelpGroupOptions] = JsonCodecMaker.make
}
