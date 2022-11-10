package scala.build

import scala.build.options.{BuildOptions, HasScope, Scope}

final case class ScopedSources(
  paths: Seq[HasScope[(os.Path, os.RelPath)]],
  inMemory: Seq[HasScope[Sources.InMemory]],
  defaultMainClass: Option[String],
  resourceDirs: Seq[HasScope[os.Path]],
  buildOptions: Seq[HasScope[BuildOptions]]
) {
  def sources(scope: Scope, baseOptions: BuildOptions): Sources =
    Sources(
      paths.flatMap(_.valueFor(scope).toSeq),
      inMemory.flatMap(_.valueFor(scope).toSeq),
      defaultMainClass,
      resourceDirs.flatMap(_.valueFor(scope).toSeq),
      buildOptions
        .flatMap(_.valueFor(scope).toSeq)
        .foldRight(baseOptions)(_ orElse _)
    )
}
