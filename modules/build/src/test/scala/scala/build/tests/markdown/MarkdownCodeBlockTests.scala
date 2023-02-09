package scala.build.tests.markdown

import com.eed3si9n.expecty.Expecty.expect

import scala.build.Position
import scala.build.errors.{BuildException, MarkdownUnclosedBackticksError}
import scala.build.internal.markdown.MarkdownCodeBlock
import scala.build.preprocessing.MarkdownCodeBlockProcessor
import scala.build.tests.markdown.MarkdownTestUtil.*

class MarkdownCodeBlockTests extends munit.FunSuite {
  test("no code blocks are extracted from markdown if none are present") {
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
    expect(MarkdownCodeBlock.findCodeBlocks(os.sub / "Example.md", markdown) == Right(Seq.empty))
  }

  test("a simple Scala code block is extracted correctly from markdown") {
    val code = """println("Hello")"""
    val markdown =
      s"""# Some snippet
         |
         |```scala
         |$code
         |```
         |""".stripMargin
    val expectedResult =
      MarkdownCodeBlock(
        info = PlainScalaInfo,
        body = code,
        startLine = 3,
        endLine = 3
      )
    val Right(Seq(actualResult)) =
      MarkdownCodeBlock.findCodeBlocks(os.sub / "Example.md", markdown)
    expect(actualResult == expectedResult)
  }

  test("shebang line is ignored in plain scala code blocks") {
    val code = """println("Hello")""".stripMargin
    val markdown =
      s"""# Some snippet
         |
         |```scala
         |#!/usr/bin/env -S scala-cli shebang
         |$code
         |```
         |""".stripMargin
    val expectedResult =
      MarkdownCodeBlock(
        info = PlainScalaInfo,
        body = "\n" + code,
        startLine = 3,
        endLine = 4
      )
    val Right(Seq(actualResult: MarkdownCodeBlock)) =
      MarkdownCodeBlock.findCodeBlocks(os.sub / "Example.md", markdown)
    expect(actualResult == expectedResult)
  }

  test("a raw Scala code block is extracted correctly from markdown") {
    val code = """object Main extends App {
                 |  println("Hello")
                 |}""".stripMargin
    val markdown =
      s"""# Some snippet
         |
         |```scala raw
         |$code
         |```
         |""".stripMargin
    val expectedResult =
      MarkdownCodeBlock(
        info = RawScalaInfo,
        body = code,
        startLine = 3,
        endLine = 5
      )
    val Right(Seq(actualResult)) =
      MarkdownCodeBlock.findCodeBlocks(os.sub / "Example.md", markdown)
    expect(actualResult == expectedResult)
  }

  test("shebang line is ignored in raw scala code blocks") {
    val code =
      """object Main extends App {
        |  println("Hello")
        |}""".stripMargin
    val markdown =
      s"""# Some snippet
         |
         |```scala raw
         |#!/usr/bin/env -S scala-cli shebang
         |$code
         |```
         |""".stripMargin
    val expectedResult =
      MarkdownCodeBlock(
        info = RawScalaInfo,
        body = "\n" + code,
        startLine = 3,
        endLine = 6
      )
    val Right(Seq(actualResult: MarkdownCodeBlock)) =
      MarkdownCodeBlock.findCodeBlocks(os.sub / "Example.md", markdown)
    expect(actualResult == expectedResult)
  }

  test("a test Scala snippet is extracted correctly from markdown") {
    val code =
      """//> using dep "org.scalameta::munit:0.7.29"
        |class Test extends munit.FunSuite {
        |  assert(true)
        |}""".stripMargin
    val markdown =
      s"""# Some snippet
         |
         |```scala test
         |$code
         |```
         |""".stripMargin
    val expectedResult =
      MarkdownCodeBlock(
        info = TestScalaInfo,
        body = code,
        startLine = 3,
        endLine = 6
      )
    val Right(Seq(actualResult)) =
      MarkdownCodeBlock.findCodeBlocks(os.sub / "Example.md", markdown)
    expect(actualResult == expectedResult)
  }

  test("a Scala code block is skipped when it's tagged as `ignore` in markdown") {
    val code = """println("Hello")"""
    val markdown =
      s"""# Some snippet
         |
         |```scala ignore
         |$code
         |```
         |""".stripMargin
    expect(MarkdownCodeBlock.findCodeBlocks(os.sub / "Example.md", markdown) == Right(Seq.empty))
  }

  test("an unclosed code block produces a build error") {
    val code = """println("Hello")"""
    val markdown =
      s"""# Some snippet
         |
         |```scala
         |$code
         |""".stripMargin
    val subPath          = os.sub / "Example.md"
    val expectedPosition = Position.File(Right(os.pwd / subPath), 2 -> 0, 2 -> 3)
    val expectedError    = MarkdownUnclosedBackticksError("```", Seq(expectedPosition))
    val Left(result)     = MarkdownCodeBlock.findCodeBlocks(subPath, markdown)
    expect(result.message == expectedError.message)
    expect(result.positions == expectedError.positions)
  }

  test("recovery from an unclosed code block error works correctly") {
    val code = """println("closed snippet")"""
    val markdown =
      """# Some snippet
        |```scala
        |println("closed snippet")
        |```
        |
        |# Some other snippet
        |
        |````scala
        |println("unclosed snippet")
        |
        |```scala
        |println("whatever")
        |```
        |""".stripMargin
    val subPath                            = os.sub / "Example.md"
    var maybeError: Option[BuildException] = None
    val recoveryFunction = (be: BuildException) => {
      maybeError = Some(be)
      None
    }
    val Right(Seq(actualResult)) =
      MarkdownCodeBlock.findCodeBlocks(subPath, markdown, maybeRecoverOnError = recoveryFunction)
    val expectedResult =
      MarkdownCodeBlock(
        info = PlainScalaInfo,
        body = code,
        startLine = 2,
        endLine = 2
      )
    expect(actualResult == expectedResult)
    val expectedPosition  = Position.File(Right(os.pwd / subPath), 7 -> 0, 7 -> 4)
    val expectedError     = MarkdownUnclosedBackticksError("````", Seq(expectedPosition))
    val Some(actualError) = maybeError
    expect(actualError.positions == expectedError.positions)
    expect(actualError.message == expectedError.message)
  }
}
