package scala.build

import scala.util.Properties

object Os {
  lazy val pwd: os.Path =
    if (Properties.isWin)
      os.Path(os.pwd.toIO.getCanonicalFile)
    else
      os.pwd
}
