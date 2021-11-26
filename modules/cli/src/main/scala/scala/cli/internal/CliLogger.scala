package scala.cli.internal

import ch.epfl.scala.{bsp4j => b}
import coursier.cache.CacheLogger
import coursier.cache.loggers.{FallbackRefreshDisplay, ProgressBarRefreshDisplay, RefreshLogger}

import java.io.PrintStream

import scala.build.blooprifle.BloopRifleLogger
import scala.build.errors.{BuildException, CompositeBuildException, Severity}
import scala.build.{ConsoleBloopBuildClient, Logger, Position}
import scala.collection.mutable
import scala.scalanative.{build => sn}
class CliLogger(
  verbosity: Int,
  quiet: Boolean,
  progress: Option[Boolean],
  out: PrintStream
) extends Logger { logger =>

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

  private def printEx(
    ex: BuildException,
    contentCache: mutable.Map[os.Path, Seq[String]],
    severity: Severity
  ): Unit =
    ex match {
      case c: CompositeBuildException =>
        // FIXME We might want to order things here… Or maybe just collect all b.Diagnostics
        // below, and order them before printing them.
        for (ex <- c.exceptions)
          printEx(ex, contentCache, severity)
      case _ =>
        if (ex.positions.isEmpty)
          out.println(ex.getMessage)
        else {
          val positions = ex.positions.distinct
          val filePositions = positions.collect {
            case f: Position.File => f
          }
          val otherPositions = positions.filter {
            case _: Position.File => false
            case _                => true
          }

          for (f <- filePositions) {
            val startPos = new b.Position(f.startPos._1, f.startPos._2)
            val endPos   = new b.Position(f.endPos._1, f.endPos._2)
            val range    = new b.Range(startPos, endPos)
            val diag     = new b.Diagnostic(range, ex.getMessage)
            diag.setSeverity(severity match {
              case Severity.ERROR   => b.DiagnosticSeverity.ERROR
              case Severity.WARNING => b.DiagnosticSeverity.WARNING
            })

            for (file <- f.path) {
              val lines = contentCache.getOrElseUpdate(file, os.read(file).linesIterator.toVector)
              if (f.startPos._1 < lines.length)
                diag.setCode(lines(f.startPos._1))
            }
            ConsoleBloopBuildClient.printDiagnostic(
              out,
              f.path,
              diag
            )
          }

          if (otherPositions.nonEmpty) {
            for (pos <- otherPositions)
              out.println(pos.render())
            out.print("  ")
            out.println(ex.getMessage)
          }
        }
    }

  def log(ex: BuildException, severity: Severity): Unit =
    if (verbosity >= 0)
      printEx(ex, new mutable.HashMap, severity)

  def exit(ex: BuildException): Nothing = exit(ex, Severity.ERROR)

  def exit(ex: BuildException, severity: Severity): Nothing =
    if (verbosity < 0)
      sys.exit(1)
    else if (verbosity == 0) {
      printEx(ex, new mutable.HashMap, severity)
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
