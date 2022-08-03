package scala.build.errors

import scala.build.Position

trait Diagnostic {
  def message: String
  def severity: Severity
  def positions: Seq[Position]
  def hint: Option[String] = None
}

object Diagnostic {
  object Messages {
    val bloopTooOld =
      "JVM that is hosting bloop is older than the requested runtime. Please run `scala-cli bloop exit`, and then use `--jvm` flag to restart Bloop"
  }

  private case class ADiagnostic(
    message: String,
    severity: Severity,
    positions: Seq[Position],
    override val hint: Option[String]
  ) extends Diagnostic

  def apply(
    message: String,
    severity: Severity,
    positions: Seq[Position] = Nil,
    hint: Option[String] = None
  ): Diagnostic = ADiagnostic(message, severity, positions, hint)
}
