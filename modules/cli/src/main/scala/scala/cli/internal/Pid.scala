package scala.cli.internal

import java.lang.management.ManagementFactory

class Pid {
  def get(): Integer =
    try {
      val pid = ManagementFactory.getRuntimeMXBean.getName.takeWhile(_ != '@').toInt
      pid: Integer
    }
    catch {
      case _: NumberFormatException => null
    }
}
