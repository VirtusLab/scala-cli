package scala.build.preprocessing

import scala.build.Logger
import scala.build.errors.Diagnostic.TextEdit
import scala.build.internal.util.WarningMessages.deprecatedWarning
import scala.build.preprocessing.directives.{
  DirectiveHandler,
  DirectiveUtil,
  StrictDirective,
  Toolkit
}
import scala.build.warnings.DeprecatedWarning

object DeprecatedDirectives {

  /** Used to represent a general form of a deprecated directive, and its replacement
    * @param keys
    *   representation of deprecated keys
    * @param values
    *   representation of deprecated value
    */
  case class DirectiveTemplate(keys: Seq[String], values: Option[Seq[String]]) {
    def appliesTo(foundKey: String, foundValues: Seq[String]): Boolean =
      (keys.isEmpty || keys.contains(foundKey)) &&
      // FIXME values.contains is not perfect, but is enough for now since we don't look for specific multiple values
      (values.isEmpty || values.contains(foundValues))
  }

  type WarningAndReplacement = (String, DirectiveTemplate)

  private def keyReplacement(replacement: String)(warning: String): WarningAndReplacement =
    (warning, DirectiveTemplate(Seq(replacement), None))

  private def valueReplacement(replacements: String*)(warning: String): WarningAndReplacement =
    (warning, DirectiveTemplate(Nil, Some(replacements.toSeq)))

  private def allAliasesOf(key: String, handler: DirectiveHandler[_]): Seq[String] =
    handler.keys.find(_.nameAliases.contains(key))
      .toSeq
      .flatMap(_.nameAliases)

  private def allKeysFrom(handler: DirectiveHandler[_]): Seq[String] =
    handler.keys.flatMap(_.nameAliases)

  private val deprecatedCombinationsAndReplacements = Map[DirectiveTemplate, WarningAndReplacement](
    DirectiveTemplate(Seq("lib"), None) -> keyReplacement("dep")(deprecatedWarning("lib", "dep")),
    DirectiveTemplate(Seq("lib"), None) -> keyReplacement("dep")(deprecatedWarning("libs", "dep")),
    DirectiveTemplate(Seq("compileOnly.lib"), None) -> keyReplacement("compileOnly.dep")(
      deprecatedWarning("compileOnly.lib", "compileOnly.dep")
    ),
    DirectiveTemplate(Seq("compileOnly.libs"), None) -> keyReplacement("compileOnly.dep")(
      deprecatedWarning("compileOnly.libs", "compileOnly.dep")
    ),
    DirectiveTemplate(
      allKeysFrom(directives.Toolkit.handler),
      Some(Seq("latest"))
    ) -> valueReplacement("default")(deprecatedWarning("latest", "default")),
    DirectiveTemplate(
      allKeysFrom(directives.Toolkit.handler),
      Some(Seq(s"${Toolkit.typelevel}:latest"))
    ) -> valueReplacement(s"${Toolkit.typelevel}:default")(deprecatedWarning("latest", "default"))
  )

  def warningAndReplacement(directive: StrictDirective): Option[WarningAndReplacement] =
    deprecatedCombinationsAndReplacements
      .find(_._1.appliesTo(directive.key, directive.toStringValues))
      .map(_._2) // grab WarningAndReplacement

  def issueWarnings(
    path: Either[String, os.Path],
    directives: Seq[StrictDirective],
    logger: Logger
  ) =
    directives.map(d => d -> warningAndReplacement(d))
      .foreach {
        case (directive, Some(warning, replacement)) =>
          val newKey    = replacement.keys.headOption.getOrElse(directive.key)
          val newValues = replacement.values.getOrElse(directive.toStringValues)
          val newText   = s"$newKey ${newValues.mkString(" ")}"

          // TODO use key and/or value positions instead of whole directive
          val position = directive.position(path)

          val diagnostic = DeprecatedWarning(
            warning,
            Seq(position),
            Some(TextEdit(s"Change to: $newText", newText))
          )
          logger.log(Seq(diagnostic))
        case _ => ()
      }

}
