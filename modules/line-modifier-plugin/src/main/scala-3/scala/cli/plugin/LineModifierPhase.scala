package scala.cli.plugin

import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.plugins.PluginPhase

class LineModifierPhase extends PluginPhase {
  import tpd.*

  val phaseName = "LineModifier"

  override val runsAfter = Set("parser")
  override val runsBefore = Set("namer")
}
