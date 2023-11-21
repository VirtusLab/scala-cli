package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.build.input.{ScalaCliInvokeData, Script, SourceScalaFile}
import scala.build.options.SuppressWarningOptions
import scala.build.preprocessing.{PreprocessedSource, ScalaPreprocessor, ScriptPreprocessor}

class ScalaPreprocessorTests extends TestUtil.ScalaCliBuildSuite {

  test("should respect using directives in a .scala file with the shebang line") {
    val lastUsingLine =
      "//> using dep \"com.lihaoyi::os-lib::0.8.1\" \"com.lihaoyi::os-lib::0.8.1\""
    TestInputs(os.rel / "Main.scala" ->
      s"""#!/usr/bin/env -S scala-cli shebang
         |//> using jvm 11
         |$lastUsingLine
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
        allowRestrictedFeatures = true,
        suppressWarningOptions = SuppressWarningOptions()
      )(using ScalaCliInvokeData.dummy)
      expect(result.nonEmpty)
      val Some(directivesPositions) = result.head.directivesPositions
      expect(directivesPositions.startPos == 0 -> 0)
      expect(directivesPositions.endPos == 3   -> lastUsingLine.length)
    }
  }

  test("should respect using directives in a .sc file with the shebang line") {
    val depLine = "//> using dep com.lihaoyi::os-lib::0.8.1"

    TestInputs(os.rel / "sample.sc" ->
      s"""#!/usr/bin/env -S scala-cli shebang
         |$depLine
         |println(os.pwd)
         |""".stripMargin).fromRoot { root =>
      val scalaFile = Script(root, os.sub / "sample.sc", None)
      val Some(Right(result)) = ScriptPreprocessor.preprocess(
        scalaFile,
        logger = TestLogger(),
        allowRestrictedFeatures = false,
        suppressWarningOptions = SuppressWarningOptions()
      )(using ScalaCliInvokeData.dummy)
      expect(result.nonEmpty)
      val Some(directivesPositions) = result.head.directivesPositions
      expect(directivesPositions.startPos == 0 -> 0)
      expect(directivesPositions.endPos == 2   -> depLine.length)
    }
  }

  val lastUsingLines = Seq(
    "//> using dep \"com.lihaoyi::os-lib::0.8.1\" \"com.lihaoyi::os-lib::0.8.1\"" -> "string literal",
    "//> using scala 2.13.7"       -> "numerical string",
    "//> using objectWrapper true" -> "boolean literal",
    "//> using objectWrapper"      -> "empty value literal"
  )

  for ((lastUsingLine, typeName) <- lastUsingLines) do
    test(s"correct directive positions with $typeName") {
      TestInputs(os.rel / "Main.scala" ->
        s"""#!/usr/bin/env -S scala-cli shebang
           |//> using jvm 11
           |$lastUsingLine
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
          allowRestrictedFeatures = true,
          suppressWarningOptions = SuppressWarningOptions()
        )(using ScalaCliInvokeData.dummy)
        expect(result.nonEmpty)
        val Some(directivesPositions) = result.head.directivesPositions
        expect(directivesPositions.startPos == 0 -> 0)
        expect(directivesPositions.endPos == 3   -> lastUsingLine.length)
      }
    }
}
