package scala.build

import java.util.Locale

import scala.util.Properties

object Os {
  lazy val pwd: os.Path =
    if (Properties.isWin)
      os.Path(os.pwd.toIO.getCanonicalFile)
    else
      os.pwd
  lazy val isArmArchitecture: Boolean =
    sys.props.getOrElse("os.arch", "").toLowerCase(Locale.ROOT) == "aarch64"
}
