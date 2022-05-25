package scala.cli.commands
package util

object VerbosityOptionsUtil {
  implicit class VerbosityOptionsOps(v: VerbosityOptions) {
    import v._

    val Interactive = if (interactive.getOrElse(false))
      scala.build.interactive.Interactive.InteractiveAsk
    else scala.build.interactive.Interactive.InteractiveNop
  }
}
