package scala.build.preprocessing

import scala.build.Logger
import scala.build.errors.Diagnostic.TextEdit
import scala.build.internal.Constants
import scala.build.internal.util.WarningMessages.{
  deprecatedToolkitLatest,
  deprecatedWarning,
  deprecatedWarningForRemoval
}
import scala.build.options.SuppressWarningOptions
import scala.build.preprocessing.directives.{DirectiveHandler, StrictDirective, Toolkit}
import scala.build.warnings.DeprecatedWarning

object DeprecatedDirectives {

  /** Used to represent a general form of a deprecated directive, and its replacement
    * @param keys
    *   representation of deprecated keys
    * @param values
    *   representation of deprecated value
    */
  private case class DirectiveTemplate(keys: Seq[String], values: Option[Seq[String]]) {
    def appliesTo(foundKey: String, foundValues: Seq[String]): Boolean =
      (keys.isEmpty || keys.contains(foundKey)) &&
      // FIXME values.contains is not perfect, but is enough for now since we don't look for specific multiple values
      (values.isEmpty || values.contains(foundValues))
  }

  private type WarningAndReplacement = (String, Option[DirectiveTemplate])

  private def keyReplacement(replacement: String)(warning: String): WarningAndReplacement =
    (warning, Some(DirectiveTemplate(Seq(replacement), None)))

  private def valueReplacement(replacements: String*)(warning: String): WarningAndReplacement =
    (warning, Some(DirectiveTemplate(Nil, Some(replacements.toSeq))))

  private def noReplacement(warning: String): WarningAndReplacement =
    (warning, None)

  private def allKeysFrom(handler: DirectiveHandler[?]): Seq[String] =
    handler.keys.flatMap(_.nameAliases)

  private val deprecatedCombinationsAndReplacements = Map[DirectiveTemplate, WarningAndReplacement](
    DirectiveTemplate(Seq("lib"), None)  -> keyReplacement("dep")(deprecatedWarning("lib", "dep")),
    DirectiveTemplate(Seq("libs"), None) -> keyReplacement("dep")(deprecatedWarning(
      "libs",
      "deps"
    )),
    DirectiveTemplate(Seq("compileOnly.lib"), None) -> keyReplacement("compileOnly.dep")(
      deprecatedWarning("compileOnly.lib", "compileOnly.dep")
    ),
    DirectiveTemplate(Seq("compileOnly.libs"), None) -> keyReplacement("compileOnly.dep")(
      deprecatedWarning("compileOnly.libs", "compileOnly.deps")
    ),
    DirectiveTemplate(
      allKeysFrom(directives.Toolkit.handler),
      Some(Seq("latest"))
    ) -> valueReplacement("default")(deprecatedToolkitLatest()),
    DirectiveTemplate(
      allKeysFrom(directives.Toolkit.handler),
      Some(Seq(s"${Toolkit.typelevel}:latest"))
    ) -> valueReplacement(s"${Toolkit.typelevel}:default")(
      deprecatedToolkitLatest()
    ),
    DirectiveTemplate(
      allKeysFrom(directives.Toolkit.handler),
      Some(Seq(s"${Constants.typelevelOrganization}:latest"))
    ) -> valueReplacement(s"${Toolkit.typelevel}:default")(
      deprecatedToolkitLatest()
    ),
    DirectiveTemplate(Seq("deprecatedTestDirective"), None) -> keyReplacement("testDirective")(
      deprecatedWarning("deprecatedTestDirective", "testDirective")
    ),
    DirectiveTemplate(Seq("deprecatedForRemovalTestDirective"), None) -> noReplacement(
      deprecatedWarningForRemoval("deprecatedForRemovalTestDirective")
    )
  )

  private def warningAndReplacement(directive: StrictDirective): Option[WarningAndReplacement] =
    deprecatedCombinationsAndReplacements
      .find(_._1.appliesTo(directive.key, directive.toStringValues))
      .map(_._2) // grab WarningAndReplacement

  def issueWarnings(
    path: Either[String, os.Path],
    directives: Seq[StrictDirective],
    suppressWarningOptions: SuppressWarningOptions,
    logger: Logger
  ): Unit =
    if !suppressWarningOptions.suppressDeprecatedFeatureWarning.getOrElse(false) then
      directives.map(d => d -> warningAndReplacement(d))
        .foreach {
          case (directive, Some(warning, replacementOpt)) =>
            val position    = directive.position(path)
            val textEditOpt = replacementOpt.map { replacement =>
              val newKey    = replacement.keys.headOption.getOrElse(directive.key)
              val newValues = replacement.values.getOrElse(directive.toStringValues)
              val newText   = s"$newKey ${newValues.mkString(" ")}"
              TextEdit(s"Change to: $newText", newText)
            }
            val diagnostic = DeprecatedWarning(warning, Seq(position), textEditOpt)
            logger.log(Seq(diagnostic))
          case _ => ()
        }

}
