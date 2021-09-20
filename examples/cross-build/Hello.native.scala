// using scala 2.13
// require scala-native

import scala.scalanative.libc._
import scala.scalanative.unsafe._

object Test {
  def main(args: Array[String]): Unit = {
    val message = "Hello from Scala Native\n"
    Zone { implicit z =>
      stdio.printf(toCString(message))
    }
  }
}
