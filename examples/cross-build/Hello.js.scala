//> using scala 2.13
//> using target scala-js

import scala.scalajs.js

object Test {
  def main(args: Array[String]): Unit = {
    val console = js.Dynamic.global.console
    console.log("Hello from Scala.JS")
  }
}
