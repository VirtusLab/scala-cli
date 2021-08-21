package scala.cli.runner

import java.util.Locale

import scala.annotation.tailrec

final case class StackTracePrinter(
  loader: ClassLoader = Thread.currentThread().getContextClassLoader,
  callerClass: Option[String] = None,
  cutInvoke: Boolean = false,
  colored: Boolean = StackTracePrinter.coloredStackTraces
) {

  private def bold  = if (colored) Console.BOLD else ""
  private def gray  = if (colored) "\u001b[90m" else ""
  private def reset = if (colored) Console.RESET else ""

  private def truncateStackTrace(ex: Throwable): Unit = {
    val noCallerStackTrace = callerClass match {
      case None => ex.getStackTrace
      case Some(caller) =>
        ex.getStackTrace
          .takeWhile(_.getClassName.stripSuffix("$") != caller.stripSuffix("$"))
    }
    val drop =
      if (cutInvoke)
        noCallerStackTrace
          .reverseIterator
          .takeWhile { elem =>
            def isJdkClass =
              elem.getClassName.startsWith("java.") ||
              elem.getClassName.startsWith("jdk.") ||
              elem.getClassName.startsWith("sun.")
            elem.getMethodName.startsWith("invoke") && isJdkClass
          }
          .length
      else
        0
    val truncated = noCallerStackTrace.dropRight(drop)
    if (truncated.length != ex.getStackTrace.length)
      ex.setStackTrace(truncated)
  }

  @tailrec
  private def printCause(ex: Throwable, causedStackTrace: Array[StackTraceElement]): Unit =
    if (ex != null) {
      truncateStackTrace(ex)
      if (!Stacktrace.print(ex, "Caused by: ")) {
        System.err.println(s"Caused by: $ex")
        printStackTrace(ex.getStackTrace, causedStackTrace)
      }
      printCause(ex.getCause, ex.getStackTrace)
    }
  private def printStackTrace(trace: Array[StackTraceElement]): Unit =
    printStackTrace(trace, Array.empty)
  private def printStackTrace(
    trace: Array[StackTraceElement],
    causedStackTrace: Array[StackTraceElement]
  ): Unit = {
    val cut = causedStackTrace
      .reverseIterator
      .zip(trace.reverseIterator)
      .takeWhile { case (a, b) => a == b }
      .length
    for (elem <- trace.take(trace.length - cut)) {
      val clsName     = elem.getClassName
      val resource    = clsName.replace('.', '/') + ".class"
      val resourceUrl = loader.getResource(resource)
      val highlight   = resourceUrl != null && resourceUrl.getProtocol == "file"
      if (highlight) {
        val location =
          if (elem.isNativeMethod) "Native Method"
          else if (elem.getFileName == null) "Unknown Source"
          else if (elem.getLineNumber >= 0) s"${elem.getFileName}:${elem.getLineNumber}"
          else elem.getFileName
        val str = s"${bold}${elem.getClassName}.${elem.getMethodName}$reset" +
          s"$gray($reset$location$gray)$reset"
        System.err.println(s"\t${gray}at$reset $str")
      }
      else
        System.err.println(s"\t${gray}at $elem")
    }
    if (cut > 0)
      System.err.println(s"\t$gray... $cut more$reset")
  }

  def printException(ex: Throwable): Unit = {
    val q          = "\""
    val threadName = Thread.currentThread().getName
    truncateStackTrace(ex)
    if (!Stacktrace.print(ex, "")) {
      System.err.println(s"Exception in thread $q$threadName$q $ex")
      printStackTrace(ex.getStackTrace)
    }
    printCause(ex.getCause, ex.getStackTrace)
  }
}

object StackTracePrinter {

  lazy val coloredStackTraces: Boolean =
    sys.props.get("scala.colored-stack-traces")
      .map(_.toLowerCase(Locale.ROOT))
      .forall(_ == "true")

}
