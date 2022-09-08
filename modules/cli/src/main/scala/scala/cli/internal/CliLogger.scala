package scala.cli.internal

import ch.epfl.scala.bsp4j.Location
import ch.epfl.scala.{bsp4j => b}
import coursier.cache.CacheLogger
import coursier.cache.loggers.{FallbackRefreshDisplay, RefreshLogger}
import org.scalajs.logging.{Level => ScalaJsLevel, Logger => ScalaJsLogger, ScalaConsoleLogger}

import java.io.PrintStream

import scala.build.blooprifle.BloopRifleLogger
import scala.build.errors.Diagnostic.RelatedInformation
import scala.build.errors.{BuildException, CompositeBuildException, Diagnostic, Severity}
import scala.build.internal.CustomProgressBarRefreshDisplay
import scala.build.{ConsoleBloopBuildClient, Logger, Position}
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.scalanative.{build => sn}

class CliLogger(
  val verbosity: Int,
  quiet: Boolean,
  progress: Option[Boolean],
  out: PrintStream
) extends Logger { logger =>

  override def log(diagnostics: Seq[Diagnostic]): Unit = {
    val hashMap = new mutable.HashMap[os.Path, Seq[String]]
    diagnostics.foreach { d =>
      printDiagnostic(
        d.positions,
        d.severity,
        d.message,
        hashMap,
        d.relatedInformation
      )
    }
  }

  def error(message: String) =
    out.println(message)
  def message(message: => String) =
    if (verbosity >= 0)
      out.println(message)
  def log(message: => String) =
    if (verbosity >= 1)
      out.println(message)
  def log(message: => String, debugMessage: => String) =
    if (verbosity >= 2)
      out.println(debugMessage)
    else if (verbosity >= 1)
      out.println(message)
  def debug(message: => String) =
    if (verbosity >= 2)
      out.println(message)

  def printDiagnostic(
    positions: Seq[Position],
    severity: Severity,
    message: String,
    contentCache: mutable.Map[os.Path, Seq[String]],
    relatedInformation: Option[RelatedInformation]
  ) =
    if (positions.isEmpty)
      out.println(
        s"${ConsoleBloopBuildClient.diagnosticPrefix(severity)} $message"
      )
    else {
      val positions0 = positions.distinct
      val filePositions = positions0.collect {
        case f: Position.File => f
      }
      val otherPositions = positions0.filter {
        case _: Position.File => false
        case _                => true
      }

      for (f <- filePositions) {
        val startPos = new b.Position(f.startPos._1, f.startPos._2)
        val endPos   = new b.Position(f.endPos._1, f.endPos._2)
        val range    = new b.Range(startPos, endPos)
        val diag     = new b.Diagnostic(range, message)
        diag.setSeverity(severity.toBsp4j)
        diag.setSource("scala-cli")

        for {
          filePath <- f.path
          info     <- relatedInformation
        } {
          val location = new Location(filePath.toNIO.toUri.toASCIIString, range)
          val related  = new b.DiagnosticRelatedInformation(location, info.message)
          diag.setRelatedInformation(List(related).asJava)
        }

        for (file <- f.path) {
          val lines = contentCache.getOrElseUpdate(file, os.read(file).linesIterator.toVector)
          if (f.startPos._1 < lines.length)
            diag.setCode(lines(f.startPos._1))
        }
        ConsoleBloopBuildClient.printFileDiagnostic(
          this,
          f.path,
          diag
        )
      }

      if (otherPositions.nonEmpty)
        ConsoleBloopBuildClient.printOtherDiagnostic(
          this,
          message,
          severity,
          otherPositions
        )
    }

  private def printEx(
    ex: BuildException,
    contentCache: mutable.Map[os.Path, Seq[String]]
  ): Unit =
    ex match {
      case c: CompositeBuildException =>
        // FIXME We might want to order things here… Or maybe just collect all b.Diagnostics
        // below, and order them before printing them.
        for (ex <- c.exceptions)
          printEx(ex, contentCache)
      case _ =>
        printDiagnostic(ex.positions, Severity.Error, ex.getMessage(), contentCache, None)
    }

  def log(ex: BuildException): Unit =
    if (verbosity >= 0)
      printEx(ex, new mutable.HashMap)

  def debug(ex: BuildException): Unit =
    if (verbosity >= 2)
      printEx(ex, new mutable.HashMap)
  def exit(ex: BuildException): Nothing =
    if (verbosity < 0)
      sys.exit(1)
    else if (verbosity == 0) {
      printEx(ex, new mutable.HashMap)
      sys.exit(1)
    }
    else
      throw new Exception(ex)

  def coursierLogger(printBefore: String) =
    if (quiet)
      CacheLogger.nop
    else if (progress.getOrElse(coursier.paths.Util.useAnsiOutput()))
      RefreshLogger.create(
        CustomProgressBarRefreshDisplay.create(
          keepOnScreen = verbosity >= 1,
          if (printBefore.nonEmpty) System.err.println(printBefore),
          ()
        )
      )
    else
      RefreshLogger.create(new FallbackRefreshDisplay)

  def bloopRifleLogger =
    new BloopRifleLogger {
      def info(msg: => String) = logger.message(msg)
      def debug(msg: => String, ex: Throwable) =
        if (verbosity >= 3) {
          logger.debug(msg)
          if (ex != null)
            ex.printStackTrace(out)
        }
      def error(msg: => String, ex: Throwable) = {
        logger.log(s"Error: $msg ($ex)")
        if (verbosity >= 1 && ex != null)
          ex.printStackTrace(out)
      }
      def bloopBspStdout =
        if (verbosity >= 2) Some(out)
        else None
      def bloopBspStderr =
        if (verbosity >= 2) Some(out)
        else None
      def bloopCliInheritStdout = verbosity >= 3
      def bloopCliInheritStderr = verbosity >= 3
    }

  def scalaJsLogger: ScalaJsLogger =
    // FIXME Doesn't use 'out'
    new ScalaConsoleLogger(
      minLevel =
        if (verbosity >= 2) ScalaJsLevel.Debug
        else if (verbosity >= 1) ScalaJsLevel.Info
        else if (verbosity >= 0) ScalaJsLevel.Warn
        else ScalaJsLevel.Error
    )

  def scalaNativeTestLogger: sn.Logger =
    new sn.Logger {
      def trace(msg: Throwable) = ()
      def debug(msg: String)    = logger.debug(msg)
      def info(msg: String)     = logger.log(msg)
      def warn(msg: String)     = logger.log(msg)
      def error(msg: String)    = logger.message(msg)
    }

  val scalaNativeCliInternalLoggerOptions: List[String] =
    if (verbosity >= 1) List("-v", "-v", "-v") // debug
    else if (verbosity >= 0) List("-v", "-v")  // info
    else List()                                // error

  // Allow to disable that?
  def compilerOutputStream = out
}
