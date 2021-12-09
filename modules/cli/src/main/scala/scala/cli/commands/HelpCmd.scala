package scala.cli.commands

import caseapp._

import scala.cli.ScalaCli

object HelpCmd extends ScalaCommand[HelpOptions] {
  override def names = List(List("help"))

  def run(options: HelpOptions, args: RemainingArgs) =
    println(ScalaCli.help.help(ScalaCli.helpFormat))
}
