package scala.cli.internal

import coursier.cache.CacheLogger
import coursier.cache.loggers.{FallbackRefreshDisplay, ProgressBarRefreshDisplay, RefreshLogger}

import java.io.PrintStream

import scala.build.Logger
import scala.build.blooprifle.BloopRifleLogger
import scala.build.errors.{BuildException, CompositeBuildException}
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

  private def printEx(ex: BuildException): Unit =
    ex match {
      case c: CompositeBuildException =>
        for (ex <- c.exceptions)
          printEx(ex)
      case _ =>
        for (pos <- ex.positions.distinct)
          out.println("Error: " + pos.render())
        if (ex.positions.nonEmpty)
          out.print("  ")
        out.println(ex.getMessage)
    }

  def log(ex: BuildException): Unit =
    if (verbosity >= 0)
      printEx(ex)

  def exit(ex: BuildException): Nothing =
    if (verbosity < 0)
      sys.exit(1)
    else if (verbosity == 0) {
      printEx(ex)
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
