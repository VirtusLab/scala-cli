package scala.build.errors
import scala.build.Position

class MarkdownUnclosedBackticksError(
  backticks: String,
  positions: Seq[Position]
) extends BuildException(s"Unclosed $backticks code block in a Markdown input", positions)

object MarkdownUnclosedBackticksError {
  def apply(backticks: String, positions: Seq[Position]) =
    new MarkdownUnclosedBackticksError(backticks, positions)
}
