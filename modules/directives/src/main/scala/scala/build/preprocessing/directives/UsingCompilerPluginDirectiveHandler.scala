package scala.build.preprocessing.directives
import dependency.AnyDependency
import dependency.parser.DependencyParser

import scala.build.Ops.EitherSeqOps
import scala.build.errors.{BuildException, CompositeBuildException, DependencyFormatError}
import scala.build.options.{BuildOptions, ScalaOptions}
import scala.build.{Logger, Positioned}

case object UsingCompilerPluginDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Compiler plugins"
  def description = "Adds compiler plugins"
  def usage =
    "//> using plugin \"org:name:ver\" | //> using plugins \"org:name:ver\", \"org2:name2:ver2\""
  override def usageMd = "`using plugin `_org_`:`name`:`ver"
  override def examples = Seq(
    "//> using plugin \"org.typelevel:::kind-projector:0.13.2\""
  )

  private def parseDependency(depStr: String): Either[BuildException, AnyDependency] =
    DependencyParser.parse(depStr)
      .left.map(err => new DependencyFormatError(depStr, err))

  def keys = Seq("plugin", "plugins")

  override def getValueNumberBounds(key: String): UsingDirectiveValueNumberBounds =
    UsingDirectiveValueNumberBounds(1, Int.MaxValue)

  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] =
    checkIfValuesAreExpected(scopedDirective).flatMap { groupedScopedValuesContainer =>
      groupedScopedValuesContainer.scopedStringValues.map {
        case ScopedValue(positioned, _) =>
          // Really necessary? (might already be handled by the coursier-dependency library)
          val dependencyStr = positioned.value.filter(!_.isSpaceChar)

          parseDependency(dependencyStr).map(Positioned(positioned.positions, _))
      }
        .sequence
        .left.map(CompositeBuildException(_))
    }.map {
      extraDependencies =>

        ProcessedDirective(
          Some(BuildOptions(
            scalaOptions = ScalaOptions(
              compilerPlugins = extraDependencies
            )
          )),
          Seq.empty
        )
    }

}
