package scala.build.tests.markdown

import scala.build.internal.markdown.{MarkdownCodeBlock, MarkdownCodeWrapper}
import com.eed3si9n.expecty.Expecty.expect

import scala.build.Position
import scala.build.errors.{BuildException, MarkdownUnclosedBackticksError}
import scala.build.internal.AmmUtil
import scala.build.preprocessing.directives.StrictDirective
import scala.build.preprocessing.{
  ExtractedDirectives,
  PreprocessedMarkdown,
  PreprocessedMarkdownCodeBlocks
}
import scala.build.tests.markdown.MarkdownTestUtil.*

class MarkdownCodeWrapperTests extends munit.FunSuite {

  test("empty markdown produces no wrapped code") {
    val result = MarkdownCodeWrapper(os.sub / "Example.md", PreprocessedMarkdown.empty)
    expect(result == (None, None, None))
  }

  test("a simple Scala code block is wrapped correctly") {
    val snippet                = """println("Hello")"""
    val codeBlock              = MarkdownCodeBlock(PlainScalaInfo, snippet, 3, 3)
    val preprocessedCodeBlocks = PreprocessedMarkdownCodeBlocks(Seq(codeBlock))
    val markdown               = PreprocessedMarkdown(scriptCodeBlocks = preprocessedCodeBlocks)
    val expectedScala = MarkdownCodeWrapper.WrappedMarkdownCode(
      s"""object Example_md { @annotation.nowarn("msg=pure expression does nothing") def main(args: Array[String]): Unit = { Scope; }
         |
         |object Scope {
         |$snippet
         |}}""".stripMargin
    )
    val result = MarkdownCodeWrapper(os.sub / "Example.md", markdown)
    expect(result == (Some(expectedScala), None, None))
  }

  test("multiple plain Scala code blocks are wrapped correctly") {
    val snippet1   = """println("Hello")"""
    val codeBlock1 = MarkdownCodeBlock(PlainScalaInfo, snippet1, 3, 3)
    val snippet2   = """println("world")"""
    val codeBlock2 = MarkdownCodeBlock(PlainScalaInfo, snippet2, 8, 8)
    val snippet3   = """println("!")"""
    val codeBlock3 = MarkdownCodeBlock(PlainScalaInfo, snippet3, 12, 12)
    val preprocessedCodeBlocks =
      PreprocessedMarkdownCodeBlocks(Seq(codeBlock1, codeBlock2, codeBlock3))
    val markdown = PreprocessedMarkdown(scriptCodeBlocks = preprocessedCodeBlocks)
    val expectedScala = MarkdownCodeWrapper.WrappedMarkdownCode(
      s"""object Example_md { @annotation.nowarn("msg=pure expression does nothing") def main(args: Array[String]): Unit = { Scope; }
         |
         |object Scope {
         |$snippet1
         |
         |
         |
         |
         |$snippet2
         |
         |
         |
         |$snippet3
         |}}""".stripMargin
    )
    val result = MarkdownCodeWrapper(os.sub / "Example.md", markdown)
    expect(result == (Some(expectedScala), None, None))
  }

  test("multiple plain Scala code blocks with different scopes are wrapped correctly") {
    val snippet1   = """println("Hello")"""
    val codeBlock1 = MarkdownCodeBlock(PlainScalaInfo, snippet1, 3, 3)
    val snippet2   = """println("world")"""
    val codeBlock2 = MarkdownCodeBlock(ResetScalaInfo, snippet2, 8, 8)
    val snippet3   = """println("!")"""
    val codeBlock3 = MarkdownCodeBlock(PlainScalaInfo, snippet3, 12, 12)
    val preprocessedCodeBlocks =
      PreprocessedMarkdownCodeBlocks(Seq(codeBlock1, codeBlock2, codeBlock3))
    val markdown = PreprocessedMarkdown(scriptCodeBlocks = preprocessedCodeBlocks)
    val expectedScala = MarkdownCodeWrapper.WrappedMarkdownCode(
      s"""object Example_md { @annotation.nowarn("msg=pure expression does nothing") def main(args: Array[String]): Unit = { Scope; Scope1; }
         |
         |object Scope {
         |$snippet1
         |
         |
         |
         |}; object Scope1 {
         |$snippet2
         |
         |
         |
         |$snippet3
         |}}""".stripMargin
    )
    val result = MarkdownCodeWrapper(os.sub / "Example.md", markdown)
    expect(result == (Some(expectedScala), None, None))
  }

