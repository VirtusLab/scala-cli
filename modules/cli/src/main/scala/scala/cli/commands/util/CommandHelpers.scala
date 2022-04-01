package scala.cli.commands.util

import scala.build.Logger
import scala.build.errors.BuildException

trait CommandHelpers {

  implicit class EitherBuildExceptionOps[E <: BuildException, T](private val either: Either[E, T]) {
    def orReport(logger: Logger): Option[T] =
      either match {
        case Left(ex) =>
          logger.log(ex)
          None
        case Right(t) => Some(t)
      }
    def orExit(logger: Logger): T =
      either match {
        case Left(ex) => logger.exit(ex)
        case Right(t) => t
      }
  }
}
