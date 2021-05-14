package scala.cli.tests

import scala.cli.Logger

case class TestLogger(info: Boolean = true, debug: Boolean = false) extends Logger {
  def log(s: => String): Unit =
    if (info)
      System.err.println(s)
  def log(s: => String, debug: => String): Unit =
    if (this.debug)
      System.err.println(debug)
    else if (info)
      System.err.println(s)
  def debug(s: => String): Unit =
    if (debug)
      System.err.println(s)
}
