package scala.build.preprocessing

import com.virtuslab.using_directives.UsingDirectivesProcessor
import com.virtuslab.using_directives.custom.model.{
  BooleanValue,
  EmptyValue,
  StringValue,
  UsingDirectives,
  Value
}
import com.virtuslab.using_directives.custom.utils.ast.*

import scala.annotation.targetName
import scala.build.errors.*
import scala.build.options.SuppressWarningOptions
import scala.build.preprocessing.UsingDirectivesOps.*
import scala.build.preprocessing.directives.{DirectiveUtil, ScopedDirective, StrictDirective}
import scala.build.{Logger, Position}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

case class ExtractedDirectives(
  directives: Seq[StrictDirective],
  position: Option[Position.File]
) {
  @targetName("append")
  def ++(other: ExtractedDirectives): ExtractedDirectives =
    ExtractedDirectives(directives ++ other.directives, position)
}

object ExtractedDirectives {

  def empty: ExtractedDirectives = ExtractedDirectives(Seq.empty, None)

  def from(
    contentChars: Array[Char],
    path: Either[String, os.Path],
    suppressWarningOptions: SuppressWarningOptions,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException]
  ): Either[BuildException, ExtractedDirectives] = {
    val errors = new mutable.ListBuffer[Diagnostic]
    val reporter = CustomDirectivesReporter
      .create(path) {
        case diag
            if diag.severity == Severity.Warning &&
            diag.message.toLowerCase.contains("deprecated") &&
            suppressWarningOptions.suppressDeprecatedFeatureWarning.getOrElse(false) =>
          () // skip deprecated feature warnings if suppressed
        case diag if diag.severity == Severity.Warning =>
          logger.log(Seq(diag))
        case diag => errors += diag
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

      val directivesOpt = allDirectives.headOption
      val directivesPositionOpt = directivesOpt match {
        case Some(directives)
            if directives.containsTargetDirectives ||
            directives.isEmpty => None
        case Some(directives) => Some(directives.getPosition(path))
        case None             => None
      }

      val strictDirectives = directivesOpt.toSeq.flatMap { directives =>
        def toStrictValue(value: UsingValue): Seq[Value[_]] = value match {
          case uvs: UsingValues   => uvs.values.asScala.toSeq.flatMap(toStrictValue)
          case el: EmptyLiteral   => Seq(EmptyValue(el))
          case sl: StringLiteral  => Seq(StringValue(sl.getValue(), sl))
          case bl: BooleanLiteral => Seq(BooleanValue(bl.getValue(), bl))
        }
        def toStrictDirective(ud: UsingDef) =
          StrictDirective(
            ud.getKey(),
            toStrictValue(ud.getValue()),
            ud.getPosition().getColumn(),
            ud.getPosition().getLine()
          )

        directives.getAst match
          case uds: UsingDefs => uds.getUsingDefs.asScala.toSeq.map(toStrictDirective)
          case ud: UsingDef   => Seq(toStrictDirective(ud))
          case _ => Nil // There should be nothing else here other than UsingDefs or UsingDef
      }

      Right(ExtractedDirectives(strictDirectives.reverse, directivesPositionOpt))
    }
    else
      maybeCompositeMalformedDirectiveError match {
        case Some(e) => Left(e)
        case None    => Right(ExtractedDirectives.empty)
      }
  }

}
