package scala.cli.commands
package util

import scala.build.interactive.Interactive._

object VerbosityOptionsUtil {
  implicit class VerbosityOptionsOps(v: VerbosityOptions) {
    val interactiveInstance = if (v.interactive.getOrElse(false)) InteractiveAsk else InteractiveNop
  }
}
