package scala.cli.commands

import caseapp._

import scala.cli.ScalaCli
case class HelpOptions()

object HelpOptions {
  implicit lazy val parser: Parser[HelpOptions] = Parser.derive
  implicit lazy val help: Help[HelpOptions]     = Help.derive
}

object HelpCmd extends ScalaCommand[HelpOptions] {
  def run(options: HelpOptions, args: RemainingArgs) =
    println(ScalaCli.help.help(ScalaCli.helpFormat))
}
