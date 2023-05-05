package scala.build.directives

import scala.build.Ops.*
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.options.{BuildOptions, Scope, WithBuildRequirements}

trait HasBuildOptionsWithRequirements {
  def buildOptionsList: List[Either[BuildException, WithBuildRequirements[BuildOptions]]]
  final def buildOptionsWithRequirements
    : Either[BuildException, List[WithBuildRequirements[BuildOptions]]] =
    buildOptionsList
      .sequence
      .left.map(CompositeBuildException(_))
      .map(_.toList)
}
