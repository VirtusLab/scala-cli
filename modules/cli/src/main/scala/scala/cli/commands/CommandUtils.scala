package scala.cli.commands

import scala.build.internal.Constants

object CommandUtils {

  def isOutOfDateVersion(newVersion: String, oldVersion: String): Boolean = {
    import coursier.core.Version

    Version(newVersion) > Version(oldVersion)
  }

  lazy val shouldCheckUpdate: Boolean = scala.util.Random.nextInt % 10 == 1
}
