package scala.cli.commands.util

import scala.build.errors.BuildException
import scala.build.postprocessing.SlothPatcher
import scala.build.{Build, Builds, CrossBuildParams, Logger, Os}
import scala.cli.commands.ScalaCommand
import scala.cli.commands.shared.SharedOptions
import scala.cli.commands.util.ScalacOptionsUtil.*

trait BuildCommandHelpers { self: ScalaCommand[?] =>
  extension (b: Seq[Build.Successful]) {
    def groupedByCrossParams: Map[CrossBuildParams, Seq[Build.Successful]] =
      b.groupBy(bb => CrossBuildParams(bb.scalaParams, bb.options))
  }
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

    /** -O -d defaults to --compile-output; if both are defined, --compile-output takes precedence.
      * When `--sloth` is enabled, the destination is patched unless the source build output was
      * already patched in this process (e.g. by `compile`), so the same bytes are not scanned
      * twice.
      */
    def copyOutput(sharedOptions: SharedOptions, logger: Logger): Unit =
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
          if SlothPatcher.wasPatchedInThisProcess(successfulBuild.output) then
            logger.debug(
              s"Skipping Sloth patch of $output: source ${successfulBuild.output} already patched"
            )
          else
            for
              ex <- SlothPatcher.patchClassDirInPlace(
                output,
                successfulBuild.options,
                logger,
                shouldPatch = SlothPatcher.shouldPatchProjectClasses(Seq(successfulBuild))
              ).left
            do logger.exit(ex)
        }
  }
}
