package scala.cli.runner

import com.virtuslab.stacktracebuddy.core.StackTraceBuddy
import com.virtuslab.stacktracebuddy.printer.PrettyExceptionPrinter

object Stacktrace {

  def print(t: Throwable, prefix: String): Boolean = {
    val e = t match {
      case e: Exception => e
      case _ => new Exception(t) // meh
    }
    val prettyStackTrace = StackTraceBuddy.convertToPrettyStackTrace(e)
    Console.out.print(prefix)
    PrettyExceptionPrinter.printStacktrace(prettyStackTrace)
    true
  }

}
