package scala.cli.commands

object CommandUtils {

  def isOutOfDateVersion(newVersion: String, oldVersion: String): Boolean = {
    import coursier.core.Version

    Version(newVersion) > Version(oldVersion)
  }
}
