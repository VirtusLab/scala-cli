package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.build.Position
import scala.build.preprocessing.TemporaryDirectivesParser
import scala.build.preprocessing.directives.Directive

class TemporaryDirectivesParserTests extends munit.FunSuite {

  test("spaces") {
    val res = TemporaryDirectivesParser.parseDirectives(
      Left(""),
      """//   using foo
        |// using  a
        |""".stripMargin
    ).map(_._1)
    val expectedRes = Some(
      Seq(
        Directive(
          Directive.Using,
          Seq("foo"),
          None,
          isComment = true,
          Position.File(Left(""), (0, 5), (0, 14))
        ),
        Directive(
          Directive.Using,
          Seq("a"),
          None,
          isComment = true,
          Position.File(Left(""), (1, 3), (1, 11))
        )
      )
    )
    expect(res == expectedRes)
  }

  test("no spaces after //") {
    val res = TemporaryDirectivesParser.parseDirectives(
      Left(""),
      """//using foo
        |//require  foo
        |""".stripMargin
    ).map(_._1)
    val expectedRes = Some(
      Seq(
        Directive(
          Directive.Using,
          Seq("foo"),
          None,
          isComment = true,
          Position.File(Left(""), (0, 2), (0, 11))
        ),
        Directive(
          Directive.Require,
          Seq("foo"),
          None,
          isComment = true,
          Position.File(Left(""), (1, 2), (1, 14))
        )
      )
    )
    expect(res == expectedRes)
  }

  test("using target as require") {
    val res = TemporaryDirectivesParser.parseDirectives(
      Left(""),
      """require foo
        |using target aa bb
        |""".stripMargin
    ).map(_._1)
    val expectedRes = Some(
      Seq(
        Directive(
          Directive.Require,
          Seq("foo"),
          None,
          isComment = false,
          Position.File(Left(""), (0, 0), (0, 11))
        ),
        Directive(
          Directive.Require,
          Seq("aa", "bb"),
          None,
          isComment = false,
          Position.File(Left(""), (1, 0), (1, 18))
        )
      )
    )
    expect(res == expectedRes)
  }

  test("ignore comment") {
    val res = TemporaryDirectivesParser.parseDirectives(
      Left(""),
      """// not require foo
        |require foo
        |""".stripMargin
    ).map(_._1)
    val expectedRes = Some(
      Seq(
        Directive(
          Directive.Require,
          Seq("foo"),
          None,
          isComment = false,
          Position.File(Left(""), (1, 0), (1, 11))
        )
      )
    )
    expect(res == expectedRes)
  }

  test("ignore empty line") {
    val res = TemporaryDirectivesParser.parseDirectives(
      Left(""),
      """
        |require foo
        |""".stripMargin
    ).map(_._1)
    val expectedRes = Some(
      Seq(
        Directive(
          Directive.Require,
          Seq("foo"),
          None,
          isComment = false,
          Position.File(Left(""), (1, 0), (1, 11))
        )
      )
    )
    expect(res == expectedRes)
  }

  test("ignore empty lines and comments") {
    val res = TemporaryDirectivesParser.parseDirectives(
      Left(""),
      """
        | // aa
        |
        |require foo
        |""".stripMargin
    ).map(_._1)
    val expectedRes = Some(
      Seq(
        Directive(
          Directive.Require,
          Seq("foo"),
          None,
          isComment = false,
          Position.File(Left(""), (3, 0), (3, 11))
        )
      )
    )
    expect(res == expectedRes)
  }

}
