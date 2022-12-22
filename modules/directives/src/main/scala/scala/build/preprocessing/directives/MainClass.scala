package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, JavaOpt, ShadowingSeq}
import scala.build.{Logger, Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Main class")
@DirectiveExamples("//> using main-class \"helloWorld\"")
@DirectiveUsage(
  "//> using main-class _main class_ | //> using mainClass _main class_",
  """`//> using main-class `_main class_
    |
    |`//> using mainClass `_main class_""".stripMargin
)
@DirectiveDescription("Specify default main class")
@DirectiveLevel(SpecificationLevel.MUST)
// format: off
final case class MainClass(
  mainClass: Option[String] = None
) extends HasBuildOptions {
  // format: on
  def buildOptions: Either[BuildException, BuildOptions] = {
    val buildOpt = BuildOptions(
      mainClass = mainClass
    )
    Right(buildOpt)
  }
}

object MainClass {
  val handler: DirectiveHandler[MainClass] = DirectiveHandler.derive
}
