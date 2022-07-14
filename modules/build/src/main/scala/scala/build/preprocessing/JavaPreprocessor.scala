package scala.build.preprocessing

import com.virtuslab.using_directives.custom.model.UsingDirectiveKind
import coursier.cache.ArchiveCache
import coursier.util.Task

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.JavaParserProxyMaker
import scala.build.options.BuildRequirements
import scala.build.preprocessing.ExtractedDirectives.from
import scala.build.preprocessing.ScalaPreprocessor._
import scala.build.{Inputs, Logger}

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
    input: Inputs.SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e)
  ): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case j: Inputs.JavaFile => Some(either {
          val content   = value(PreprocessingUtil.maybeRead(j.path))
          val scopePath = ScopePath.fromPath(j.path)
          val ExtractedDirectives(_, directives0) =
            value(from(
              content.toCharArray,
              Right(j.path),
              logger,
              Array(UsingDirectiveKind.PlainComment, UsingDirectiveKind.SpecialComment),
              scopePath,
              maybeRecoverOnError
            ))
          val updatedOptions = value(DirectivesProcessor.process(
            directives0,
            usingDirectiveHandlers,
            Right(j.path),
            scopePath,
            logger
          ))
          Seq(PreprocessedSource.OnDisk(
            j.path,
            Some(updatedOptions.global),
            Some(BuildRequirements()),
            Nil,
            None
          ))
        })
      case v: Inputs.VirtualJavaFile =>
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
          val s = PreprocessedSource.InMemory(
            originalPath = Left(v.source),
            relPath = relPath,
            code = content,
            ignoreLen = 0,
            options = None,
            requirements = None,
            scopedRequirements = Nil,
            mainClassOpt = None,
            scopePath = v.scopePath
          )
          Seq(s)
        }
        Some(res)

      case _ => None
    }
}
