package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.build.input.{MarkdownFile, ScalaCliInvokeData, Script, SourceScalaFile}
import scala.build.options.SuppressWarningOptions
import scala.build.preprocessing.{MarkdownPreprocessor, ScalaPreprocessor, ScriptPreprocessor}

class PreprocessingTests extends TestUtil.ScalaCliBuildSuite {
  test("Report error if scala file not exists") {
    val logger    = TestLogger()
    val scalaFile = SourceScalaFile(os.temp.dir(), os.SubPath("NotExists.scala"))

    val res = ScalaPreprocessor.preprocess(
      scalaFile,
      logger,
      allowRestrictedFeatures = false,
      suppressWarningOptions = SuppressWarningOptions()
    )(using ScalaCliInvokeData.dummy)
    val expectedMessage = s"File not found: ${scalaFile.path}"

    assert(res.nonEmpty)
    assert(res.get.isLeft)
    expect(res.get.swap.toOption.get.message == expectedMessage)
  }

  test("Report error if scala script not exists") {
    val logger      = TestLogger()
    val scalaScript = Script(os.temp.dir(), os.SubPath("NotExists.sc"), None)

    val res = ScriptPreprocessor.preprocess(
      scalaScript,
      logger,
      allowRestrictedFeatures = false,
      suppressWarningOptions = SuppressWarningOptions()
    )(using ScalaCliInvokeData.dummy)
    val expectedMessage = s"File not found: ${scalaScript.path}"

    assert(res.nonEmpty)
    assert(res.get.isLeft)
    expect(res.get.swap.toOption.get.message == expectedMessage)
  }

  test("Report error if markdown does not exist") {
    val logger       = TestLogger()
    val markdownFile = MarkdownFile(os.temp.dir(), os.SubPath("NotExists.md"))

    val res = MarkdownPreprocessor.preprocess(
      markdownFile,
      logger,
      allowRestrictedFeatures = false,
      suppressWarningOptions = SuppressWarningOptions()
    )(using ScalaCliInvokeData.dummy)
    val expectedMessage = s"File not found: ${markdownFile.path}"

    assert(res.nonEmpty)
    assert(res.get.isLeft)
    expect(res.get.swap.toOption.get.message == expectedMessage)
  }
}
