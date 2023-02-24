package scala.cli.commands.shared

import caseapp.*
import caseapp.core.Scala3Helpers.*
import caseapp.core.help.{Help, HelpFormat}
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.cli.commands.tags

@HelpMessage("Print help message")
case class HelpGroupOptions(
  @Group(HelpGroup.Help.toString)
  @HelpMessage("Show options for ScalaJS")
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
  helpJs: Boolean = false,
  @Group(HelpGroup.Help.toString)
  @HelpMessage("Show options for ScalaNative")
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
  helpNative: Boolean = false,
  @Group(HelpGroup.Help.toString)
  @HelpMessage("Show options for Scaladoc")
  @Name("scaladocHelp")
  @Name("docHelp")
  @Name("helpDoc")
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
  helpScaladoc: Boolean = false,
  @Group(HelpGroup.Help.toString)
  @HelpMessage("Show options for Scala REPL")
  @Name("replHelp")
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
  helpRepl: Boolean = false,
  @Group(HelpGroup.Help.toString)
  @HelpMessage("Show options for Scalafmt")
  @Name("scalafmtHelp")
  @Name("fmtHelp")
  @Name("helpFmt")
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
  helpScalafmt: Boolean = false
) {

  private def printHelpWithGroup(help: Help[_], helpFormat: HelpFormat, group: String): Nothing = {
    val oldHiddenGroups = helpFormat.hiddenGroups.toSeq.flatten
    val oldSortedGroups = helpFormat.sortedGroups.toSeq.flatten
    val newHiddenGroups = (oldHiddenGroups ++ oldSortedGroups).filterNot(_ == group)
    println(
      help.help(
        helpFormat.withHiddenGroupsWhenShowHidden(Some(newHiddenGroups)),
        showHidden = true
      )
    )
    sys.exit(0)
  }

  def maybePrintGroupHelp(help: Help[_], helpFormat: HelpFormat): Unit = {
    if (helpJs) printHelpWithGroup(help, helpFormat, HelpGroup.ScalaJs.toString)
    else if (helpNative) printHelpWithGroup(help, helpFormat, HelpGroup.ScalaNative.toString)
  }
}

object HelpGroupOptions {
  implicit lazy val parser: Parser[HelpGroupOptions]            = Parser.derive
  implicit lazy val help: Help[HelpGroupOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[HelpGroupOptions] = JsonCodecMaker.make
}
