package scala.build.tests

import bloop.rifle.BloopRifleLogger
import coursier.cache.CacheLogger
import coursier.cache.loggers.{FallbackRefreshDisplay, RefreshLogger}
import org.scalajs.logging.{Logger as ScalaJsLogger, NullLogger}

import java.io.PrintStream

import scala.build.Logger
import scala.build.errors.{BuildException, Diagnostic}
import scala.build.internals.FeatureType
import scala.collection.mutable.ListBuffer
import scala.scalanative.build as sn

/** Logger that records all message() and log() calls for test assertions. */
final class RecordingLogger(delegate: Logger = TestLogger()) extends Logger {
  val messages: ListBuffer[String] = ListBuffer.empty

  override def error(message: String): Unit      = delegate.error(message)
  override def message(message: => String): Unit = {
    val msg = message
    messages += msg
    delegate.message(msg)
  }
  override def log(s: => String): Unit = {
    val msg = s
    messages += msg
    delegate.log(msg)
  }
  override def log(s: => String, debug: => String): Unit         = delegate.log(s, debug)
  override def debug(s: => String): Unit                         = delegate.debug(s)
  override def log(diagnostics: Seq[Diagnostic]): Unit           = delegate.log(diagnostics)
  override def log(ex: BuildException): Unit                     = delegate.log(ex)
  override def debug(ex: BuildException): Unit                   = delegate.debug(ex)
  override def exit(ex: BuildException): Nothing                 = delegate.exit(ex)
  override def coursierLogger(message: String): CacheLogger      = delegate.coursierLogger(message)
  override def bloopRifleLogger: BloopRifleLogger                = delegate.bloopRifleLogger
  override def scalaJsLogger: ScalaJsLogger                      = delegate.scalaJsLogger
  override def scalaNativeTestLogger: sn.Logger                  = delegate.scalaNativeTestLogger
  override def scalaNativeCliInternalLoggerOptions: List[String] =
    delegate.scalaNativeCliInternalLoggerOptions
  override def compilerOutputStream: PrintStream = delegate.compilerOutputStream
  override def verbosity: Int                    = delegate.verbosity
  override def experimentalWarning(featureName: String, featureType: FeatureType): Unit =
    delegate.experimentalWarning(featureName, featureType)
  override def flushExperimentalWarnings: Unit = delegate.flushExperimentalWarnings
}

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
        def debug(msg: => String): Unit                  = {
          System.err.println(msg)
        }
        def debug(msg: => String, ex: Throwable): Unit = {
          System.err.println(msg)
          if (ex != null) ex.printStackTrace(System.err)
        }
        def error(msg: => String, ex: Throwable): Unit = {
          System.err.println(msg)
          if (ex != null) ex.printStackTrace(System.err)
        }
        def error(msg: => String): Unit = System.err.println(msg)
        def info(msg: => String): Unit  =
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

  def compilerOutputStream: PrintStream = System.err

  def verbosity: Int =
    if debug then 2
    else if info then 0
    else -1

  override def experimentalWarning(featureName: String, featureType: FeatureType): Unit =
    System.err.println(s"Experimental $featureType `$featureName` used")

  override def flushExperimentalWarnings: Unit = ()
}
