package scala.cli.runner

import org.virtuslab.stacktraces.printer.PrettyExceptionPrinter

object Stacktrace {

  def print(t: Throwable, prefix: String): Boolean = {
    val e = t match {
      case e: Exception => e
      case _ => new Exception(t) // meh
    }
    val prettyStackTrace = Stacktraces.convertToPrettyStackTrace(e)
    Console.out.print(prefix)
    PrettyExceptionPrinter.printStacktrace(prettyStackTrace)
    true
  }

}
