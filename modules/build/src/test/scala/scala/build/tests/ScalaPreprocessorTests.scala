package scala.build
package tests

import com.eed3si9n.expecty.Expecty.{assert => expect}
import com.virtuslab.using_directives.custom.model.UsingDirectiveKind

import scala.build.errors.Diagnostic
import scala.build.preprocessing.{ExtractedDirectives, ScopePath}

class ScalaPreprocessorTests extends munit.FunSuite {

  val path = os.pwd

  trait Check {
    def test(diags: Seq[Diagnostic]): Boolean
  }
  case class Warn(messagePattern: String, line: Int, index: Int) extends Check {
    def test(diags: Seq[Diagnostic]): Boolean = {
      val matching = diags.filter(_.positions.exists {
        case Position.File(_, (`line`, `index`), _) => true
        case _                                      => false
      })
      val Regex = s".*$messagePattern.*".r
      matching.map(_.message).exists {
        case Regex() => true
        case _       => false
      }
    }
  }

  case class NoWarn(messagePattern: String) extends Check {
    def test(diags: Seq[Diagnostic]): Boolean = {
      val Regex    = s".*$messagePattern.*".r
      val matching = diags.filter(d => Regex.unapplySeq(d.message).nonEmpty)
      expect(matching.isEmpty)
      matching.isEmpty
    }
  }

  private def testWarnings(lines: String*)(expectedWarnings: Check*): Unit = {
    val persistentLogger = new PersistentDiagnosticLogger(Logger.nop)
    val code             = lines.mkString("\n").toCharArray()
    val res = ExtractedDirectives.from(code, Right(path), persistentLogger, UsingDirectiveKind.values(), ScopePath.fromPath(path))
    expect(res.isRight)

    val diags = persistentLogger.diagnostics
    if (expectedWarnings.isEmpty) expect(diags.isEmpty)
    else expectedWarnings.foreach(warn => expect(warn.test(diags)))
  }

  val commentDirective        = """// using commentDirective "b" """
  val specialCommentDirective = """//> using specialCommentDirective "b" """
  val directive               = """using directive "b" """

  test("Test deprecation warnings about comments") {
    testWarnings(commentDirective)(Warn("deprecated", 0, 0))
    testWarnings(
      commentDirective,
      specialCommentDirective,
      commentDirective
    )(
      Warn("deprecated", 0, 0),
      Warn("deprecated", 2, 0)
    )
  }

  test("Test warnings about mixing syntax") {
    testWarnings(directive, specialCommentDirective)(Warn("ignored", 1, 0))
    testWarnings(directive, commentDirective)(Warn("ignored", 1, 0))
    testWarnings(specialCommentDirective, commentDirective)(Warn("ignored", 1, 0))
    testWarnings(commentDirective, specialCommentDirective)(Warn("deprecated", 0, 0))
  }

  test("Plain comment only result in no ignored warning") {
    testWarnings(commentDirective)(NoWarn("ignored"))
  }

  test("@using is deprecated") {
    def addAt(s: String) = s.replace("using ", "@using ")
    testWarnings(addAt(commentDirective))(Warn("syntax", 0, 3), Warn("keyword", 0, 3))
    testWarnings(addAt(directive))(Warn("syntax", 0, 0), Warn("keyword", 0, 0))
    testWarnings(addAt(specialCommentDirective))(Warn("syntax", 0, 4), Warn("keyword", 0, 4))
  }

}
