package scala.build.errors

import ch.epfl.scala.bsp4j as b

sealed abstract class Severity extends Product with Serializable {
  def toBsp4j: b.DiagnosticSeverity
}

object Severity {
  case object Error extends Severity {
    override def toBsp4j: b.DiagnosticSeverity = b.DiagnosticSeverity.ERROR
  }
  case object Warning extends Severity {
    override def toBsp4j: b.DiagnosticSeverity = b.DiagnosticSeverity.WARNING
  }
  case object Hint extends Severity {
    override def toBsp4j: b.DiagnosticSeverity = b.DiagnosticSeverity.HINT
  }
}
