package scala.build.preprocessing.directives

import scala.cli.commands.SpecificationLevel
import scala.build.directives.*
import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.options.{BuildOptions, SourceGeneratorOptions, GeneratorConfig}
import scala.build.options.GeneratorConfig
import scala.build.{Positioned, options}

@DirectiveUsage("//> using sourceGenerator", "`//> using sourceGenerator`")
@DirectiveDescription("Generate code using Source Generator")
@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)
final case class SourceGenerator(
  sourceGenerator: DirectiveValueParser.WithScopePath[List[Positioned[String]]] =
    DirectiveValueParser.WithScopePath.empty(Nil)
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] =
    SourceGenerator.buildOptions(sourceGenerator)
}

object SourceGenerator {
  val handler: DirectiveHandler[SourceGenerator] = DirectiveHandler.derive
  def buildOptions(sourceGenerator: DirectiveValueParser.WithScopePath[List[Positioned[String]]])
    : Either[BuildException, BuildOptions] = {
    val sourceGenValue = sourceGenerator.value
    sourceGenValue
      .map(config => GeneratorConfig.parse(config, sourceGenerator.scopePath.subPath))
      .sequence
      .left.map(CompositeBuildException(_))
      .map { configs =>
        BuildOptions(sourceGeneratorOptions =
          SourceGeneratorOptions(generatorConfig = configs)
        )
      }
  }
}
