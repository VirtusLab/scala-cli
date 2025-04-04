package scala.build.preprocessing.directives

import scala.build.Positioned
import scala.build.directives.*
import scala.build.errors.{BuildException, DirectiveErrors, InputsException}
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
@DirectiveExamples("//> using options.preset suggested")
@DirectiveUsage(
  "using option _option_ | using options _option1_ _option2_ …",
  """`//> using scalacOption` _option_
    |`//> using option` _option_
    |`//> using scalacOptions` _option1_ _option2_ …
    |`//> using options` _option1_ _option2_ …
    |
    |`//> using test.scalacOption` _option_
    |`//> using test.option` _option_
    |`//> using test.scalacOptions` _option1_ _option2_ …
    |`//> using test.options` _option1_ _option2_ …
    |
    |`//> using options` _option1_ _option2_ …
    |
    |`//> using options.preset` _suggested_ | _ci_ | _strict_
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
  testOptions: List[Positioned[String]] = Nil,
  @DirectiveName("option.preset")
  @DirectiveName("options.preset")
  presetOptions: Option[String] = None
) extends HasBuildOptionsWithRequirements {
  def buildOptionsList: List[Either[BuildException, WithBuildRequirements[BuildOptions]]] = {
    val explicitScalacOptions = List(
      ScalacOptions.buildOptions(options).map(_.withEmptyRequirements),
      ScalacOptions.buildOptions(testOptions).map(_.withScopeRequirement(Scope.Test))
    )

    // todo: fix: How to get this correctly?
    val scalaVersion = "3.3.4"
    
    //todo: how to get the command line preset option. If it is there in the command line use it, else use from the directive.

    presetOptions match {
      case None => explicitScalacOptions
      case Some(ScalacOpt.PresetOptions.Suggested.preset) =>
        val presetOptions =
          ScalacOptions.buildOptions(
            ScalacOptions.presetOptionsSuggested(scalaVersion).map(Positioned.none)
          )
        explicitScalacOptions :+ presetOptions.map(_.withEmptyRequirements)
      case Some(ScalacOpt.PresetOptions.CI.preset) =>
        val presetOptions =
          ScalacOptions.buildOptions(
            ScalacOptions.presetOptionsCI(scalaVersion).map(Positioned.none)
          )
        explicitScalacOptions :+ presetOptions.map(_.withEmptyRequirements)
      case Some(ScalacOpt.PresetOptions.Strict.preset) =>
        val presetOptions =
          ScalacOptions.buildOptions(
            ScalacOptions.presetOptionsStrict(scalaVersion).map(Positioned.none)
          )
        explicitScalacOptions :+ presetOptions.map(_.withEmptyRequirements)
      case Some(other) =>
        List(Left(
          InputsException(
            s"Unknown preset options: $other. Available options are: ${ScalacOpt.PresetOptions.values.map(_.preset).mkString(", ")}"
          )
        ))
    }
  }
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

  // todo: Is this might not be the correct location to handle this?
  private def presetOptionsSuggested(scalaVersion: String) =
    scalaVersion match {
      case v if v.startsWith("2.12") =>
        List(
          "-encoding",
          "utf8",
          "-deprecation",
          "-feature",
          "-unchecked"
        )
      case v if v.startsWith("2.13") =>
        List(
          "-encoding",
          "utf8",
          "-deprecation",
          "-feature",
          "-unchecked"
        )

      case v if v.startsWith("3.0") =>
        List(
          "-encoding",
          "utf8",
          "-deprecation",
          "-feature",
          "-unchecked"
        )
    }

  private def presetOptionsCI(scalaVersion: String) =
    scalaVersion match {
      case v if v.startsWith("2.12") =>
        List(
          "-encoding",
          "utf8",
          "-deprecation",
          "-feature",
          "-unchecked"
        )
      case v if v.startsWith("2.13") =>
        List(
          "-encoding",
          "utf8",
          "-deprecation",
          "-feature",
          "-unchecked"
        )

      case v if v.startsWith("3.0") =>
        List(
          "-encoding",
          "utf8",
          "-deprecation",
          "-feature",
          "-unchecked"
        )
    }

  private def presetOptionsStrict(scalaVersion: String) =
    scalaVersion match {
      case v if v.startsWith("2.12") =>
        List(
          "-encoding",
          "utf8",
          "-deprecation",
          "-feature",
          "-unchecked"
        )
      case v if v.startsWith("2.13") =>
        List(
          "-encoding",
          "utf8",
          "-deprecation",
          "-feature",
          "-unchecked"
        )

      case v if v.startsWith("3.0") =>
        List(
          "-encoding",
          "utf8",
          "-deprecation",
          "-feature",
          "-unchecked"
        )
    }
}
