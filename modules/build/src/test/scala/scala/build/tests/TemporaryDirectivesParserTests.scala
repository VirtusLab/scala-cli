package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.build.preprocessing.TemporaryDirectivesParser
import scala.build.preprocessing.directives.Directive

class TemporaryDirectivesParserTests extends munit.FunSuite {

  test("spaces") {
    val res = TemporaryDirectivesParser.parseDirectives(
      """//   using foo
        |// using  a
        |""".stripMargin
    ).map(_._1)
    val expectedRes = Some(
      Seq(
        Directive(Directive.Using, Seq("foo"), None, isComment = true),
        Directive(Directive.Using, Seq("a"), None, isComment = true)
      )
    )
    expect(res == expectedRes)
  }

  test("using target as require") {
    val res = TemporaryDirectivesParser.parseDirectives(
      """require foo
        |using target aa bb
        |""".stripMargin
    ).map(_._1)
    val expectedRes = Some(
      Seq(
        Directive(Directive.Require, Seq("foo"), None, isComment = false),
        Directive(Directive.Require, Seq("aa", "bb"), None, isComment = false)
      )
    )
    expect(res == expectedRes)
  }

}
