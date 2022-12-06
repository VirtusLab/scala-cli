package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.build.input.SourceScalaFile
import scala.build.preprocessing.{PreprocessedSource, ScalaPreprocessor}

class ScalaPreprocessorTests extends munit.FunSuite {

  test("should respect using directives in a .scala file with the shebang line") {
    TestInputs(os.rel / "Main.scala" ->
      """#!/usr/bin/env -S scala-cli shebang
        |//> using lib "com.lihaoyi::os-lib::0.8.1"
        |
        |object Main {
        |  def main(args: Array[String]): Unit = {
        |    println(os.pwd)
        |  }
        |}""".stripMargin).fromRoot { root =>
      val scalaFile = SourceScalaFile(root, os.sub / "Main.scala")
      val Some(Right(result)) = ScalaPreprocessor.preprocess(
        scalaFile,
        logger = TestLogger(),
        allowRestrictedFeatures = false
      )
      expect(result.nonEmpty)
      val Some(directivesPositions) = result.head.directivesPositions
      expect(directivesPositions.specialCommentDirectives.startPos == 0 -> 0)
      expect(directivesPositions.specialCommentDirectives.endPos == 3   -> 0)
    }
  }

  test("should respect using directives in a .sc file with the shebang line") {
    TestInputs(os.rel / "sample.sc" ->
      """#!/usr/bin/env -S scala-cli shebang
        |//> using lib "com.lihaoyi::os-lib::0.8.1"
        |println(os.pwd)
        |""".stripMargin).fromRoot { root =>
      val scalaFile = SourceScalaFile(root, os.sub / "sample.sc")
      val Some(Right(result)) = ScalaPreprocessor.preprocess(
        scalaFile,
        logger = TestLogger(),
        allowRestrictedFeatures = false
      )
      expect(result.nonEmpty)
      val Some(directivesPositions) = result.head.directivesPositions
      expect(directivesPositions.specialCommentDirectives.startPos == 0 -> 0)
      expect(directivesPositions.specialCommentDirectives.endPos == 2   -> 0)
    }
  }
}
