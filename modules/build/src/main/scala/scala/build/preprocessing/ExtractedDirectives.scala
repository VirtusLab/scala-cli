package scala.build.preprocessing

import com.virtuslab.using_directives.config.Settings
import com.virtuslab.using_directives.custom.model.{
  UsingDirectiveKind,
  UsingDirectiveSyntax,
  UsingDirectives
}
import com.virtuslab.using_directives.custom.utils.ast.{UsingDef, UsingDefs}
import com.virtuslab.using_directives.{Context, UsingDirectivesProcessor}

import scala.annotation.targetName
import scala.build.errors.*
import scala.build.preprocessing.UsingDirectivesOps.*
import scala.build.preprocessing.directives.{DirectiveUtil, ScopedDirective, StrictDirective}
import scala.build.{Logger, Position}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

case class ExtractedDirectives(
  offset: Int,
  directives: Seq[StrictDirective],
  positions: Option[DirectivesPositions]
) {
  @targetName("append")
  def ++(other: ExtractedDirectives): ExtractedDirectives =
    ExtractedDirectives(offset, directives ++ other.directives, positions)
}

case class DirectivesPositions(
  codeDirectives: Position.File,
  specialCommentDirectives: Position.File,
  plainCommentDirectives: Position.File
)

object ExtractedDirectives {

  def empty: ExtractedDirectives = ExtractedDirectives(0, Seq.empty, None)

  val changeToSpecialCommentMsg =
    "Using directive using plain comments are deprecated. Please use a special comment syntax: '//> ...' or '/*> ... */'"

  def from(
    contentChars: Array[Char],
    path: Either[String, os.Path],
    logger: Logger,
    supportedDirectives: Array[UsingDirectiveKind],
    cwd: ScopePath,
    maybeRecoverOnError: BuildException => Option[BuildException]
  ): Either[BuildException, ExtractedDirectives] = {
    val errors = new mutable.ListBuffer[Diagnostic]
    val reporter = CustomDirectivesReporter.create(path) { diag =>
      if (diag.severity == Severity.Warning)
        logger.log(Seq(diag))
      else
        errors += diag
    }
    val processor = {
      val settings = new Settings
      settings.setAllowStartWithoutAt(true)
      settings.setAllowRequire(false)
      val context = new Context(reporter, settings)
      new UsingDirectivesProcessor(context)
    }
    val all = processor.extract(contentChars, true, true).asScala
    val malformedDirectiveErrors =
      errors.map(diag => new MalformedDirectiveError(diag.message, diag.positions)).toSeq
    val maybeCompositeMalformedDirectiveError =
      if (malformedDirectiveErrors.nonEmpty)
        maybeRecoverOnError(CompositeBuildException(malformedDirectiveErrors))
      else None
    if (malformedDirectiveErrors.isEmpty || maybeCompositeMalformedDirectiveError.isEmpty) {

      def byKind(kind: UsingDirectiveKind) = all.find(_.getKind == kind).get

      val codeDirectives           = byKind(UsingDirectiveKind.Code)
      val specialCommentDirectives = byKind(UsingDirectiveKind.SpecialComment)
      val plainCommentDirectives   = byKind(UsingDirectiveKind.PlainComment)

      val directivesPositionsOpt =
        if (
          codeDirectives.containsTargetDirectivesOnly &&
          specialCommentDirectives.containsTargetDirectivesOnly &&
          plainCommentDirectives.containsTargetDirectivesOnly
        )
          None
        else
          Some(DirectivesPositions(
            codeDirectives.getPosition(path),
            specialCommentDirectives.getPosition(path),
            plainCommentDirectives.getPosition(path)
          ))

      def reportWarning(msg: String, values: Seq[UsingDef], before: Boolean = true): Unit =
        values.foreach { v =>
          val astPos = v.getPosition
          val (start, end) =
            if (before) (0, astPos.getColumn)
            else (astPos.getColumn, astPos.getColumn + v.getSyntax.getKeyword.length)
          val position = Position.File(path, (astPos.getLine, start), (astPos.getLine, end))
          logger.diagnostic(msg, positions = Seq(position))
        }

      val usedDirectives =
        if (codeDirectives.nonEmpty) {
          val msg =
            "This using directive is ignored. File contains directives outside comments and those have higher precedence."
          reportWarning(
            msg,
            getDirectives(plainCommentDirectives) ++ getDirectives(specialCommentDirectives)
          )
          codeDirectives
        }
        else if (specialCommentDirectives.nonEmpty) {
          val msg =
            s"This using directive is ignored. $changeToSpecialCommentMsg"
          reportWarning(msg, getDirectives(plainCommentDirectives))
          specialCommentDirectives
        }
        else {
          reportWarning(changeToSpecialCommentMsg, getDirectives(plainCommentDirectives))
          plainCommentDirectives
        }

      // All using directives should use just `using` keyword, no @using or require
      reportWarning(
        "Deprecated using directive syntax, please use keyword `using`.",
        getDirectives(usedDirectives).filter(_.getSyntax != UsingDirectiveSyntax.Using),
        before = false
      )

      val flattened = usedDirectives.getFlattenedMap.asScala.toSeq
      val strictDirectives =
        flattened.map {
          case (k, l) =>
            StrictDirective(k.getPath.asScala.mkString("."), l.asScala.toSeq)
        }

      val offset =
        if (usedDirectives.getKind != UsingDirectiveKind.Code) 0
        else usedDirectives.getCodeOffset
      if (supportedDirectives.contains(usedDirectives.getKind))
        Right(ExtractedDirectives(offset, strictDirectives, directivesPositionsOpt))
      else {
        val directiveVales =
          usedDirectives.getFlattenedMap.values().asScala.toList.flatMap(_.asScala)
        val values = DirectiveUtil.concatAllValues(ScopedDirective(
          StrictDirective("", directiveVales),
          path,
          cwd
        ))
        val directiveErrors = new DirectiveErrors(
          ::(s"Directive '${usedDirectives.getKind}' is not supported in the given context'", Nil),
          values.flatMap(_.positions)
        )
        maybeRecoverOnError(directiveErrors) match {
          case Some(e) => Left(e)
          case None    => Right(ExtractedDirectives.empty)
        }
      }
    }
    else
      maybeCompositeMalformedDirectiveError match {
        case Some(e) => Left(e)
        case None    => Right(ExtractedDirectives.empty)
      }
  }
}
