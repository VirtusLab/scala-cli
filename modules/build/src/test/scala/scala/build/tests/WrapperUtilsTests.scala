package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.build.internal.WrapperUtils
import scala.build.internal.WrapperUtils.ScriptMainMethod
import scala.meta.*

class WrapperUtilsTests extends TestUtil.ScalaCliBuildSuite {

  private val scriptWithTupleComparison =
    """|object MainNeverCalled {
       |  def main(args: Array[String]): Unit = {
       |    printf("yo!\n")
       |    val pair = ("", "")
       |    val (a, b) = pair
       |    if (a, b) == pair then println("yo")
       |  }
       |}
       |""".stripMargin

  test("detect main method in script with tuple comparison in if condition") {
    WrapperUtils.mainObjectInScript("3.8.3", scriptWithTupleComparison) match
      case ScriptMainMethod.Exists(name) => expect(name == "MainNeverCalled")
      case other                         => fail(s"Expected main method to be detected, got $other")
  }

  test("parse tuple comparison in if condition") {
    val scriptDialect =
      dialects.Scala3Future.withAllowToplevelStatements(true).withAllowToplevelTerms(true)
    given Dialect = scriptDialect
    val code      = "if (a, b) == pair then ???"
    code.parse[Stat] match
      case Parsed.Success(_) => ()
      case other             => fail(s"Expected successful parse, got: $other")
  }

  test("parse script with tuple comparison without spurious toplevel terms") {
    val scriptDialect =
      dialects.Scala3Future.withAllowToplevelStatements(true).withAllowToplevelTerms(true)
    given Dialect = scriptDialect
    val stats     = scriptWithTupleComparison.parse[Source] match
      case Parsed.Success(Source(parsedStats)) => parsedStats
      case other                               => fail(s"Failed to parse script: $other")

    val toplevelTerms = stats.collect { case t: Term => t }
    assert(
      toplevelTerms.isEmpty,
      clue(s"Expected no spurious toplevel terms, got: ${toplevelTerms.mkString(", ")}")
    )
  }

}
