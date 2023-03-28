import scala.scalanative.libc._
import scala.scalanative.unsafe._

Zone { implicit z =>
  val io = StdioHelpers(stdio)
  io.printf(c"%s\n", c"Hello from Scala Native")
}