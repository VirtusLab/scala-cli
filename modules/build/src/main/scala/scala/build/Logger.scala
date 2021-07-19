package scala.build

import coursier.cache.CacheLogger

import java.io.{OutputStream, PrintStream}

import scala.build.blooprifle.BloopRifleLogger
import scala.scalanative.{build => sn}

trait Logger {
  // TODO Use macros for log and debug calls to have zero cost when verbosity <= 0
  def log(s: => String): Unit
  def log(s: => String, debug: => String): Unit
  def debug(s: => String): Unit

  def coursierLogger: coursier.cache.CacheLogger
  def bloopRifleLogger: BloopRifleLogger
  def scalaNativeLogger: sn.Logger

  def compilerOutputStream: PrintStream
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
    def scalaNativeLogger: sn.Logger =
      sn.Logger.nullLogger

    def compilerOutputStream: PrintStream =
      new PrintStream(
        new OutputStream {
          override def write(b: Int): Unit = ()
          override def write(b: Array[Byte], off: Int, len: Int): Unit = ()
        }
      )
  }
  def nop: Logger = new Nop
}
