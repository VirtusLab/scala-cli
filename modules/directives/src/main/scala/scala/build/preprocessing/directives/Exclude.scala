package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.directives.*
import scala.build.errors.{BuildException, CompositeBuildException, WrongSourcePathError}
import scala.build.options.{BuildOptions, InternalOptions, JavaOpt, ShadowingSeq}
import scala.build.preprocessing.directives.DirectiveUtil.given
import scala.build.{Logger, Positioned, options}
import scala.cli.commands.SpecificationLevel
import scala.cli.directivehandler.*
import scala.util.Try

@DirectiveGroupName("Exclude sources")
@DirectiveExamples("//> using exclude utils.scala")
@DirectiveUsage(
  "`//> using exclude `_pattern_ | `//> using exclude `_pattern_ _pattern_ …",
  """`//> using exclude "*.sc"`
    |
    |`//> using exclude "examples/*" "*/resources/*" …`""".stripMargin
)
@DirectiveDescription("Exclude sources from the project")
@DirectiveLevel(SpecificationLevel.SHOULD)
// format: off
final case class Exclude(
  exclude: List[Positioned[String]] = Nil
) extends HasBuildOptions {
// format: on
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
