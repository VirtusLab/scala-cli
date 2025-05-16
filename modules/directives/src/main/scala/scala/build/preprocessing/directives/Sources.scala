package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.directives.*
import scala.build.errors.{BuildException, CompositeBuildException, WrongSourcePathError}
import scala.build.options.{BuildOptions, InternalOptions}
import scala.build.{Positioned, options}
import scala.cli.commands.SpecificationLevel
import scala.util.Try

@DirectiveGroupName("Custom sources")
@DirectiveExamples("//> using file utils.scala")
@DirectiveExamples(
  "//> using file https://raw.githubusercontent.com/softwaremill/sttp/refs/heads/master/examples/src/main/scala/sttp/client4/examples/json/GetAndParseJsonCatsEffectCirce.scala"
)
@DirectiveUsage(
  "`//> using file `_path_ | `//> using files `_path1_ _path2_ …",
  """`//> using file` _path_
    |
    |`//> using files` _path1_ _path2_ …
    |""".stripMargin
)
@DirectiveDescription(
  "Manually add sources to the project. Does not support chaining, sources are added only once, not recursively."
)
@DirectiveLevel(SpecificationLevel.SHOULD)
final case class Sources(
  @DirectiveName("file")
  files: DirectiveValueParser.WithScopePath[List[Positioned[String]]] =
    DirectiveValueParser.WithScopePath.empty(Nil)
) extends HasBuildOptions {

  private def codeFile(codeFile: String, root: os.Path): Sources.CodeFile =
    scala.util.Try {
      val uri = java.net.URI.create(codeFile)
      uri.getScheme match {
        case "file" | "http" | "https" => uri
      }
    }.getOrElse {
      os.Path(codeFile, root)
    }

  def buildOptions: Either[BuildException, BuildOptions] = either {

    val paths = files
      .value
      .map { positioned =>
        for {
          root <- Directive.osRoot(files.scopePath, positioned.positions.headOption)
          path <- {
            try Right(positioned.map(codeFile(_, root)))
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

  type CodeFile = os.Path | java.net.URI

  val handler: DirectiveHandler[Sources] = DirectiveHandler.derive
}
