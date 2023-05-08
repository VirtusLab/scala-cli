package scala.build.preprocessing.directives

import scala.build.Position
import scala.build.options.{BuildOptions, BuildRequirements, WithBuildRequirements}
import scala.build.preprocessing.Scoped

case class PreprocessedDirectives(
  globalReqs: BuildRequirements,
  globalUsings: BuildOptions,
  usingsWithReqs: List[WithBuildRequirements[BuildOptions]],
  scopedReqs: Seq[Scoped[BuildRequirements]],
  strippedContent: Option[String],
  directivesPositions: Option[Position.File]
) {
  def isEmpty: Boolean = globalReqs == BuildRequirements.monoid.zero &&
    globalUsings == BuildOptions.monoid.zero &&
    scopedReqs.isEmpty &&
    strippedContent.isEmpty &&
    usingsWithReqs.isEmpty
}

object PreprocessedDirectives {
  def empty: PreprocessedDirectives =
    PreprocessedDirectives(
      globalReqs = BuildRequirements.monoid.zero,
      globalUsings = BuildOptions.monoid.zero,
      usingsWithReqs = Nil,
      scopedReqs = Nil,
      strippedContent = None,
      directivesPositions = None
    )
}
