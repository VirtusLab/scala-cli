package scala.cli.internal

import java.io.PrintStream

object Util {

  def printException(t: Throwable, out: PrintStream = System.err): Unit =
    if (t != null) {
      out.println(t)
      for (l <- t.getStackTrace)
        out.println(s"  $l")
      printException(t.getCause, out)
    }
}