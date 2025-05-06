package scala.build.preprocessing.directives

import scala.build.directives._
import scala.build.errors.BuildException
import scala.build.options.BuildOptions
import scala.build.{Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Main class")
@DirectiveExamples("//> using mainClass HelloWorld")
@DirectiveUsage(
  "//> using mainClass _main-class_",
  "`//> using mainClass` _main-class_"
)
@DirectiveDescription("Specify default main class")
@DirectiveLevel(SpecificationLevel.MUST)
final case class MainClass(mainClass: Option[String] = None) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] =
    Right(BuildOptions(mainClass = mainClass))
}

object MainClass {
  val handler: DirectiveHandler[MainClass] = DirectiveHandler.derive
}
