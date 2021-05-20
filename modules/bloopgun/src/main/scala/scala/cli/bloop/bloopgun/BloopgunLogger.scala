package scala.cli.bloop.bloopgun

import scala.util.control.NonFatal

trait BloopgunLogger {
  def debug(msg: => String): Unit
  def error(msg: => String, ex: Throwable): Unit
  def runnable(name: String)(r: Runnable): Runnable = { () =>
    try r.run()
    catch {
      case NonFatal(e) =>
        error(s"Error running $name", e)
    }
  }
  def coursierInterfaceLogger: coursierapi.Logger
}

object BloopgunLogger {
  def nop: BloopgunLogger =
    new BloopgunLogger {
      def debug(msg: => String): Unit = {}
      def error(msg: => String, ex: Throwable): Unit = {}
      def coursierInterfaceLogger = coursierapi.Logger.nop()
    }
}
