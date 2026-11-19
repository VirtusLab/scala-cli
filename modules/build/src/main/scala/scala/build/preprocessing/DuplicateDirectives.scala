package scala.build.preprocessing

import scala.build.errors.{Diagnostic, Severity}
import scala.build.internal.util.WarningMessages
import scala.build.options.SuppressWarningOptions
import scala.build.preprocessing.directives.{DirectivesPreprocessingUtils, StrictDirective}
import scala.build.{Logger, Position}

object DuplicateDirectives {

  // Map every recognized key alias (excluding target.* requirement directives) to a canonical key
  // so equivalent spellings like `dep` and `deps` are treated as the same directive.
  private lazy val canonicalKeyByAlias: Map[String, String] = {
    val handlers =
      DirectivesPreprocessingUtils.usingDirectiveHandlers ++
        DirectivesPreprocessingUtils.usingDirectiveWithReqsHandlers
    for {
      handler   <- handlers
      key       <- handler.keys
      canonical <- key.nameAliases.headOption.toSeq
      alias     <- key.nameAliases
    } yield alias -> canonical
  }.toMap

  private def directivePosition(
    directive: StrictDirective,
    path: Either[String, os.Path]
  ): Position.File =
    if directive.values.nonEmpty then directive.position(path)
    else
      Position.File(
        path,
        (directive.startLine, directive.startColumn),
        (directive.startLine, directive.startColumn + directive.key.length)
      )

  /** Returns exact duplicate directives after their first occurrence, ordered by source position.
    */
  def findDuplicates(directives: Seq[StrictDirective]): Seq[StrictDirective] = {
    // ExtractedDirectives.from stores directives in reverse order, so sort back to source order.
    val sorted             = directives.sortBy(d => (d.startLine, d.startColumn))
    val (_, duplicatesRev) =
      sorted.foldLeft((Set.empty[(String, Seq[String])], List.empty[StrictDirective])) {
        case ((seen, acc), directive) =>
          canonicalKeyByAlias.get(directive.key) match
            case Some(canonicalKey) =>
              val signature = (canonicalKey, directive.toStringValues)
              if seen.contains(signature) then (seen, directive :: acc)
              else (seen + signature, acc)
            case None =>
              (seen, acc)
      }
    duplicatesRev.reverse
  }

  private def formatValue(value: String): String =
    if value.isEmpty || value.exists(c => c.isWhitespace || c == '"') then
      val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
      s"\"$escaped\""
    else value

  private def formatDirective(directive: StrictDirective): String = {
    val values = directive.toStringValues.map(formatValue)
    if values.isEmpty then s"//> using ${directive.key}"
    else s"//> using ${directive.key} ${values.mkString(" ")}"
  }

  /** Emits warnings for duplicated using directives in a single source file. */
  def issueWarnings(
    path: Either[String, os.Path],
    directives: Seq[StrictDirective],
    suppressWarningOptions: SuppressWarningOptions,
    logger: Logger
  ): Unit =
    if !suppressWarningOptions.suppressDuplicatedDirectivesWarning.getOrElse(false) then
      val diagnostics = findDuplicates(directives).map { directive =>
        Diagnostic(
          message = WarningMessages.duplicatedUsingDirective(formatDirective(directive)),
          severity = Severity.Warning,
          positions = Seq(directivePosition(directive, path))
        )
      }
      if diagnostics.nonEmpty then logger.log(diagnostics)
}
