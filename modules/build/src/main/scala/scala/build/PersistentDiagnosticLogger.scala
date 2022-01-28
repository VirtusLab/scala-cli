package scala.build

import java.io.PrintStream

import scala.build.blooprifle.BloopRifleLogger
import scala.build.errors.{BuildException, Diagnostic}
import scala.scalanative.{build => sn}

class PersistentDiagnosticLogger(parent: Logger) extends Logger {
  private val diagBuilder = List.newBuilder[Diagnostic]

  def diagnostics = diagBuilder.result()

  // TODO Use macros for log and debug calls to have zero cost when verbosity <= 0
  def message(message: => String): Unit         = parent.message(message)
  def log(s: => String): Unit                   = parent.log(s)
  def log(s: => String, debug: => String): Unit = parent.log(s, debug)
  def debug(s: => String): Unit                 = parent.debug(s)

  def log(diagnostics: Seq[Diagnostic]): Unit = {
    parent.log(diagnostics)
    diagBuilder ++= diagnostics
  }

  def log(ex: BuildException): Unit     = parent.log(ex)
  def exit(ex: BuildException): Nothing = parent.exit(ex)

  def coursierLogger: coursier.cache.CacheLogger        = parent.coursierLogger
  def bloopRifleLogger: BloopRifleLogger                = parent.bloopRifleLogger
  def scalaNativeTestLogger: sn.Logger                  = parent.scalaNativeTestLogger
  def scalaNativeCliInternalLoggerOptions: List[String] = parent.scalaNativeCliInternalLoggerOptions

  def compilerOutputStream: PrintStream = parent.compilerOutputStream
}
