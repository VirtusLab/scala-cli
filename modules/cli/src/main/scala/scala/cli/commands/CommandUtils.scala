package scala.cli.commands

import java.io.File
import java.nio.file.Paths

import scala.build.Os
import scala.cli.ScalaCli
import scala.cli.internal.ProcUtil
import scala.util.Try

object CommandUtils {

  def isOutOfDateVersion(newVersion: String, oldVersion: String): Boolean = {
    import coursier.core.Version

    Version(newVersion) > Version(oldVersion)
  }

  // Ensure the path to the CLI is absolute
  def getAbsolutePathToScalaCli(programName: String): String =
    if (programName.contains(File.separator))
      os.Path(programName, Os.pwd).toString
    else
      /*
  In order to get absolute path we first try to get it from coursier.mainJar (this works for standalone launcher)
  If this fails we fallback to getting it from this class and finally we may also use rawArg if there is nothing left
       */
      sys.props.get("coursier.mainJar")
        .map(Paths.get(_).toAbsolutePath.toString)
        .orElse {
          val scalaCliPathsOnPATH = ProcUtil.findApplicationPathsOnPATH(ScalaCli.progName)
          /*
            https://github.com/VirtusLab/scala-cli/issues/1048
            scalaCLICanonicalPathFromPATH is a map consisting of canonical Scala CLI paths for each symlink find on PATH.
            If the current launcher path is the same as the canonical Scala CLI path,
              we use a related symlink that targets to current launcher path.
           */
          val scalaCLICanonicalPathsFromPATH =
            scalaCliPathsOnPATH
              .map(path => (os.followLink(os.Path(path, os.pwd)), path))
              .collect {
                case (Some(canonicalPath), symlinkPath) => (canonicalPath, symlinkPath)
              }.toMap
          val currentLauncherPathOpt = Try(
            // This is weird but on windows we get /D:\a\scala-cli...
            Paths.get(getClass.getProtectionDomain.getCodeSource.getLocation.toURI)
              .toAbsolutePath
              .toString
          ).toOption
          currentLauncherPathOpt.map(currentLauncherPath =>
            scalaCLICanonicalPathsFromPATH.get(os.Path(currentLauncherPath))
              .getOrElse(currentLauncherPath)
          )
        }
        .getOrElse(programName)

  lazy val shouldCheckUpdate: Boolean = scala.util.Random.nextInt() % 10 == 1

  def printablePath(path: os.Path): String =
    if (path.startsWith(Os.pwd)) "." + File.separator + path.relativeTo(Os.pwd).toString
    else path.toString
}
