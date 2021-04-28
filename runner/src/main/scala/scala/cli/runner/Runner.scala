package scala.cli.runner

import java.lang.reflect.InvocationTargetException

object Runner {
  def bold = Console.BOLD
  def gray = "\u001b[90m"
  def reset = Console.RESET
  @annotation.tailrec
  def printCause(ex: Throwable, causedStackTrace: Array[StackTraceElement], loader: ClassLoader): Unit =
    if (ex != null) {
      System.err.println(s"Caused by: $ex")
      printStackTrace(ex.getStackTrace, causedStackTrace, loader)
      printCause(ex.getCause, ex.getStackTrace, loader)
    }
  def printStackTrace(trace: Array[StackTraceElement], loader: ClassLoader): Unit =
    printStackTrace(trace, Array.empty, loader)
  def printStackTrace(trace: Array[StackTraceElement], causedStackTrace: Array[StackTraceElement], loader: ClassLoader): Unit = {
    val cut = causedStackTrace
      .reverseIterator
      .zip(trace.reverseIterator)
      .takeWhile { case (a, b) => a == b }
      .length
    for (elem <- trace.take(trace.length - cut)) {
      val clsName = elem.getClassName
      val resource = clsName.replace('.', '/') + ".class"
      val resourceUrl = loader.getResource(resource)
      val highlight = resourceUrl != null && resourceUrl.getProtocol == "file"
      if (highlight) {
        val location =
          if (elem.isNativeMethod) "Native Method"
          else if (elem.getFileName == null) "Unknown Source"
          else if (elem.getLineNumber >= 0) s"${elem.getFileName}:${elem.getLineNumber}"
          else elem.getFileName
        val str = s"${bold}${elem.getClassName}.${elem.getMethodName}$reset$gray($reset$location$gray)$reset"
        System.err.println(s"\t${gray}at$reset $str")
      } else
        System.err.println(s"\t${gray}at $elem")
    }
    if (cut > 0)
      System.err.println(s"\t$gray... $cut more$reset")
  }
  def printException(ex: Throwable, loader: ClassLoader): Unit = {
    val q = "\""
    val threadName = Thread.currentThread().getName
    System.err.println(s"Exception in thread $q$threadName$q $ex")
    printStackTrace(ex.getStackTrace, loader)
    printCause(ex.getCause, ex.getStackTrace, loader)
  }
  def main(args: Array[String]): Unit = {
    assert(args.nonEmpty)
    val mainClass = args.head
    val args0 = args.tail

    val loader = Thread.currentThread().getContextClassLoader
    val cls = loader.loadClass(mainClass)
    val method = cls.getMethod("main", classOf[Array[String]])
    try method.invoke(null, args0)
    catch {
      case e: InvocationTargetException if e.getCause != null =>
        printException(e.getCause, loader)
        System.exit(1)
    }
  }
}
