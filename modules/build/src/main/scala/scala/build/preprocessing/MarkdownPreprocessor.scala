package scala.build.preprocessing

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.{AmmUtil, CodeWrapper, CustomCodeWrapper, Name}
import scala.build.options.{BuildOptions, BuildRequirements}
import scala.build.preprocessing.ScalaPreprocessor.ProcessingOutput
import scala.build.{Inputs, Logger}

import scala.build.preprocessing.mdsandbox.SnippetPackager

case object MarkdownPreprocessor extends Preprocessor {
  def preprocess(
    input: Inputs.SingleElement,
    logger: Logger
  ): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case markdown: Inputs.MarkdownFile =>
        val res = either {
          val content = value(PreprocessingUtil.maybeRead(markdown.path))
          val preprocessed = value {
            MarkdownPreprocessor.preprocess(
              Right(markdown.path),
              content,
              markdown.subPath,
              ScopePath.fromPath(markdown.path),
              logger
            )
          }
          preprocessed
        }
        Some(res)

      case _ =>
        None
    }
  
  private def preprocess(
    reportingPath: Either[String, os.Path],
    content: String,
    subPath: os.SubPath,
    scopePath: ScopePath,
    logger: Logger
  ): Either[BuildException, List[PreprocessedSource.InMemory]] = either {

    val packager: SnippetPackager = new SnippetPackager(subPath.toString, content)
    val topWrapperLen = 0

    val parsedMain: String = packager.buildScalaMain()
    val mainProcessingOutput =
      value(ScalaPreprocessor.process(
        parsedMain,
        reportingPath,
        scopePath / os.up,
        logger
      ))
        .getOrElse(ProcessingOutput(BuildRequirements(), Nil, BuildOptions(), None))
    val mainCode = mainProcessingOutput.updatedContent.getOrElse(parsedMain)
    val mainClassName = s"Markdown_${subPath.last}"
    val mainRelPath   = os.rel / (subPath / os.up) / s"${subPath.last.stripSuffix(".md")}_mainmd.scala"
    val mainFile = PreprocessedSource.InMemory(
      reportingPath.map((subPath, _)),
      mainRelPath,
      mainCode,
      topWrapperLen,
      Some(mainProcessingOutput.opts),
      Some(mainProcessingOutput.globalReqs),
      mainProcessingOutput.scopedReqs,
      Some(CustomCodeWrapper.mainClassObject(Name(mainClassName)).backticked),
      scopePath
    )

    val parsedTest: String = packager.buildScalaTest()
    val testProcessingOutput =
      value(ScalaPreprocessor.process(
        parsedTest,
        reportingPath,
        scopePath / os.up,
        logger
      ))
        .getOrElse(ProcessingOutput(BuildRequirements(), Nil, BuildOptions(), None))
    val testCode = testProcessingOutput.updatedContent.getOrElse(parsedTest)
    val testRelPath   = os.rel / (subPath / os.up) / s"${subPath.last.stripSuffix(".md")}_testmd.scala"
    val testFile = PreprocessedSource.InMemory(
      reportingPath.map((subPath, _)),
      testRelPath,
      testCode,
      topWrapperLen,
      Some(testProcessingOutput.opts),
      Some(testProcessingOutput.globalReqs),
      testProcessingOutput.scopedReqs,
      None,
      scopePath
    )

    val parsedRaw: String = packager.buildScalaRaw()
    val rawProcessingOutput =
      value(ScalaPreprocessor.process(
        parsedRaw,
        reportingPath,
        scopePath / os.up,
        logger
      ))
        .getOrElse(ProcessingOutput(BuildRequirements(), Nil, BuildOptions(), None))
    val rawCode = rawProcessingOutput.updatedContent.getOrElse(parsedRaw)
    val rawRelPath   = os.rel / (subPath / os.up) / s"${subPath.last.stripSuffix(".md")}_rawmd.scala"
    val rawFile = PreprocessedSource.InMemory(
      reportingPath.map((subPath, _)),
      rawRelPath,
      rawCode,
      topWrapperLen,
      Some(rawProcessingOutput.opts),
      Some(rawProcessingOutput.globalReqs),
      rawProcessingOutput.scopedReqs,
      None,
      scopePath
    )

    List(mainFile, testFile, rawFile)
  }

}