  test("a raw Scala code block is wrapped correctly") {
    val snippet =
      """object Main extends App {
        |  println("Hello")
        |}""".stripMargin
    val codeBlock              = MarkdownCodeBlock(RawScalaInfo, snippet, 3, 5)
    val preprocessedCodeBlocks = PreprocessedMarkdownCodeBlocks(Seq(codeBlock))
    val markdown               = PreprocessedMarkdown(rawCodeBlocks = preprocessedCodeBlocks)
    val expectedScala = MarkdownCodeWrapper.WrappedMarkdownCode(
      s"""
         |
         |
         |$snippet
         |""".stripMargin
    )
    val result = MarkdownCodeWrapper(os.sub / "Example.md", markdown)
    expect(result == (None, Some(expectedScala), None))
  }

  test("multiple raw Scala code blocks are glued together correctly") {
    val snippet1 =
      """case class Message(value: String)""".stripMargin
    val codeBlock1 = MarkdownCodeBlock(RawScalaInfo, snippet1, 3, 3)
    val snippet2 =
      """object Main extends App {
        |  println(Message("Hello").value)
        |}""".stripMargin
    val codeBlock2             = MarkdownCodeBlock(RawScalaInfo, snippet2, 5, 7)
    val preprocessedCodeBlocks = PreprocessedMarkdownCodeBlocks(Seq(codeBlock1, codeBlock2))
    val markdown               = PreprocessedMarkdown(rawCodeBlocks = preprocessedCodeBlocks)
    val expectedScala = MarkdownCodeWrapper.WrappedMarkdownCode(
      s"""
         |
         |
         |$snippet1
         |
         |$snippet2
         |""".stripMargin
    )
    val result = MarkdownCodeWrapper(os.sub / "Example.md", markdown)
    expect(result == (None, Some(expectedScala), None))
  }

  test("a test Scala snippet is wrapped correctly") {
    val snippet =
      """//> using dep "org.scalameta::munit:0.7.29"
        |class Test extends munit.FunSuite {
        |  assert(true)
        |}""".stripMargin
    val codeBlock              = MarkdownCodeBlock(TestScalaInfo, snippet, 3, 6)
    val preprocessedCodeBlocks = PreprocessedMarkdownCodeBlocks(Seq(codeBlock))
    val markdown               = PreprocessedMarkdown(testCodeBlocks = preprocessedCodeBlocks)
    val expectedScala = MarkdownCodeWrapper.WrappedMarkdownCode(
      s"""
         |
         |
         |$snippet
         |""".stripMargin
    )
    val result = MarkdownCodeWrapper(os.sub / "Example.md", markdown)
    expect(result == (None, None, Some(expectedScala)))
  }

  test("multiple test Scala snippets are glued together correctly") {
    val snippet1 =
      """//> using dep "org.scalameta::munit:0.7.29"
        |class Test1 extends munit.FunSuite {
        |  assert(true)
        |}""".stripMargin
    val codeBlock1 = MarkdownCodeBlock(TestScalaInfo, snippet1, 3, 6)
    val snippet2 =
      """class Test2 extends munit.FunSuite {
        |  assert(true)
        |}""".stripMargin
    val codeBlock2             = MarkdownCodeBlock(TestScalaInfo, snippet2, 8, 10)
    val preprocessedCodeBlocks = PreprocessedMarkdownCodeBlocks(Seq(codeBlock1, codeBlock2))
    val markdown               = PreprocessedMarkdown(testCodeBlocks = preprocessedCodeBlocks)
    val expectedScala = MarkdownCodeWrapper.WrappedMarkdownCode(
      s"""
         |
         |
         |$snippet1
         |
         |$snippet2
         |""".stripMargin
    )
    val result = MarkdownCodeWrapper(os.sub / "Example.md", markdown)
    expect(result == (None, None, Some(expectedScala)))
  }
}
