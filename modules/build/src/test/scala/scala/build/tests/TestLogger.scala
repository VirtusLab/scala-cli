package scala.build.tests

import bloop.rifle.BloopRifleLogger
import coursier.cache.CacheLogger
import coursier.cache.loggers.{FallbackRefreshDisplay, RefreshLogger}
import org.scalajs.logging.{NullLogger, Logger as ScalaJsLogger}

import scala.build.errors.BuildException
import scala.build.Logger
import scala.scalanative.build as sn
import scala.build.errors.Diagnostic
import scala.build.internals.FeatureType

case class TestLogger(info: Boolean = true, debug: Boolean = false) extends Logger {

  override def log(diagnostics: Seq[Diagnostic]): Unit = {
    diagnostics.foreach { d =>
      System.err.println(d.positions.map(_.render()).mkString("/") ++ ": " ++ d.message)
    }
  }

  def error(message: String): Unit =
    System.err.println(message)
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
  def debug(ex: BuildException): Unit =
    debug(ex.getMessage)
  def exit(ex: BuildException): Nothing =
    throw new Exception(ex)

  def coursierLogger(message: String): CacheLogger =
    RefreshLogger.create(new FallbackRefreshDisplay)

  def bloopRifleLogger: BloopRifleLogger =
    if (debug)
      new BloopRifleLogger {
        def bloopBspStderr: Option[java.io.OutputStream] = Some(System.err)
        def bloopBspStdout: Option[java.io.OutputStream] = Some(System.out)
        def bloopCliInheritStderr: Boolean               = true
        def bloopCliInheritStdout: Boolean               = true
        def debug(msg: => String, ex: Throwable): Unit = {
          System.err.println(msg)
          if (ex != null) ex.printStackTrace(System.err)
        }
        def error(msg: => String, ex: Throwable): Unit = {
          System.err.println(msg)
          if (ex != null) ex.printStackTrace(System.err)
        }
        def error(msg: => String): Unit = System.err.println(msg)
        def info(msg: => String): Unit =
          System.err.println(msg)
      }
    else
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

  def verbosity =
    if (debug) 2
    else if (info) 0
    else -1

  override def experimentalWarning(featureName: String, featureType: FeatureType): Unit =
    System.err.println(s"Experimental $featureType `$featureName` used")

  override def flushExperimentalWarnings: Unit = ()
}
