package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{
  BuildOptions,
  ScalaOptions,
  ScalacOpt,
  Scope,
  ShadowingSeq,
  WithBuildRequirements
}
import scala.build.preprocessing.directives.ScalacOptions.buildOptions
import scala.build.{Logger, Positioned}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Compiler options")
@DirectiveExamples("//> using option -Xasync")
@DirectiveExamples("//> using test.option -Xasync")
@DirectiveExamples("//> using options -Xasync, -Xfatal-warnings")
@DirectiveUsage(
  "using option _option_ | using options _option1_ _option2_ …",
  """`//> using option `_option_
    |
    |`//> using options `_option1_, _option2_ …""".stripMargin
)
@DirectiveDescription("Add Scala compiler options")
@DirectiveLevel(SpecificationLevel.MUST)
final case class ScalacOptions(
  @DirectiveName("option")
  options: List[Positioned[String]] = Nil,
  @DirectiveName("test.option")
  @DirectiveName("test.options")
  @DirectiveName("test.scalacOptions")
  testOptions: List[Positioned[String]] = Nil
) extends HasBuildOptionsWithRequirements {
  def buildOptionsWithRequirements
    : Either[BuildException, List[WithBuildRequirements[BuildOptions]]] =
    Right(List(
      buildOptions(options).withEmptyRequirements,
      buildOptions(testOptions).withScopeRequirement(Scope.Test)
    ))
}

object ScalacOptions {
  val handler: DirectiveHandler[ScalacOptions] = DirectiveHandler.derive
  def buildOptions(options: List[Positioned[String]]): BuildOptions =
    BuildOptions(
      scalaOptions = ScalaOptions(
        scalacOptions = ShadowingSeq.from(options.map(_.map(ScalacOpt(_))))
      )
    )
}
