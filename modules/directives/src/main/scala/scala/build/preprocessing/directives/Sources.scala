package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.directives.*
import scala.build.errors.{BuildException, CompositeBuildException, WrongSourcePathError}
import scala.build.options.{BuildOptions, InternalOptions, JavaOpt, ShadowingSeq}
import scala.build.{Logger, Positioned, options}
import scala.cli.commands.SpecificationLevel
import scala.util.Try

@DirectiveGroupName("Custom sources")
@DirectiveExamples("//> using file \"utils.scala\"")
@DirectiveUsage(
  "`//> using file `_path_ | `//> using files `_path1_, _path2_ …",
  """//> using file hello.sc
    |
    |//> using files Utils.scala, Helper.scala …""".stripMargin
)
@DirectiveDescription("Manually add sources to the project")
@DirectiveLevel(SpecificationLevel.SHOULD)
// format: off
final case class Sources(
  @DirectiveName("file")
    files: DirectiveValueParser.WithScopePath[List[Positioned[String]]] =
      DirectiveValueParser.WithScopePath.empty(Nil)
) extends HasBuildOptions {
  // format: on
  def buildOptions: Either[BuildException, BuildOptions] = either {

    val paths = files
      .value
      .map { positioned =>
        for {
          root <- Directive.osRoot(files.scopePath, positioned.positions.headOption)
          path <- {
            try Right(positioned.map(os.Path(_, root)))
            catch {
              case e: IllegalArgumentException =>
                Left(new WrongSourcePathError(positioned.value, e, positioned.positions))
            }
          }
        } yield path
      }
      .sequence
      .left.map(CompositeBuildException(_))

    BuildOptions(
      internal = InternalOptions(
        extraSourceFiles = value(paths)
      )
    )
  }
}

object Sources {
  val handler: DirectiveHandler[Sources] = DirectiveHandler.derive
}
