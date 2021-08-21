package scala.build.blooprifle

import java.io.OutputStream

import scala.util.control.NonFatal

trait BloopRifleLogger {
  def debug(msg: => String): Unit
  def error(msg: => String, ex: Throwable): Unit
  def runnable(name: String)(r: Runnable): Runnable = { () =>
    try r.run()
    catch {
      case NonFatal(e) =>
        error(s"Error running $name", e)
    }
  }
  def bloopBspStdout: Option[OutputStream]
  def bloopBspStderr: Option[OutputStream]
  def bloopCliInheritStdout: Boolean
  def bloopCliInheritStderr: Boolean
}

object BloopRifleLogger {
  def nop: BloopRifleLogger =
    new BloopRifleLogger {
      def debug(msg: => String) = {}
      def error(msg: => String, ex: Throwable) = {}
      def bloopBspStdout        = None
      def bloopBspStderr        = None
      def bloopCliInheritStdout = false
      def bloopCliInheritStderr = false
    }
}
