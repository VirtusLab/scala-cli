package scala.cli.plugin

import dotty.tools.dotc.plugins.{PluginPhase, StandardPlugin}

class LineModifierPlugin extends StandardPlugin {
  val name = "linemodifier"
  val description = ""

  def init(options: List[String]): List[PluginPhase] =
    (new LineModifierPhase) :: Nil
}
