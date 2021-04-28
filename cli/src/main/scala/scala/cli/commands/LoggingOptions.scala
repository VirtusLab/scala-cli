package scala.cli.commands

import caseapp._

import scala.cli.Logger

final case class LoggingOptions(
  @Name("v")
    verbose: Int @@ Counter = Tag.of(0),
  @Name("q")
    quiet: Boolean = false
) {

  lazy val verbosity = Tag.unwrap(verbose) - (if (quiet) 1 else 0)

  lazy val logger: Logger =
    new Logger {
      def log(message: => String): Unit =
        if (verbosity >= 1)
          System.err.println(message)
      def log(message: => String, debugMessage: => String): Unit =
        if (verbosity >= 2)
          System.err.println(debugMessage)
        else if (verbosity >= 1)
          System.err.println(message)
      def debug(message: => String): Unit =
        if (verbosity >= 2)
          System.err.println(message)
    }

}
