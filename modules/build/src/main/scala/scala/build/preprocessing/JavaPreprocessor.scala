package scala.build.preprocessing

import coursier.cache.ArchiveCache
import coursier.util.Task

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.input.{Inputs, JavaFile, ScalaCliInvokeData, SingleElement, VirtualJavaFile}
import scala.build.internal.JavaParserProxyMaker
import scala.build.options.{
  BuildOptions,
  BuildRequirements,
  SuppressWarningOptions,
  WithBuildRequirements
}
import scala.build.preprocessing.directives.PreprocessedDirectives

/** Java source preprocessor.
  *
  * Doesn't modify Java sources. This only extracts using directives from them, and for unnamed
  * sources (like stdin), tries to infer a class name from the sources themselves.
  *
  * @param archiveCache
  *   when using a java-class-name external binary to infer a class name (see [[JavaParserProxy]]),
  *   a cache to download that binary with
  * @param javaClassNameVersionOpt
  *   when using a java-class-name external binary to infer a class name (see [[JavaParserProxy]]),
  *   this forces the java-class-name version to download
  */
final case class JavaPreprocessor(
  archiveCache: ArchiveCache[Task],
  javaClassNameVersionOpt: Option[String],
  javaCommand: () => String
) extends Preprocessor {
  def preprocess(
    input: SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e),
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  )(using ScalaCliInvokeData): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case j: JavaFile => Some(either {
          val content: String = value(PreprocessingUtil.maybeRead(j.path))
          val scopePath       = ScopePath.fromPath(j.path)
          val preprocessedDirectives: PreprocessedDirectives = value {
            DirectivesPreprocessor.preprocess(
              content,
              Right(j.path),
              scopePath,
              logger,
              allowRestrictedFeatures,
              suppressWarningOptions,
              maybeRecoverOnError
            )
          }
          Seq(PreprocessedSource.OnDisk(
            path = j.path,
            options = Some(preprocessedDirectives.globalUsings),
            optionsWithTargetRequirements = preprocessedDirectives.usingsWithReqs,
            requirements = Some(BuildRequirements()),
            scopedRequirements = Nil,
            mainClassOpt = None,
            directivesPositions = preprocessedDirectives.directivesPositions
          ))
        })
      case v: VirtualJavaFile =>
        val res = either {
          val relPath =
            if (v.isStdin || v.isSnippet) {
              val classNameOpt = value {
                (new JavaParserProxyMaker)
                  .get(
                    archiveCache,
                    javaClassNameVersionOpt,
                    logger,
                    () => javaCommand()
                  )
                  .className(v.content)
              }
              val fileName = classNameOpt
                .map(_ + ".java")
                .getOrElse(v.generatedSourceFileName)
              os.sub / fileName
            }
            else v.subPath
          val content = new String(v.content, StandardCharsets.UTF_8)
          val preprocessedDirectives: PreprocessedDirectives = value {
            DirectivesPreprocessor.preprocess(
              content,
              Left(relPath.toString),
              v.scopePath,
              logger,
              allowRestrictedFeatures,
              suppressWarningOptions,
              maybeRecoverOnError
            )
          }
          val s = PreprocessedSource.InMemory(
            originalPath = Left(v.source),
            relPath = relPath,
            code = content,
            ignoreLen = 0,
            options = Some(preprocessedDirectives.globalUsings),
            optionsWithTargetRequirements = preprocessedDirectives.usingsWithReqs,
            requirements = Some(BuildRequirements()),
            scopedRequirements = Nil,
            mainClassOpt = None,
            scopePath = v.scopePath,
            directivesPositions = preprocessedDirectives.directivesPositions
          )
          Seq(s)
        }
        Some(res)

      case _ => None
    }
}
