package scala.build.preprocessing

import com.virtuslab.using_directives.UsingDirectivesProcessor
import com.virtuslab.using_directives.custom.model.UsingDirectives
import com.virtuslab.using_directives.custom.utils.ast.{UsingDef, UsingDefs}

import scala.annotation.targetName
import scala.build.errors.*
import scala.build.preprocessing.UsingDirectivesOps.*
import scala.build.preprocessing.directives.{DirectiveUtil, ScopedDirective, StrictDirective}
import scala.build.{Logger, Position}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

case class ExtractedDirectives(
  directives: Seq[StrictDirective],
  positions: Option[Position.File]
) {
  @targetName("append")
  def ++(other: ExtractedDirectives): ExtractedDirectives =
    ExtractedDirectives(directives ++ other.directives, positions)
}

object ExtractedDirectives {

  def empty: ExtractedDirectives = ExtractedDirectives(Seq.empty, None)

  val changeToSpecialCommentMsg =
    "Using directive using plain comments are deprecated. Please use a special comment syntax: '//> ...'"

  def from(
    contentChars: Array[Char],
    path: Either[String, os.Path],
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException]
  ): Either[BuildException, ExtractedDirectives] = {
    val errors = new mutable.ListBuffer[Diagnostic]
    val reporter = CustomDirectivesReporter.create(path) { diag =>
      if (diag.severity == Severity.Warning)
        logger.log(Seq(diag))
      else
        errors += diag
    }
    val processor     = new UsingDirectivesProcessor(reporter)
    val allDirectives = processor.extract(contentChars).asScala
    val malformedDirectiveErrors =
      errors.map(diag => new MalformedDirectiveError(diag.message, diag.positions)).toSeq
    val maybeCompositeMalformedDirectiveError =
      if (malformedDirectiveErrors.nonEmpty)
        maybeRecoverOnError(CompositeBuildException(malformedDirectiveErrors))
      else None
    if (malformedDirectiveErrors.isEmpty || maybeCompositeMalformedDirectiveError.isEmpty) {

      val directives = allDirectives.head
      val directivesPositionOpt =
        if (directives.containsTargetDirectivesOnly)
          None
        else
          Some(directives.getPosition(path))

      val flattened = directives.getFlattenedMap.asScala.toSeq
      val strictDirectives =
        flattened.map {
          case (k, l) =>
            StrictDirective(k.getPath.asScala.mkString("."), l.asScala.toSeq)
        }

      Right(ExtractedDirectives(strictDirectives, directivesPositionOpt))
    }
    else
      maybeCompositeMalformedDirectiveError match {
        case Some(e) => Left(e)
        case None    => Right(ExtractedDirectives.empty)
      }
  }

}
