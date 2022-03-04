package scala.cli.commands

import java.io.File
import java.nio.file.Paths

import scala.build.Os
import scala.util.Try

object CommandUtils {

  /** we only want to check for update within 10% random calls; not everytime
    */
  lazy val shouldCheckUpdate: Boolean =
    scala.util.Random.nextInt % 10 == 1

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
          Try(
            // This is weird but on windows we get /D:\a\scala-cli...
            Paths.get(getClass.getProtectionDomain.getCodeSource.getLocation.toURI)
              .toAbsolutePath
              .toString
          ).toOption
        }
        .getOrElse(programName)

}
