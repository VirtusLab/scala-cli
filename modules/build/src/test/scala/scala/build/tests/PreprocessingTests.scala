package scala.build.tests

import scala.build.Inputs
import scala.build.preprocessing.{ScalaPreprocessor, ScriptPreprocessor, MarkdownPreprocessor}
import com.eed3si9n.expecty.Expecty.expect

import scala.build.internal.CustomCodeWrapper

class PreprocessingTests extends munit.FunSuite {

  test("Report error if scala file not exists") {
    val logger    = TestLogger()
    val scalaFile = Inputs.SourceScalaFile(os.temp.dir(), os.SubPath("NotExists.scala"))

    val res = ScalaPreprocessor.preprocess(scalaFile, logger, allowRestrictedFeatures = false)
    val expectedMessage = s"File not found: ${scalaFile.path}"

    assert(res.nonEmpty)
    assert(res.get.isLeft)
    expect(res.get.swap.toOption.get.message == expectedMessage)
  }

  test("Report error if scala script not exists") {
    val logger      = TestLogger()
    val scalaScript = Inputs.Script(os.temp.dir(), os.SubPath("NotExists.sc"))

    val res = ScriptPreprocessor(CustomCodeWrapper).preprocess(
      scalaScript,
      logger,
      allowRestrictedFeatures = false
    )
    val expectedMessage = s"File not found: ${scalaScript.path}"

    assert(res.nonEmpty)
    assert(res.get.isLeft)
    expect(res.get.swap.toOption.get.message == expectedMessage)
  }

  test("Report error if markdown does not exist") {
    val logger       = TestLogger()
    val markdownFile = Inputs.MarkdownFile(os.temp.dir(), os.SubPath("NotExists.md"))

    val res = MarkdownPreprocessor.preprocess(markdownFile, logger, allowRestrictedFeatures = false)
    val expectedMessage = s"File not found: ${markdownFile.path}"

    assert(res.nonEmpty)
    assert(res.get.isLeft)
    expect(res.get.swap.toOption.get.message == expectedMessage)
  }
}
