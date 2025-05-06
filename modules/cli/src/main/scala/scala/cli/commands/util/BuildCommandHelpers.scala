package scala.cli.commands.util

import scala.build.errors.BuildException
import scala.build.{Build, Builds, Logger, Os}
import scala.cli.commands.ScalaCommand
import scala.cli.commands.shared.SharedOptions
import scala.cli.commands.util.ScalacOptionsUtil._

trait BuildCommandHelpers { self: ScalaCommand[?] =>
  extension (successfulBuild: Build.Successful) {
    def retainedMainClass(
      logger: Logger,
      mainClasses: Seq[String] = successfulBuild.foundMainClasses()
    ): Either[BuildException, String] =
      successfulBuild.retainedMainClass(
        mainClasses,
        self.argvOpt.map(_.mkString(" ")).getOrElse(actualFullCommand),
        logger
      )
  }

  extension (builds: Builds) {
    def anyBuildCancelled: Boolean = builds.all.exists {
      case _: Build.Cancelled => true
      case _                  => false
    }

    def anyBuildFailed: Boolean = builds.all.exists {
      case _: Build.Failed => true
      case _               => false
    }
  }
}

object BuildCommandHelpers {
  extension (successfulBuild: Build.Successful) {

    /** -O -d defaults to --compile-output; if both are defined, --compile-output takes precedence
      */
    def copyOutput(sharedOptions: SharedOptions): Unit =
      sharedOptions.compilationOutput.filter(_.nonEmpty)
        .orElse(sharedOptions.scalacOptions.getScalacOption("-d"))
        .filter(_.nonEmpty)
        .map(os.Path(_, Os.pwd)).foreach { output =>
          os.copy(
            successfulBuild.output,
            output,
            createFolders = true,
            mergeFolders = true,
            replaceExisting = true
          )
        }
  }
}
