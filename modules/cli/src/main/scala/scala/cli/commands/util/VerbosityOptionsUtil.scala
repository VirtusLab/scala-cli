package scala.cli.commands
package util

import scala.build.interactive.Interactive._
import scala.cli.commands.shared.VerbosityOptions

object VerbosityOptionsUtil {
  implicit class VerbosityOptionsOps(v: VerbosityOptions) {
    def interactiveInstance(forceEnable: Boolean = false) =
      if (v.interactive.getOrElse(forceEnable)) InteractiveAsk else InteractiveNop
  }
}
