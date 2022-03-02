package scala.build.preprocessing

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.{AmmUtil, CodeWrapper, CustomCodeWrapper, Name}
import scala.build.options.{BuildOptions, BuildRequirements}
import scala.build.preprocessing.ScalaPreprocessor.ProcessingOutput
import scala.build.{Inputs, Logger}

final case class ScriptPreprocessor(codeWrapper: CodeWrapper) extends Preprocessor {
  def preprocess(
    input: Inputs.SingleElement,
    logger: Logger
  ): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case script: Inputs.Script =>
        val res = either {
          val content = value(PreprocessingUtil.maybeRead(script.path))
          val preprocessed = value {
            ScriptPreprocessor.preprocess(
              Right(script.path),
              content,
              codeWrapper,
              script.subPath,
              ScopePath.fromPath(script.path),
              logger
            )
          }
          preprocessed
        }
        Some(res)

      case script: Inputs.VirtualScript =>
        val content = new String(script.content, StandardCharsets.UTF_8)

        val res = either {
          val preprocessed = value {
            ScriptPreprocessor.preprocess(
              Left(script.source),
              content,
              codeWrapper,
              script.wrapperPath,
              script.scopePath,
              logger
            )
          }
          preprocessed
        }
        Some(res)

      case _ =>
        None
    }
}

object ScriptPreprocessor {

  private def preprocess(
    reportingPath: Either[String, os.Path],
    content: String,
    codeWrapper: CodeWrapper,
    subPath: os.SubPath,
    scopePath: ScopePath,
    logger: Logger
  ): Either[BuildException, List[PreprocessedSource.InMemory]] = either {

    val (contentIgnoredSheBangLines, _) = SheBang.ignoreSheBangLines(content)

    val (pkg, wrapper) = AmmUtil.pathToPackageWrapper(subPath)

    val processingOutput =
      value(ScalaPreprocessor.process(
        contentIgnoredSheBangLines,
        reportingPath,
        scopePath / os.up,
        logger
      ))
        .getOrElse(ProcessingOutput(BuildRequirements(), Nil, BuildOptions(), None))

    val (code, topWrapperLen, _) = codeWrapper.wrapCode(
      pkg,
      wrapper,
      processingOutput.updatedContent.getOrElse(contentIgnoredSheBangLines)
    )

    val className = (pkg :+ wrapper).map(_.raw).mkString(".")
    val relPath   = os.rel / (subPath / os.up) / s"${subPath.last.stripSuffix(".sc")}.scala"

    val file = PreprocessedSource.InMemory(
      reportingPath.map((subPath, _)),
      relPath,
      code,
      topWrapperLen,
      Some(processingOutput.opts),
      Some(processingOutput.globalReqs),
      processingOutput.scopedReqs,
      Some(CustomCodeWrapper.mainClassObject(Name(className)).backticked),
      scopePath
    )
    List(file)
  }

}
