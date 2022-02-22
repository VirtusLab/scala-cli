package scala.build.tests

import coursier.cache.CacheLogger
import coursier.cache.loggers.{FallbackRefreshDisplay, RefreshLogger}
import org.scalajs.logging.{Logger => ScalaJsLogger, NullLogger}

import scala.build.blooprifle.BloopRifleLogger
import scala.build.errors.BuildException
import scala.build.Logger
import scala.scalanative.{build => sn}
import scala.build.errors.Diagnostic

case class TestLogger(info: Boolean = true, debug: Boolean = false) extends Logger {

  override def log(diagnostics: Seq[Diagnostic]): Unit = {
    diagnostics.foreach { d =>
      System.err.println(d.positions.map(_.render()).mkString("/") ++ ": " ++ d.message)
    }
  }

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

  def coursierLogger(message: String): CacheLogger =
    RefreshLogger.create(new FallbackRefreshDisplay)

  def bloopRifleLogger: BloopRifleLogger =
    BloopRifleLogger.nop
  def scalaJsLogger: ScalaJsLogger =
    NullLogger
  def scalaNativeTestLogger: sn.Logger =
    sn.Logger.nullLogger
  def scalaNativeCliInternalLoggerOptions: List[String] =
    List()
  def bloopBspStderr        = None
  def bloopBspStdout        = None
  def bloopCliInheritStdout = false
  def bloopCliInheritStderr = false

  def compilerOutputStream = System.err
}
