package scala.cli.commands

import caseapp._
import coursier.cache.loggers.RefreshLogger
import coursier.cache.CacheLogger
import scala.cli.bloop.bloopgun
import scala.cli.Logger

final case class LoggingOptions(
  @Name("v")
    verbose: Int @@ Counter = Tag.of(0),
  @Name("q")
    quiet: Boolean = false
) {

  lazy val verbosity = Tag.unwrap(verbose) - (if (quiet) 1 else 0)

  lazy val logger: Logger =
    new Logger { logger =>
      def log(message: => String) =
        if (verbosity >= 1)
          System.err.println(message)
      def log(message: => String, debugMessage: => String) =
        if (verbosity >= 2)
          System.err.println(debugMessage)
        else if (verbosity >= 1)
          System.err.println(message)
      def debug(message: => String) =
        if (verbosity >= 2)
          System.err.println(message)

      def withCoursierLogger[T](f: CacheLogger => T) = {
        val logger = RefreshLogger.create()
        logger.use(f(logger))
      }
      def coursierInterfaceLogger =
        coursierapi.Logger.progressBars()

      def bloopgunLogger =
        new bloopgun.BloopgunLogger {
          def debug(msg: => String) =
            if (verbosity >= 3)
              logger.debug(msg)
          def error(msg: => String, ex: Throwable) =
            logger.log(s"Error: $msg ($ex)")
          def coursierInterfaceLogger =
            logger.coursierInterfaceLogger
          def bloopBspStdout =
            if (verbosity >= 2) Some(System.err)
            else None
          def bloopBspStderr =
            if (verbosity >= 2) Some(System.err)
            else None
        }
    }

}
