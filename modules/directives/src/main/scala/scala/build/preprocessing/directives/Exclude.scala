package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.directives._
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, InternalOptions}
import scala.build.{Positioned, options}
import scala.cli.commands.SpecificationLevel
import scala.util.Try

@DirectiveGroupName("Exclude sources")
@DirectiveExamples("//> using exclude utils.scala")
@DirectiveExamples("//> using exclude examples/* */resources/*")
@DirectiveExamples("//> using exclude *.sc")
@DirectiveUsage(
  "`//> using exclude `_pattern_ | `//> using exclude `_pattern_ _pattern_ …",
  """`//> using exclude` _pattern_
    |
    |`//> using exclude` _pattern1_ _pattern2_ …
    |""".stripMargin
)
@DirectiveDescription("Exclude sources from the project")
@DirectiveLevel(SpecificationLevel.SHOULD)
final case class Exclude(exclude: List[Positioned[String]] = Nil) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] = either {
    BuildOptions(
      internal = InternalOptions(
        exclude = exclude
      )
    )
  }
}

object Exclude {
  val handler: DirectiveHandler[Exclude] = DirectiveHandler.derive
}
