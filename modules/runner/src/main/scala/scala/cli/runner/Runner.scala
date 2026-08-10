package scala.cli.runner

import java.lang.reflect.{InvocationTargetException, Method, Modifier}

import scala.util.Try

object Runner {
  def main(args: Array[String]): Unit = {
    assert(args.nonEmpty)
    val mainClass = args.head
    val verbosity = args.tail.head.toInt
    val args0     = args.drop(2)

    val loader = Thread.currentThread().getContextClassLoader
    val cls    = loader.loadClass(mainClass)
    try invokeMain(cls, args0)
    catch {
      case e: InvocationTargetException if e.getCause != null =>
        val printer = StackTracePrinter(
          loader = loader,
          callerClass = Some(getClass.getName),
          cutInvoke = true
        )
        printer.printException(e.getCause, verbosity)
        System.exit(1)
    }
  }

  /** JEP 512 launch protocol: prefer `main(String[])` over `main()`; invoke static methods
    * directly, otherwise instantiate via a non-private zero-arg constructor.
    */
  private[runner] def invokeMain(cls: Class[?], args: Array[String]): Unit = {
    val withArgs = findMain(cls, hasArgs = true)
    val noArgs   = findMain(cls, hasArgs = false)
    val method   = withArgs.orElse(noArgs).getOrElse {
      throw new NoSuchMethodException(
        s"${cls.getName}.main([Ljava.lang.String;) or ${cls.getName}.main()"
      )
    }
    val receiver =
      if Modifier.isStatic(method.getModifiers) then null
      else {
        val ctor = cls.getDeclaredConstructors
          .find(c => c.getParameterCount == 0 && !Modifier.isPrivate(c.getModifiers))
          .getOrElse {
            throw new NoSuchMethodException(
              s"no non-private zero argument constructor found in class ${cls.getName}"
            )
          }
        ctor.setAccessible(true)
        ctor.newInstance()
      }
    method.setAccessible(true)
    if method.getParameterCount == 1 then method.invoke(receiver, args)
    else method.invoke(receiver)
  }

  private def findMain(cls: Class[?], hasArgs: Boolean): Option[Method] = {
    val paramTypes: Seq[Class[?]] =
      if hasArgs then Seq(classOf[Array[String]]) else Seq.empty
    cls.getDeclaredMethods
      .find { m =>
        m.getName == "main" &&
        !Modifier.isPrivate(m.getModifiers) &&
        m.getReturnType == java.lang.Void.TYPE &&
        m.getParameterTypes.toSeq == paramTypes
      }
      .orElse {
        // Also search public methods inherited from superclasses (getMethod does that).
        Try(
          if hasArgs then cls.getMethod("main", classOf[Array[String]])
          else cls.getMethod("main")
        ).toOption.filter(m =>
          !Modifier.isPrivate(m.getModifiers) && m.getReturnType == java.lang.Void.TYPE
        )
      }
  }
}
