package scala.build.preprocessing

import com.virtuslab.using_directives.custom.utils.Position
import com.virtuslab.using_directives.reporter.Reporter

import java.io.PrintStream

class DirectivesOutputStreamReporter(out: PrintStream) extends Reporter {

  private var errorCount = 0

  private def msgWithPos(pos: Position, msg: String): String =
    s"${pos.getLine}:${pos.getColumn}:\n$msg"

  private def errorMessage(msg: String): String = {
    errorCount += 1
    s"ERROR: $msg"
  }
  private def warningMessage(msg: String): String =
    s"WARNING: $msg"

  override def error(msg: String): Unit =
    out.println(errorMessage(msg))
  override def error(position: Position, msg: String): Unit =
    out.println(msgWithPos(position, errorMessage(msg)))
  override def warning(msg: String): Unit =
    out.println(warningMessage(msg))
  override def warning(position: Position, msg: String): Unit =
    out.println(msgWithPos(position, warningMessage(msg)))

  override def hasErrors(): Boolean =
    errorCount != 0

  override def reset(): Unit = {
    errorCount = 0
  }
}
