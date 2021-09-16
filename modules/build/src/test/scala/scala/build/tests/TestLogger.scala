package scala.build.tests

import coursier.cache.CacheLogger
import coursier.cache.loggers.{FallbackRefreshDisplay, RefreshLogger}

import scala.build.blooprifle.BloopRifleLogger
import scala.build.errors.BuildException
import scala.build.Logger
import scala.scalanative.{build => sn}

case class TestLogger(info: Boolean = true, debug: Boolean = false) extends Logger {
  def message(message: => String): Unit =
    if (info)
      System.err.println(message)
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

  def log(ex: BuildException): Unit =
    System.err.println(ex.getMessage)
  def exit(ex: BuildException): Nothing =
    throw new Exception(ex)

  def coursierLogger: CacheLogger =
    RefreshLogger.create(new FallbackRefreshDisplay)

  def bloopRifleLogger: BloopRifleLogger =
    BloopRifleLogger.nop
  def scalaNativeLogger: sn.Logger =
    sn.Logger.nullLogger
  def bloopBspStderr        = None
  def bloopBspStdout        = None
  def bloopCliInheritStdout = false
  def bloopCliInheritStderr = false

  def compilerOutputStream = System.err
}
