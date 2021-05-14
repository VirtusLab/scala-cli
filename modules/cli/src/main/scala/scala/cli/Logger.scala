package scala.cli

trait Logger {
  // TODO Use macros for log and debug calls to have zero cost when verbosity <= 0
  def log(s: => String): Unit
  def log(s: => String, debug: => String): Unit
  def debug(s: => String): Unit
}
