package scala.cli.commands

import scala.cli.ScalaCliCore

object DefaultCore extends DefaultBase(
  ScalaCliCore.help.help(ScalaCliCore.helpFormat),
  ScalaCliCore.help.help(ScalaCliCore.helpFormat, showHidden = true)
)
