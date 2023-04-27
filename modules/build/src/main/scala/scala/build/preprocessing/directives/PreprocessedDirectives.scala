package scala.build.preprocessing.directives

import scala.build.options.{BuildOptions, BuildRequirements, WithBuildRequirements}
import scala.build.preprocessing.{DirectivesPositions, Scoped}

case class PreprocessedDirectives(
  globalReqs: BuildRequirements,
  globalUsings: BuildOptions,
  usingsWithReqs: List[WithBuildRequirements[BuildOptions]],
  scopedReqs: Seq[Scoped[BuildRequirements]],
  strippedContent: Option[String],
  directivesPositions: Option[DirectivesPositions]
) {
  def isEmpty: Boolean = globalReqs == BuildRequirements.monoid.zero &&
    globalUsings == BuildOptions.monoid.zero &&
    scopedReqs.isEmpty &&
    strippedContent.isEmpty &&
    usingsWithReqs.isEmpty
}
