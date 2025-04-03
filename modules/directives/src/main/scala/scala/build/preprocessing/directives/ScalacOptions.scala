package scala.build.preprocessing.directives

import scala.build.Positioned
import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.WithBuildRequirements.*
import scala.build.options.{
  BuildOptions,
  ScalaOptions,
  ScalacOpt,
  Scope,
  ShadowingSeq,
  WithBuildRequirements
}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Compiler options")
@DirectiveExamples("//> using option -Xasync")
@DirectiveExamples("//> using options -Xasync -Xfatal-warnings")
@DirectiveExamples("//> using test.option -Xasync")
@DirectiveExamples("//> using test.options -Xasync -Xfatal-warnings")
@DirectiveUsage(
  "using option _option_ | using options _option1_ _option2_ …",
  """`//> using scalacOption` _option_
    |
    |`//> using option` _option_
    |
    |`//> using scalacOptions` _option1_ _option2_ …
    |
    |`//> using options` _option1_ _option2_ …
    |
    |`//> using test.scalacOption` _option_
    |
    |`//> using test.option` _option_
    |
    |`//> using test.scalacOptions` _option1_ _option2_ …
    |
    |`//> using test.options` _option1_ _option2_ …
    |
    |""".stripMargin
)
@DirectiveDescription("Add Scala compiler options")
@DirectiveLevel(SpecificationLevel.MUST)
final case class ScalacOptions(
  @DirectiveName("option")
  @DirectiveName("scalacOption")
  @DirectiveName("scalacOptions")
  options: List[Positioned[String]] = Nil,
  @DirectiveName("test.option")
  @DirectiveName("test.options")
  @DirectiveName("test.scalacOption")
  @DirectiveName("test.scalacOptions")
  testOptions: List[Positioned[String]] = Nil
) extends HasBuildOptionsWithRequirements {
  def buildOptionsList: List[Either[BuildException, WithBuildRequirements[BuildOptions]]] = List(
    ScalacOptions.buildOptions(options).map(_.withEmptyRequirements),
    ScalacOptions.buildOptions(testOptions).map(_.withScopeRequirement(Scope.Test))
  )
}

object ScalacOptions {
  val handler: DirectiveHandler[ScalacOptions] = DirectiveHandler.derive
  def buildOptions(options: List[Positioned[String]]): Either[BuildException, BuildOptions] =
    Right {
      BuildOptions(
        scalaOptions = ScalaOptions(
          scalacOptions = ShadowingSeq.from(options.map(_.map(ScalacOpt(_))))
        )
      )
    }
}
