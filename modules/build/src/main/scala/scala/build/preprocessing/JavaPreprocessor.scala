package scala.build.preprocessing

import com.virtuslab.using_directives.custom.model.UsingDirectiveKind
import coursier.cache.ArchiveCache
import coursier.util.Task

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.input.{Inputs, JavaFile, SingleElement, VirtualJavaFile}
import scala.build.internal.JavaParserProxyMaker
import scala.build.options.{BuildRequirements, SuppressWarningOptions}
import scala.build.preprocessing.ExtractedDirectives.from
import scala.build.preprocessing.PreprocessingUtil.optionsAndPositionsFromDirectives
import scala.build.preprocessing.ScalaPreprocessor.*

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
  ): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case j: JavaFile => Some(either {
          val content   = value(PreprocessingUtil.maybeRead(j.path))
          val scopePath = ScopePath.fromPath(j.path)
          val (updatedOptions, directivesPositions) = value {
            optionsAndPositionsFromDirectives(
              content,
              scopePath,
              Right(j.path),
              logger,
              maybeRecoverOnError,
              allowRestrictedFeatures,
              suppressWarningOptions
            )
          }
          Seq(PreprocessedSource.OnDisk(
            j.path,
            Some(updatedOptions.global),
            Some(BuildRequirements()),
            Nil,
            None,
            directivesPositions
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
          val (updatedOptions, directivesPositions) = value {
            optionsAndPositionsFromDirectives(
              content,
              v.scopePath,
              Left(relPath.toString),
              logger,
              maybeRecoverOnError,
              allowRestrictedFeatures,
              suppressWarningOptions
            )
          }
          val s = PreprocessedSource.InMemory(
            originalPath = Left(v.source),
            relPath = relPath,
            code = content,
            ignoreLen = 0,
            options = Some(updatedOptions.global),
            requirements = Some(BuildRequirements()),
            scopedRequirements = Nil,
            mainClassOpt = None,
            scopePath = v.scopePath,
            directivesPositions = directivesPositions
          )
          Seq(s)
        }
        Some(res)

      case _ => None
    }
}
