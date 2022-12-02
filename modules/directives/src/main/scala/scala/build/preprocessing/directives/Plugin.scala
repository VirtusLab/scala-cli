package scala.build.preprocessing.directives

import dependency.AnyDependency
import dependency.parser.DependencyParser

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.directives.*
import scala.build.errors.{BuildException, CompositeBuildException, DependencyFormatError}
import scala.build.options.{BuildOptions, ScalaOptions}
import scala.build.preprocessing.ScopePath
import scala.build.{Logger, Positioned}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Compiler plugins")
@DirectiveExamples("//> using plugin \"org.typelevel:::kind-projector:0.13.2\"")
@DirectiveUsage(
  "//> using plugin \"org:name:ver\" | //> using plugins \"org:name:ver\", \"org2:name2:ver2\"",
  "`using plugin `_org_`:`name`:`ver"
)
@DirectiveDescription("Adds compiler plugins")
@DirectiveLevel(SpecificationLevel.MUST)
final case class Plugin(
  @DirectiveName("plugins")
  plugin: List[Positioned[String]] = Nil
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] = either {
    val maybeDependencies = plugin
      .map { posStr =>
        posStr
          .map { str =>
            DependencyParser.parse(str)
              .left.map(err => new DependencyFormatError(str, err))
          }
          .eitherSequence
      }
      .sequence
      .left.map(CompositeBuildException(_))
    val dependencies = value(maybeDependencies)
    BuildOptions(
      scalaOptions = ScalaOptions(
        compilerPlugins = dependencies
      )
    )
  }
}

object Plugin {
  val handler: DirectiveHandler[Plugin] = DirectiveHandler.derive
}
