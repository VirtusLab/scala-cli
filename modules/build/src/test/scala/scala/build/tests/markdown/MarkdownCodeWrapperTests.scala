package scala.build.tests.markdown

import scala.build.internal.markdown.{MarkdownCodeBlock, MarkdownCodeWrapper}
import com.eed3si9n.expecty.Expecty.expect
import os.RelPath

import scala.build.Position
import scala.build.errors.{BuildException, MarkdownUnclosedBackticksError}
import scala.build.internal.AmmUtil
import scala.build.preprocessing.directives.StrictDirective
import scala.build.preprocessing.{
  ExtractedDirectives,
  PreprocessedMarkdown,
  PreprocessedMarkdownCodeBlocks
}

class MarkdownCodeWrapperTests extends munit.FunSuite {

  test("empty markdown produces no wrapped code") {
    val result = MarkdownCodeWrapper(os.sub / "Example.md", PreprocessedMarkdown.empty)
    expect(result == (None, None, None))
  }

  test("a simple Scala code block is wrapped correctly") {
    val snippet                = """println("Hello")"""
    val codeBlock              = MarkdownCodeBlock(Seq("scala"), snippet, 3, 3)
    val preprocessedCodeBlocks = PreprocessedMarkdownCodeBlocks(Seq(codeBlock))
    val markdown               = PreprocessedMarkdown(scriptCodeBlocks = preprocessedCodeBlocks)
    val expectedScala = MarkdownCodeWrapper.WrappedMarkdownCode(
      """object Example_md { @annotation.nowarn("msg=pure expression does nothing") def main(args: Array[String]): Unit = { Scope; }
        |
        |object Scope {
        |println("Hello")
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
    val codeBlock              = MarkdownCodeBlock(Seq("scala", "raw"), snippet, 3, 5)
    val preprocessedCodeBlocks = PreprocessedMarkdownCodeBlocks(Seq(codeBlock))
    val markdown               = PreprocessedMarkdown(rawCodeBlocks = preprocessedCodeBlocks)
    val expectedScala = MarkdownCodeWrapper.WrappedMarkdownCode(
      """
        |
        |
        |object Main extends App {
        |  println("Hello")
        |}
        |""".stripMargin
    )
    val result = MarkdownCodeWrapper(os.sub / "Example.md", markdown)
    expect(result == (None, Some(expectedScala), None))
  }

  test("a test Scala snippet is wrapped correctly") {
    val snippet =
      """//> using lib "org.scalameta::munit:0.7.29"
        |class Test extends munit.FunSuite {
        |  assert(true)
        |}""".stripMargin
    val codeBlock              = MarkdownCodeBlock(Seq("scala", "test"), snippet, 3, 6)
    val preprocessedCodeBlocks = PreprocessedMarkdownCodeBlocks(Seq(codeBlock))
    val markdown               = PreprocessedMarkdown(testCodeBlocks = preprocessedCodeBlocks)
    val expectedScala = MarkdownCodeWrapper.WrappedMarkdownCode(
      """
        |
        |
        |//> using lib "org.scalameta::munit:0.7.29"
        |class Test extends munit.FunSuite {
        |  assert(true)
        |}
        |""".stripMargin
    )
    val result = MarkdownCodeWrapper(os.sub / "Example.md", markdown)
    expect(result == (None, None, Some(expectedScala)))
  }
}
