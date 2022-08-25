package scala.build.tests.markdown

import scala.build.internal.markdown.MarkdownCodeWrapper
import com.eed3si9n.expecty.Expecty.expect
import os.RelPath

import scala.build.internal.AmmUtil

class MarkdownCodeWrapperTests extends munit.FunSuite {

  test("no snippets are extracted from markdown if none are present") {
    val markdown: String =
      """
        |# Heading
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit,
        |
        |## Subheading
        |sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
        |Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
        |Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.
        |Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
        |""".stripMargin
    expect(MarkdownCodeWrapper(os.sub / "Example.md", markdown) == (None, None, None))
  }

  test("a simple Scala snippet is correctly extracted from markdown") {
    val markdown =
      """# Some snippet
        |
        |```scala
        |println("Hello")
        |```
        |""".stripMargin
    val expectedScala =
      """object Example_md { @annotation.nowarn("msg=pure expression does nothing") def main(args: Array[String]): Unit = { Scope; }
        |
        |object Scope {
        |println("Hello")
        |}}""".stripMargin
    val result = MarkdownCodeWrapper(os.sub / "Example.md", markdown)
    expect(result == (Some(expectedScala), None, None))
  }

  test("a raw Scala snippet is correctly extracted from markdown") {
    val markdown =
      """# Some snippet
        |
        |```scala raw
        |object Main extends App {
        |  println("Hello")
        |}
        |```
        |""".stripMargin
    val expectedScala =
      """
        |
        |
        |object Main extends App {
        |  println("Hello")
        |}
        |""".stripMargin
    val result = MarkdownCodeWrapper(os.sub / "Example.md", markdown)
    expect(result == (None, Some(expectedScala), None))
  }

  test("a test Scala snippet is correctly extracted from markdown") {
    val markdown =
      """# Some snippet
        |
        |```scala test
        |//> using lib "org.scalameta::munit:0.7.29"
        |class Test extends munit.FunSuite {
        |  assert(true)
        |}
        |```
        |""".stripMargin
    val expectedScala =
      """
        |
        |
        |//> using lib "org.scalameta::munit:0.7.29"
        |class Test extends munit.FunSuite {
        |  assert(true)
        |}
        |""".stripMargin
    val result = MarkdownCodeWrapper(os.sub / "Example.md", markdown)
    expect(result == (None, None, Some(expectedScala)))
  }

  test("a Scala snippet is skipped when it's marked as `ignore` in markdown") {
    val markdown =
      """# Some snippet
        |
        |```scala ignore
        |println("Hello")
        |```
        |""".stripMargin
    expect(MarkdownCodeWrapper(os.sub / "Example.md", markdown) == (None, None, None))
  }
}
