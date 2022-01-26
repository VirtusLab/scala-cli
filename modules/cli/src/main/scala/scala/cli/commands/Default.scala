package scala.cli.commands

import caseapp.core.help.RuntimeCommandsHelp

import scala.cli.ScalaCliHelp

// FIXME scalafmt formats that really weirdly, need to investigate why
// format: off
class Default(help: => RuntimeCommandsHelp) extends DefaultBase(
  help.help(ScalaCliHelp.helpFormat),
  help.help(ScalaCliHelp.helpFormat, showHidden = true)
)
// format: on
