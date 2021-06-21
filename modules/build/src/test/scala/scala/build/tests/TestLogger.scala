package scala.build.tests

import coursier.cache.CacheLogger
import coursier.cache.loggers.{FallbackRefreshDisplay, RefreshLogger}

import scala.build.blooprifle.BloopRifleLogger
import scala.build.Logger

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

  def coursierLogger: CacheLogger =
    RefreshLogger.create(new FallbackRefreshDisplay)

  def bloopRifleLogger: BloopRifleLogger =
    BloopRifleLogger.nop
  def bloopBspStderr = None
  def bloopBspStdout = None
}
