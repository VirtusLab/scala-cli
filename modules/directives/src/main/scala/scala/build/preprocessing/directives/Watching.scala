package scala.build.preprocessing.directives

import scala.build.Positioned
import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, WatchOptions}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Watch additional inputs")
@DirectiveExamples("//> using watching ./data")
@DirectiveUsage(
  """//> using watching _path_
    |
    |//> using watching _path1_ _path2_ …""".stripMargin,
  """`//> using watching` _path_
    |
    |`//> using watching` _path1_ _path2_ …
    |
    |""".stripMargin
)
@DirectiveDescription("Watch additional files or directories when using watch mode")
@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)
final case class Watching(
  watching: DirectiveValueParser.WithScopePath[List[Positioned[String]]] =
    DirectiveValueParser.WithScopePath.empty(Nil)
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] =
    Watching.buildOptions(watching)
}

object Watching {
  val handler: DirectiveHandler[Watching] = DirectiveHandler.derive

  def buildOptions(
    watching: DirectiveValueParser.WithScopePath[List[Positioned[String]]]
  ): Either[BuildException, BuildOptions] = Right {
    val paths         = watching.value.map(_.value)
    val (_, rootOpt)  = Directive.osRootResource(watching.scopePath)
    val resolvedPaths = rootOpt.toList.flatMap { root =>
      paths.map(os.Path(_, root))
    }
    BuildOptions(
      watchOptions = WatchOptions(
        extraWatchPaths = resolvedPaths
      )
    )
  }
}
