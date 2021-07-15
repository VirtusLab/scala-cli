package scala.cli.commands

import scala.cli.ScalaCli

object Default extends DefaultBase(
  ScalaCli.help.help(ScalaCli.helpFormat),
  ScalaCli.help.help(ScalaCli.helpFormat, showHidden = true)
)
