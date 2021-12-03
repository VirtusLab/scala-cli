package scala.cli.internal

import ch.epfl.scala.{bsp4j => b}
import coursier.cache.CacheLogger
import coursier.cache.loggers.{FallbackRefreshDisplay, ProgressBarRefreshDisplay, RefreshLogger}

import java.io.PrintStream

import scala.build.blooprifle.BloopRifleLogger
import scala.build.errors.{BuildException, CompositeBuildException, Diagnostic, Severity}
import scala.build.{ConsoleBloopBuildClient, Logger, Position}
import scala.collection.mutable
import scala.scalanative.{build => sn}
class CliLogger(
  verbosity: Int,
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
        hashMap
      )
    }
  }

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
    contentCache: mutable.Map[os.Path, Seq[String]]
  ) =
    if (positions.isEmpty)
      out.println(message)
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
        diag.setSeverity(severity match {
          case Severity.Error   => b.DiagnosticSeverity.ERROR
          case Severity.Warning => b.DiagnosticSeverity.WARNING
        })

        for (file <- f.path) {
          val lines = contentCache.getOrElseUpdate(file, os.read(file).linesIterator.toVector)
          if (f.startPos._1 < lines.length)
            diag.setCode(lines(f.startPos._1))
        }
        ConsoleBloopBuildClient.printFileDiagnostic(
          out,
          f.path,
          diag
        )
      }

      if (otherPositions.nonEmpty)
        ConsoleBloopBuildClient.printOtherDiagnostic(
          out,
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
        printDiagnostic(ex.positions, Severity.Error, ex.getMessage(), contentCache)
    }

  def log(ex: BuildException): Unit =
    if (verbosity >= 0)
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

  def coursierLogger =
    if (quiet)
      CacheLogger.nop
    else if (progress.getOrElse(coursier.paths.Util.useAnsiOutput()))
      RefreshLogger.create(ProgressBarRefreshDisplay.create())
    else
      RefreshLogger.create(new FallbackRefreshDisplay)

  def bloopRifleLogger =
    new BloopRifleLogger {
      def info(msg: => String) = logger.message(msg)
      def debug(msg: => String) =
        if (verbosity >= 3)
          logger.debug(msg)
      def error(msg: => String, ex: Throwable) =
        logger.log(s"Error: $msg ($ex)")
      def bloopBspStdout =
        if (verbosity >= 2) Some(out)
        else None
      def bloopBspStderr =
        if (verbosity >= 2) Some(out)
        else None
      def bloopCliInheritStdout = verbosity >= 3
      def bloopCliInheritStderr = verbosity >= 3
    }

  def scalaNativeLogger: sn.Logger =
    new sn.Logger {
      def trace(msg: Throwable) = ()
      def debug(msg: String)    = logger.debug(msg)
      def info(msg: String)     = logger.message(msg)
      def warn(msg: String)     = logger.log(msg)
      def error(msg: String)    = logger.log(msg)
    }

  // Allow to disable that?
  def compilerOutputStream = out
}
