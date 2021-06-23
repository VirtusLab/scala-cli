package scala.build

import coursier.cache.CacheLogger
import scala.build.blooprifle.BloopRifleLogger

trait Logger {
  // TODO Use macros for log and debug calls to have zero cost when verbosity <= 0
  def log(s: => String): Unit
  def log(s: => String, debug: => String): Unit
  def debug(s: => String): Unit

  def coursierLogger: coursier.cache.CacheLogger

  def bloopRifleLogger: BloopRifleLogger
}

object Logger {
  private class Nop extends Logger {
    def log(s: => String): Unit = ()
    def log(s: => String, debug: => String): Unit = ()
    def debug(s: => String): Unit = ()

    def coursierLogger: coursier.cache.CacheLogger =
      coursier.cache.CacheLogger.nop

    def bloopRifleLogger: BloopRifleLogger =
      BloopRifleLogger.nop
  }
  def nop: Logger = new Nop
}
