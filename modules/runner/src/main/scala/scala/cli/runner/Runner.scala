package scala.cli.runner

import java.lang.reflect.InvocationTargetException

object Runner {
  def main(args: Array[String]): Unit = {
    assert(args.nonEmpty)
    val mainClass = args.head
    val args0     = args.tail

    val loader = Thread.currentThread().getContextClassLoader
    val cls    = loader.loadClass(mainClass)
    val method = cls.getMethod("main", classOf[Array[String]])
    try method.invoke(null, args0)
    catch {
      case e: InvocationTargetException if e.getCause != null =>
        val printer = StackTracePrinter(
          loader = loader,
          callerClass = Some(getClass.getName),
          cutInvoke = true
        )
        printer.printException(e.getCause)
        System.exit(1)
    }
  }
}
