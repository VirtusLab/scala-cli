package scala.cli.commands
package util

import scala.build.interactive.Interactive._

object VerbosityOptionsUtil {
  implicit class VerbosityOptionsOps(v: VerbosityOptions) {
    def interactiveInstance(forceEnable: Boolean = false) =
      if (v.interactive.getOrElse(false) || forceEnable) InteractiveAsk else InteractiveNop
  }
}
