package scala.build

import coursier.cache.CacheLogger
import scala.build.bloop.bloopgun

trait Logger {
  // TODO Use macros for log and debug calls to have zero cost when verbosity <= 0
  def log(s: => String): Unit
  def log(s: => String, debug: => String): Unit
  def debug(s: => String): Unit

  def coursierLogger: coursier.cache.CacheLogger

  def bloopgunLogger: bloopgun.BloopgunLogger
}

object Logger {
  private class Nop extends Logger {
    def log(s: => String): Unit = ()
    def log(s: => String, debug: => String): Unit = ()
    def debug(s: => String): Unit = ()

    def coursierLogger: coursier.cache.CacheLogger =
      coursier.cache.CacheLogger.nop

    def bloopgunLogger: bloopgun.BloopgunLogger =
      bloopgun.BloopgunLogger.nop
  }
  def nop: Logger = new Nop
}
