package scala.build.errors

import scala.build.Position

case class Diagnostic(
  message: String,
  severity: Severity,
  positions: Seq[Position] = Nil
)

object Diagnostic {
  object Messages {
    val bloopTooOld =
      "JVM that is hosting bloop is older than the requested runtime. Please run `scala-cli bloop exit`, and then use `--jvm` flag to restart Bloop"
  }
}
