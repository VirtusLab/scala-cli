package scala.build.errors

sealed trait Severity

object Severity {
  object ERROR   extends Severity
  object WARNING extends Severity
}
