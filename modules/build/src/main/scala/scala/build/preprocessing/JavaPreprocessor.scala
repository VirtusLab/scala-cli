package scala.build.preprocessing

import com.virtuslab.using_directives.custom.model.UsingDirectiveKind

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.options.BuildRequirements
import scala.build.preprocessing.ExtractedDirectives.from
import scala.build.preprocessing.ScalaPreprocessor._
import scala.build.{Inputs, Logger}

import scala.build.internal.JavaParser

case object JavaPreprocessor extends Preprocessor {
  def preprocess(
    input: Inputs.SingleElement,
    logger: Logger
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
              scopePath
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
        val relPath =
          if (v.isStdin) {
            val fileName = JavaParser.parseRootPublicClassName(v.content).map(
              _ + ".java"
            ).getOrElse("stdin.java")
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
        Some(Right(Seq(s)))

      case _ => None
    }
}
