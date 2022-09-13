package scala.cli.commands.util

import scala.build.errors.MainClassError
import scala.build.{Build, Logger}
import scala.cli.commands.ScalaCommand

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
  }
}
