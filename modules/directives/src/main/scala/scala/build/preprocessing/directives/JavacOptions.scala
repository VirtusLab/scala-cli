package scala.build.preprocessing.directives

import scala.build.directives._
import scala.build.errors.BuildException
import scala.build.options.WithBuildRequirements._
import scala.build.options.{BuildOptions, Scope, WithBuildRequirements}
import scala.build.preprocessing.directives.DirectiveUtil._
import scala.build.{Positioned, options}
import scala.cli.commands.SpecificationLevel
@DirectiveGroupName("Javac options")
@DirectiveExamples("//> using javacOpt -source 1.8 -target 1.8")
@DirectiveExamples("//> using test.javacOpt -source 1.8 -target 1.8")
@DirectiveUsage(
  "//> using javacOpt _options_",
  """`//> using javacOpt` _options_
    |
    |`//> using javacOptions` _options_
    |
    |`//> using test.javacOpt` _options_
    |
    |`//> using test.javacOptions` _options_
    |""".stripMargin
)
@DirectiveDescription("Add Javac options which will be passed when compiling sources.")
@DirectiveLevel(SpecificationLevel.SHOULD)
final case class JavacOptions(
  @DirectiveName("javacOpt")
  javacOptions: List[Positioned[String]] = Nil,
  @DirectiveName("test.javacOptions")
  @DirectiveName("test.javacOpt")
  testJavacOptions: List[Positioned[String]] = Nil
) extends HasBuildOptionsWithRequirements {
  def buildOptionsList: List[Either[BuildException, WithBuildRequirements[BuildOptions]]] = List(
    JavacOptions.buildOptions(javacOptions).map(_.withEmptyRequirements),
    JavacOptions.buildOptions(testJavacOptions).map(_.withScopeRequirement(Scope.Test))
  )
}

object JavacOptions {
  val handler: DirectiveHandler[JavacOptions] = DirectiveHandler.derive
  def buildOptions(javacOptions: List[Positioned[String]]): Either[BuildException, BuildOptions] =
    Right(BuildOptions(javaOptions = options.JavaOptions(javacOptions = javacOptions)))
}
