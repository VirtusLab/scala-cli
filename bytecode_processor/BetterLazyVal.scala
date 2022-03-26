package runtime

import java.lang.reflect.Field

/**
 * Helper methods used in thread-safe lazy vals.
 */
object BetterLazyVal {
  private val debug = false

  private[this] val unsafe: sun.misc.Unsafe =
      classOf[sun.misc.Unsafe].getDeclaredFields.nn.find { field =>
        field.nn.getType == classOf[sun.misc.Unsafe] && {
          field.nn.setAccessible(true)
          true
        }
      }
      .map(_.nn.get(null).asInstanceOf[sun.misc.Unsafe])
      .getOrElse {
        throw new ExceptionInInitializerError {
          new IllegalStateException("Can't find instance of sun.misc.Unsafe")
        }
      }

  def getOffset(unused: Any, field: Field): Long = {
    val r = unsafe.objectFieldOffset(field)
    if (debug)
      println(s"getOffset(${field.getDeclaringClass}}, ${field.getName}) = $r")
    r
  }
}