package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, JavaOpt, ShadowingSeq}
import scala.build.{Logger, Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Javac options")
@DirectiveExamples("//> using javacOpt \"source\", \"1.8\", \"target\", \"1.8\"")
@DirectiveUsage(
  "//> using javac-opt _options_ | //> using javacOpt _options_",
  """`//> using javac-opt `_options_
    |
    |`//> using javacOpt `_options_""".stripMargin
)
@DirectiveDescription("Add Javac options which will be passed when compiling sources.")
@DirectiveLevel(SpecificationLevel.SHOULD)
// format: off
final case class JavacOptions(
  @DirectiveName("javacOpt")
    javacOptions: List[Positioned[String]] = Nil
) extends HasBuildOptions {
  // format: on
  def buildOptions: Either[BuildException, BuildOptions] = {
    val buildOpt = BuildOptions(
      javaOptions = options.JavaOptions(
        javacOptions = javacOptions
      )
    )
    Right(buildOpt)
  }
}

object JavacOptions {
  val handler: DirectiveHandler[JavacOptions] = DirectiveHandler.derive
}
