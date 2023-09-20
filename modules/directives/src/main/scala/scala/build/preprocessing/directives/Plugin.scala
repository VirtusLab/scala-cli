package scala.build.preprocessing.directives

import dependency.AnyDependency

import scala.build.EitherCps.{either, value}
import scala.build.Positioned
import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScalaOptions}
import scala.build.preprocessing.directives.DirectiveUtil.*
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Compiler plugins")
@DirectiveExamples("//> using plugin org.typelevel:::kind-projector:0.13.2")
@DirectiveUsage(
  "//> using plugin org:name:ver | //> using plugins org:name:ver org2:name2:ver2",
  "`using plugin` _org_`:`_name_`:`_ver_"
)
@DirectiveDescription("Adds compiler plugins")
@DirectiveLevel(SpecificationLevel.MUST)
final case class Plugin(
  @DirectiveName("plugins")
  plugin: List[Positioned[String]] = Nil
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] = either {
    val dependencies: Seq[Positioned[AnyDependency]] = value(plugin.asDependencies)
    BuildOptions(scalaOptions = ScalaOptions(compilerPlugins = dependencies))
  }
}

object Plugin {
  val handler: DirectiveHandler[Plugin] = DirectiveHandler.derive
}
