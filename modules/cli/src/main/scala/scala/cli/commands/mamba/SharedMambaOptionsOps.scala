package scala.cli.commands.mamba

import scala.build.internal.Constants

object SharedMambaOptionsOps {
  implicit class SharedMambaOptionsOps0(private val opts: SharedMambaOptions) extends AnyVal {
    def microMambaVersion = Constants.defaultMicroMambaVersion
    def microMambaSuffix  = Constants.defaultMicroMambaSuffix
    def condaPlatform     = scala.build.internal.Mamba.localPlatform
  }
}
