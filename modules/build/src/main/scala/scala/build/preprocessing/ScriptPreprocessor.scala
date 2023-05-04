package scala.build.preprocessing

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.input.{Inputs, ScalaCliInvokeData, Script, SingleElement, VirtualScript}
import scala.build.internal.{AmmUtil, CodeWrapper, CustomCodeWrapper, Name}
import scala.build.options.{BuildOptions, BuildRequirements, SuppressWarningOptions}
import scala.build.preprocessing.ScalaPreprocessor.ProcessingOutput

final case class ScriptPreprocessor(codeWrapper: CodeWrapper) extends Preprocessor {
  def preprocess(
    input: SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e),
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  )(using ScalaCliInvokeData): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case script: Script =>
        val res = either {
          val content = value(PreprocessingUtil.maybeRead(script.path))
          val preprocessed = value {
            ScriptPreprocessor.preprocess(
              Right(script.path),
              content,
              codeWrapper,
              script.subPath,
              script.inputArg,
              ScopePath.fromPath(script.path),
              logger,
              maybeRecoverOnError,
              allowRestrictedFeatures,
              suppressWarningOptions
            )
          }
          preprocessed
        }
        Some(res)

      case script: VirtualScript =>
        val content = new String(script.content, StandardCharsets.UTF_8)

        val res = either {
          val preprocessed = value {
            ScriptPreprocessor.preprocess(
              Left(script.source),
              content,
              codeWrapper,
              script.wrapperPath,
              None,
              script.scopePath,
              logger,
              maybeRecoverOnError,
              allowRestrictedFeatures,
              suppressWarningOptions
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
    inputArgPath: Option[String],
    scopePath: ScopePath,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException],
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  )(using ScalaCliInvokeData): Either[BuildException, List[PreprocessedSource.InMemory]] = either {

    val (contentIgnoredSheBangLines, _) = SheBang.ignoreSheBangLines(content)

    val (pkg, wrapper) = AmmUtil.pathToPackageWrapper(subPath)

    val processingOutput: ProcessingOutput =
      value(ScalaPreprocessor.process(
        contentIgnoredSheBangLines,
        reportingPath,
        scopePath / os.up,
        logger,
        maybeRecoverOnError,
        allowRestrictedFeatures,
        suppressWarningOptions
      ))
        .getOrElse(ProcessingOutput.empty)

    val (code, topWrapperLen, _) = codeWrapper.wrapCode(
      pkg,
      wrapper,
      processingOutput.updatedContent.getOrElse(contentIgnoredSheBangLines),
      inputArgPath.getOrElse(subPath.last)
    )

    val className = (pkg :+ wrapper).map(_.raw).mkString(".")
    val relPath   = os.rel / (subPath / os.up) / s"${subPath.last.stripSuffix(".sc")}.scala"

    val file = PreprocessedSource.InMemory(
      originalPath = reportingPath.map((subPath, _)),
      relPath = relPath,
      code = code,
      ignoreLen = topWrapperLen,
      options = Some(processingOutput.opts),
      optionsWithTargetRequirements = processingOutput.optsWithReqs,
      requirements = Some(processingOutput.globalReqs),
      scopedRequirements = processingOutput.scopedReqs,
      mainClassOpt = Some(CustomCodeWrapper.mainClassObject(Name(className)).backticked),
      scopePath = scopePath,
      directivesPositions = processingOutput.directivesPositions
    )
    List(file)
  }

}
