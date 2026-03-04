package scala.build

import bloop.rifle.BloopRifleLogger
import org.scalajs.logging.{Logger as ScalaJsLogger, NullLogger}

import java.io.{OutputStream, PrintStream}

import scala.annotation.unused
import scala.build.errors.{BuildException, Diagnostic, Severity}
import scala.build.internals.FeatureType
import scala.scalanative.build as sn

trait Logger {
  def error(message: String): Unit
  // TODO Use macros for log and debug calls to have zero cost when verbosity <= 0
  def message(message: => String): Unit
  def log(s: => String): Unit
  def log(s: => String, debug: => String): Unit
  def debug(s: => String): Unit

  def debugStackTrace(t: => Throwable): Unit = t.getStackTrace.foreach(ste => debug(ste.toString))

  def log(diagnostics: Seq[Diagnostic]): Unit

  def diagnostic(
    message: String,
    severity: Severity = Severity.Warning,
    positions: Seq[Position] = Nil
  ): Unit = log(Seq(Diagnostic(message, severity, positions)))

  def log(ex: BuildException): Unit
  def debug(ex: BuildException): Unit
  def exit(ex: BuildException): Nothing

  def coursierLogger(printBefore: String): coursier.cache.CacheLogger
  def bloopRifleLogger: BloopRifleLogger
  def scalaJsLogger: ScalaJsLogger
  def scalaNativeTestLogger: sn.Logger
  def scalaNativeCliInternalLoggerOptions: List[String]

  def compilerOutputStream: PrintStream

  def verbosity: Int

  /** Since we have a lot of experimental warnings all over the build process, this method can be
    * used to accumulate them for a better UX
    */
  def experimentalWarning(featureName: String, featureType: FeatureType): Unit
  def flushExperimentalWarnings: Unit

  def cliFriendlyDiagnostic(
    message: String,
    @unused cliFriendlyMessage: String,
    severity: Severity = Severity.Warning,
    positions: Seq[Position] = Nil
  ): Unit = diagnostic(message, severity, positions)
}

object Logger {
  private class Nop extends Logger {
    def error(message: String): Unit              = ()
    def message(message: => String): Unit         = ()
    def log(s: => String): Unit                   = ()
    def log(s: => String, debug: => String): Unit = ()
    def debug(s: => String): Unit                 = ()

    def log(diagnostics: Seq[Diagnostic]): Unit = ()
    def log(ex: BuildException): Unit           = ()
    def debug(ex: BuildException): Unit         = ()
    def exit(ex: BuildException): Nothing       =
      throw new Exception(ex)

    def coursierLogger(printBefore: String): coursier.cache.CacheLogger =
      coursier.cache.CacheLogger.nop
    def bloopRifleLogger: BloopRifleLogger =
      BloopRifleLogger.nop
    def scalaJsLogger: ScalaJsLogger =
      NullLogger
    def scalaNativeTestLogger: sn.Logger =
      sn.Logger.nullLogger
    def scalaNativeCliInternalLoggerOptions: List[String] =
      List()

    def compilerOutputStream: PrintStream =
      new PrintStream(
        new OutputStream {
          override def write(b: Int): Unit                             = ()
          override def write(b: Array[Byte], off: Int, len: Int): Unit = ()
        }
      )

    def verbosity: Int = -1

    def experimentalWarning(featureUsed: String, featureType: FeatureType): Unit = ()
    def flushExperimentalWarnings: Unit                                          = ()
  }
  def nop: Logger = new Nop
}
