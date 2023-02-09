package scala.build
package tests

import com.eed3si9n.expecty.Expecty.{assert => expect}
import com.virtuslab.using_directives.custom.model.UsingDirectiveKind

import scala.build.errors.Diagnostic
import scala.build.preprocessing.{ExtractedDirectives, ScopePath}
import scala.build.errors.Severity
import scala.build.errors.CompositeBuildException
import scala.build.errors.BuildException

class DirectiveParsingTest extends munit.FunSuite {

  val path = os.pwd

  trait Check {
    def test(diags: Seq[Diagnostic]): Boolean
  }

  case class Warn(messagePattern: String, line: Int, index: Int) extends Check {
    def test(diags: Seq[Diagnostic]): Boolean = {
      val matching = diags.filter(_.positions.exists {
        case Position.File(_, (`line`, `index`), _) => true
        case _                                      => false
      }).filter(_.severity == Severity.Warning)
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

  case class Error(messagePattern: String) extends Check {
    def test(diags: Seq[Diagnostic]): Boolean =
      testMsgs(diags.filter(_.severity == Severity.Error).map(_.message))

    def testMsgs(diags: Seq[String]): Boolean = {
      val Regex = s".*$messagePattern.*".r
      println("Errors: " + diags)
      val matching = diags.filter(d => Regex.unapplySeq(d).nonEmpty)
      expect(matching.nonEmpty)
      matching.nonEmpty
    }
  }

  private def testDiagnostics(lines: String*)(checks: Check*): List[Diagnostic] = {
    val persistentLogger = new PersistentDiagnosticLogger(Logger.nop)
    val code             = lines.mkString("\n").toCharArray()
    val res = ExtractedDirectives.from(
      code,
      Right(path),
      persistentLogger,
      UsingDirectiveKind.values(),
      ScopePath.fromPath(path),
      maybeRecoverOnError = e => Some(e)
    )

    def checkDiag(checks: Seq[Check]) = {
      val diags = persistentLogger.diagnostics
      if (checks.isEmpty) expect(diags.isEmpty)
      else checks.foreach(warn => expect(warn.test(diags)))
      diags
    }

    val (expectedError, expectedWarnings) =
      checks.partitionMap { case e: Error => Left(e); case o => Right(o) }
    if (expectedError.isEmpty) expect(res.isRight)
    else
      res match {
        case Left(exception) =>
          def flatten(e: BuildException): Seq[String] = e match {
            case c: CompositeBuildException =>
              c.exceptions.flatMap(flatten)
            case e =>
              Seq(e.getMessage())
          }
          val msgs = flatten(exception)
          expectedError.foreach(e => expect(e.testMsgs(msgs)))
        case _ =>
          checkDiag(expectedError)
      }
    checkDiag(expectedWarnings)

  }

  val commentDirective        = """// using commentDirective "b" """
  val specialCommentDirective = """//> using specialCommentDirective "b" """
  val directive               = """using directive "b" """

  test("Test deprecation warnings about comments") {
    testDiagnostics(commentDirective)(Warn("deprecated", 0, 0))
    testDiagnostics(
      commentDirective,
      specialCommentDirective,
      commentDirective
    )(
      Warn("deprecated", 0, 0),
      Warn("deprecated", 2, 0)
    )
  }

  test("Test warnings about mixing syntax") {
    testDiagnostics(directive, specialCommentDirective)(Warn("ignored", 1, 0))
    testDiagnostics(directive, commentDirective)(Warn("ignored", 1, 0))
    testDiagnostics(specialCommentDirective, commentDirective)(Warn("ignored", 1, 0))
    testDiagnostics(commentDirective, specialCommentDirective)(Warn("deprecated", 0, 0))
  }

  test("Plain comment only result in no ignored warning") {
    testDiagnostics(commentDirective)(NoWarn("ignored"))
  }

  test("@using is deprecated") {
    def addAt(s: String) = s.replace("using ", "@using ")
    testDiagnostics(addAt(commentDirective))(Warn("syntax", 0, 3), Warn("keyword", 0, 3))
    testDiagnostics(addAt(directive))(Warn("syntax", 0, 0), Warn("keyword", 0, 0))
    testDiagnostics(addAt(specialCommentDirective))(Warn("syntax", 0, 4), Warn("keyword", 0, 4))
  }

  test("interpolator in dependency") {
    val diags =
      testDiagnostics("""//> using dep ivy"org.scala-sbt::io:1.6.0"""")(Error("interpolator"))
    println(diags)
  }

  test("unicode in using directives") {
    val diags = testDiagnostics("""using nativeMode “release-full”"""")(Error("illegal"))
    println(diags)
  }
}
