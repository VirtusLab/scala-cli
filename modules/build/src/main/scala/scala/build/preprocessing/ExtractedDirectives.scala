package scala.build.preprocessing

import scala.annotation.targetName
import scala.build.errors.*
import scala.build.options.SuppressWarningOptions
import scala.build.preprocessing.directives.StrictDirective
import scala.build.{Logger, Position}
import scala.cli.parse.{DiagnosticSeverity, DirectiveValue, UsingDirectivesParser}
import scala.collection.mutable

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
    val result           = UsingDirectivesParser.parse(contentChars)
    val diagnosticErrors = mutable.ListBuffer.empty[Diagnostic]

    for diag <- result.diagnostics do
      val positions = diag.position.map { p =>
        Position.File(path, (p.line, p.column), (p.line, p.column))
      }.toSeq

      if diag.severity == DiagnosticSeverity.Warning then
        if diag.message.toLowerCase.contains("deprecated") &&
          suppressWarningOptions.suppressDeprecatedFeatureWarning.getOrElse(false)
        then () // skip
        else logger.log(Seq(Diagnostic(diag.message, Severity.Warning, positions)))
      else
        diagnosticErrors += Diagnostic(diag.message, Severity.Error, positions)

    val malformedDirectiveErrors =
      diagnosticErrors
        .map(diag => new MalformedDirectiveError(diag.message, diag.positions))
        .toSeq

    val maybeCompositeMalformedDirectiveError =
      if malformedDirectiveErrors.nonEmpty then
        maybeRecoverOnError(CompositeBuildException(malformedDirectiveErrors))
      else None

    if malformedDirectiveErrors.isEmpty || maybeCompositeMalformedDirectiveError.isEmpty then
      val directives = result.directives

      val containsTargetDirectives = directives.exists(_.key.startsWith("target."))

      val directivesPositionOpt =
        if containsTargetDirectives || directives.isEmpty then None
        else
          val lastDirective     = directives.last
          val (endLine, endCol) = lastDirective.values.lastOption match
            case Some(sv: DirectiveValue.StringVal) if sv.isQuoted =>
              (sv.pos.line, sv.pos.column + sv.value.length + 2)
            case Some(sv: DirectiveValue.StringVal) =>
              (sv.pos.line, sv.pos.column + sv.value.length)
            case Some(bv: DirectiveValue.BoolVal) =>
              (bv.pos.line, bv.pos.column + bv.value.toString.length)
            case Some(ev: DirectiveValue.EmptyVal) =>
              (ev.pos.line, ev.pos.column)
            case None =>
              val kp = lastDirective.keyPosition
              (kp.line, kp.column + lastDirective.key.length)
          Some(Position.File(path, (0, 0), (endLine, endCol), result.codeOffset))

      val strictDirectives = directives.map { ud =>
        StrictDirective(
          ud.key,
          ud.values,
          ud.keyPosition.column,
          ud.keyPosition.line
        )
      }

      Right(ExtractedDirectives(strictDirectives.reverse, directivesPositionOpt))
    else
      maybeCompositeMalformedDirectiveError match
        case Some(e) => Left(e)
        case None    => Right(ExtractedDirectives.empty)
  }
}
