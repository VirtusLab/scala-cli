package scala.build.testrunner

import java.io.PrintStream

class Logger(val verbosity: Int, out: PrintStream) {
  def error(message: String): Unit = out.println(message)

  def message(message: => String): Unit = if (verbosity >= 0) out.println(message)

  def log(message: => String): Unit = if (verbosity >= 1) out.println(message)
  def log(message: => String, debugMessage: => String): Unit =
    if (verbosity >= 2) out.println(debugMessage)
    else if (verbosity >= 1) out.println(message)

  def debug(message: => String): Unit = if (verbosity >= 2) out.println(message)
}

object Logger {
  def apply(verbosity: Int, out: PrintStream) = new Logger(verbosity, out)
  def apply(verbosity: Int)                   = new Logger(verbosity, out = System.err)
}
