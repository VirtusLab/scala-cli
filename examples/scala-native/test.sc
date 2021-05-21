import scala.scalanative.libc._
import scala.scalanative.unsafe._

Zone { implicit z =>
  stdio.printf(toCString("Hello from Scala Native\n"))
}
