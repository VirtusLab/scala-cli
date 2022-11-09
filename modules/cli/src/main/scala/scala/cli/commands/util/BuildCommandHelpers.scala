package scala.cli.commands.util

import scala.build.errors.MainClassError
import scala.build.{Build, Logger, Os}
import scala.cli.commands.util.ScalacOptionsUtil.*
import scala.cli.commands.{ScalaCommand, SharedOptions}

trait BuildCommandHelpers { self: ScalaCommand[_] =>
  extension (successfulBuild: Build.Successful) {
    def retainedMainClass(
      logger: Logger,
      mainClasses: Seq[String] = successfulBuild.foundMainClasses()
    ): Either[MainClassError, String] =
      successfulBuild.retainedMainClass(
        mainClasses,
        self.argvOpt.map(_.mkString(" ")).getOrElse(actualFullCommand),
        logger
      )

    /** -O -d defaults to --compile-output; if both are defined, --compile-output takes precedence
      */
    def copyOutput(sharedOptions: SharedOptions): Unit =
      sharedOptions.compilationOutput.filter(_.nonEmpty)
        .orElse(sharedOptions.scalac.scalacOption.getScalacOption("-d"))
        .filter(_.nonEmpty)
        .map(os.Path(_, Os.pwd)).foreach(output =>
          os.copy.over(successfulBuild.output, output, createFolders = true)
        )
  }
}
