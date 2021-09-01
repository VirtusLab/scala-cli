package scala.cli.runner

import org.virtuslab.stacktraces.printer.PrettyExceptionPrinter

object Stacktrace {

  private lazy val disable = java.lang.Boolean.getBoolean("scala.cli.runner.Stacktrace.disable")

  def print(t: Throwable, prefix: String): Boolean = !disable && {
    val e = t match {
      case e: Exception => e
      case _            => new Exception(t) // meh
    }
    val prettyStackTrace = Stacktraces.convertToPrettyStackTrace(e)
    Console.out.print(prefix)
    PrettyExceptionPrinter.printStacktrace(prettyStackTrace)
    true
  }

}
