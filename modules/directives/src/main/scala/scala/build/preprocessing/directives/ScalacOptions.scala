package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScalaOptions, ScalacOpt, ShadowingSeq}
import scala.build.{Logger, Positioned}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Compiler options")
@DirectiveExamples("//> using option \"-Xasync\"")
@DirectiveExamples("//> using options \"-Xasync\", \"-Xfatal-warnings\"")
@DirectiveUsage(
  "using option _option_ | using options _option1_ _option2_ …",
  """`//> using option `_option_
    |
    |`//> using options `_option1_, _option2_ …""".stripMargin
)
@DirectiveDescription("Add Scala compiler options")
@DirectiveLevel(SpecificationLevel.MUST)
// format: off
final case class ScalacOptions(
  @DirectiveName("option")
    options: List[Positioned[String]] = Nil
) extends HasBuildOptions {
  // format: on
  def buildOptions: Either[BuildException, BuildOptions] = {
    val buildOpt = BuildOptions(
      scalaOptions = ScalaOptions(
        scalacOptions = ShadowingSeq.from(options.map(_.map(ScalacOpt(_))))
      )
    )
    Right(buildOpt)
  }
}

object ScalacOptions {
  val handler: DirectiveHandler[ScalacOptions] = DirectiveHandler.derive
}
