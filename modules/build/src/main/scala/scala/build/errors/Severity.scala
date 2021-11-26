package scala.build.errors

sealed trait Severity

object Severity {
  case object Error   extends Severity
  case object Warning extends Severity
}
