package scala.cli.runner

import org.virtuslab.stacktraces.printer.PrettyExceptionPrinter

object Stacktrace {

  private lazy val disable = java.lang.Boolean.getBoolean("scala.cli.runner.Stacktrace.disable")

  def print(t: Throwable, prefix: String, verbosity: Int): Boolean = !disable && {
    val e = t
    try
      val prettyStackTrace = Stacktraces.convertToPrettyStackTrace(e)
      Console.out.print(prefix)
      PrettyExceptionPrinter.printStacktrace(prettyStackTrace)
      true
    catch
      case e: Throwable => // meh meh
        if (verbosity >= 1) {
          Console.out.print("Failed to process exception, failure:")
          e.printStackTrace()
          Console.out.println("----\n")
        }
        false
  }

}
