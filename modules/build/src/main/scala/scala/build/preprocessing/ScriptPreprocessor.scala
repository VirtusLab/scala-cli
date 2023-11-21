package scala.build.preprocessing

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.input.{Inputs, ScalaCliInvokeData, Script, SingleElement, VirtualScript}
import scala.build.internal.*
import scala.build.internal.util.WarningMessages
import scala.build.options.{BuildOptions, BuildRequirements, Platform, SuppressWarningOptions}
import scala.build.preprocessing.PreprocessedSource
import scala.build.preprocessing.ScalaPreprocessor.ProcessingOutput

case object ScriptPreprocessor extends Preprocessor {
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

  private def preprocess(
    reportingPath: Either[String, os.Path],
    content: String,
    subPath: os.SubPath,
    inputArgPath: Option[String],
    scopePath: ScopePath,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException],
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  )(using ScalaCliInvokeData): Either[BuildException, List[PreprocessedSource.UnwrappedScript]] =
    either {
      val (pkg, wrapper) = AmmUtil.pathToPackageWrapper(subPath)

      val processingOutput: ProcessingOutput =
        value(ScalaPreprocessor.process(
          content,
          reportingPath,
          scopePath / os.up,
          logger,
          maybeRecoverOnError,
          allowRestrictedFeatures,
          suppressWarningOptions
        ))
          .getOrElse(ProcessingOutput.empty)

      val scriptCode =
        processingOutput.updatedContent.getOrElse(SheBang.ignoreSheBangLines(content)._1)
      // try to match in multiline mode, don't match comment lines starting with '//'
      val containsMainAnnot = "(?m)^(?!//).*@main.*".r.findFirstIn(scriptCode).isDefined

      val wrapScriptFun = getScriptWrappingFunction(
        logger,
        containsMainAnnot,
        pkg,
        wrapper,
        scriptCode,
        inputArgPath.getOrElse(subPath.toString)
      )

      val className = (pkg :+ wrapper).map(_.raw).mkString(".")
      val relPath   = os.rel / (subPath / os.up) / s"${subPath.last.stripSuffix(".sc")}.scala"

      val file = PreprocessedSource.UnwrappedScript(
        originalPath = reportingPath.map((subPath, _)),
        relPath = relPath,
        options = Some(processingOutput.opts),
        optionsWithTargetRequirements = processingOutput.optsWithReqs,
        requirements = Some(processingOutput.globalReqs),
        scopedRequirements = processingOutput.scopedReqs,
        scopePath = scopePath,
        directivesPositions = processingOutput.directivesPositions,
        wrapScriptFun = wrapScriptFun
      )
      List(file)
    }

  def getScriptWrappingFunction(
    logger: Logger,
    containsMainAnnot: Boolean,
    packageStrings: Seq[Name],
    wrapperName: Name,
    scriptCode: String,
    scriptPath: String
  ): CodeWrapper => (String, WrapperParams) = {
    (codeWrapper: CodeWrapper) =>
      if (containsMainAnnot) logger.diagnostic(
        codeWrapper match {
          case _: AppCodeWrapper.type =>
            WarningMessages.mainAnnotationNotSupported( /* annotationIgnored */ true)
          case _ => WarningMessages.mainAnnotationNotSupported( /* annotationIgnored */ false)
        }
      )

      val (code, wrapperParams) = codeWrapper.wrapCode(
        packageStrings,
        wrapperName,
        scriptCode,
        scriptPath
      )
      (code, wrapperParams)
  }

  /** Get correct script wrapper depending on the platform and version of Scala. For Scala 2 or
    * Platform JS use [[ObjectCodeWrapper]]. Otherwise - for Scala 3 on JVM or Native use
    * [[ClassCodeWrapper]].
    * @param buildOptions
    *   final version of options, build may fail if incompatible wrapper is chosen
    * @return
    *   code wrapper compatible with provided BuildOptions
    */
  def getScriptWrapper(buildOptions: BuildOptions): CodeWrapper = {
    val scalaVersionOpt = for {
      maybeScalaVersion <- buildOptions.scalaOptions.scalaVersion
      scalaVersion      <- maybeScalaVersion.versionOpt
    } yield scalaVersion

    def objectCodeWrapperForScalaVersion =
      // AppObjectWrapper only introduces the 'main.sc' restriction when used in Scala 3, there's no gain in using it with Scala 3
      if (scalaVersionOpt.exists(_.startsWith("2")))
        AppCodeWrapper
      else
        ObjectCodeWrapper

    buildOptions.scriptOptions.forceObjectWrapper match {
      case Some(true) => objectCodeWrapperForScalaVersion
      case _ =>
        buildOptions.scalaOptions.platform.map(_.value) match {
          case Some(_: Platform.JS.type)                      => objectCodeWrapperForScalaVersion
          case _ if scalaVersionOpt.exists(_.startsWith("2")) => AppCodeWrapper
          case _                                              => ClassCodeWrapper
        }
    }
  }
}
