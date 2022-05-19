package scala.cli.commands
package util

import scala.build.options._

object VerbosityOptionsUtil {
  implicit class VerbosityOptionsOps(v: VerbosityOptions) {
    import v._

    def buildOptions: BuildOptions = BuildOptions(
      internal = InternalOptions(
        interactive = interactive
      )
    )
  }
}
