package scala.build.errors

import ch.epfl.scala.bsp4j as b
import ch.epfl.scala.bsp4j.DiagnosticSeverity
sealed abstract class Severity extends Product with Serializable {
  def toBsp4j: b.DiagnosticSeverity
}

object Severity {
  case object Error extends Severity {
    override def toBsp4j: DiagnosticSeverity = b.DiagnosticSeverity.ERROR
  }
  case object Warning extends Severity {
    override def toBsp4j: DiagnosticSeverity = b.DiagnosticSeverity.WARNING
  }
  case object Hint extends Severity {
    override def toBsp4j: DiagnosticSeverity = b.DiagnosticSeverity.HINT
  }
}
