package scala.cli.commands

import scala.cli.ScalaCli

// FIXME scalafmt formats that really weirdly, need to investigate why
// format: off
object Default extends DefaultBase(
  ScalaCli.help.help(ScalaCli.helpFormat),
  ScalaCli.help.help(ScalaCli.helpFormat, showHidden = true)
)
// format: on
