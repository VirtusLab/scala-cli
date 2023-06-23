package scala.build

import scala.build.options.{BuildOptions, HasScope, Scope}
import scala.build.preprocessing.ScriptPreprocessor

/** Information gathered from preprocessing command inputs - sources (including unwrapped scripts)
  * and build options from using directives. Only scope requirements remain in this object after
  * resolving them in [[CrossSources.scopedSources]]
  *
  * @param paths
  *   paths and relative paths to sources on disk with the scope they belong to
  * @param inMemory
  *   in memory sources (e.g. snippets) with the scope they belong to
  * @param defaultMainClass
  * @param resourceDirs
  * @param buildOptions
  *   build options sources with the scope they belong to
  * @param unwrappedScripts
  *   in memory script sources with the scope they belong to, their code must be wrapped before
  *   compiling
  */

final case class ScopedSources(
  paths: Seq[HasScope[(os.Path, os.RelPath)]],
  inMemory: Seq[HasScope[Sources.InMemory]],
  defaultMainClass: Option[String],
  resourceDirs: Seq[HasScope[os.Path]],
  buildOptions: Seq[HasScope[BuildOptions]],
  unwrappedScripts: Seq[HasScope[Sources.UnwrappedScript]]
) {
  def buildOptionsFor(scope: Scope): Seq[BuildOptions] =
    scope match {
      case Scope.Test => buildOptions.flatMap(_.valueFor(Scope.Test).toSeq) ++
          buildOptions.flatMap(_.valueFor(Scope.Main).toSeq)
      case _ => buildOptions.flatMap(_.valueFor(scope).toSeq)
    }

  def sources(scope: Scope, baseOptions: BuildOptions): Sources =
    val combinedBuildOptions = buildOptionsFor(scope)
      .foldRight(baseOptions)(_ orElse _)

    val codeWrapper = ScriptPreprocessor.getScriptWrapper(combinedBuildOptions)

    val wrappedScripts = unwrappedScripts
      .flatMap(_.valueFor(scope).toSeq)
      .map(_.wrap(codeWrapper))

    Sources(
      paths.flatMap(_.valueFor(scope).toSeq),
      inMemory.flatMap(_.valueFor(scope).toSeq) ++ wrappedScripts,
      defaultMainClass,
      resourceDirs.flatMap(_.valueFor(scope).toSeq),
      combinedBuildOptions
    )
}
