package scala.build.preprocessing.directives

import java.nio.file.Paths

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.directives.*
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  NotADirectoryError,
  NotAFileError,
  WrongSourcePathError
}
import scala.build.options.{
  BuildOptions,
  InternalOptions,
  SourceGeneratorConfig,
  SourceGeneratorOptions
}
import scala.build.preprocessing.ScopePath
import scala.build.{Named, Positioned}
import scala.cli.commands.SpecificationLevel
import scala.util.Try

@DirectiveGroupName("Source generators")
@DirectivePrefix("sourceGenerator.")
@DirectiveExamples("//> using sourceGenerator.[hello].input ${.}/in")
@DirectiveExamples("//> using sourceGenerator.[hello].output ${.}/out")
@DirectiveExamples("//> using sourceGenerator.[hello].glob *.txt")
@DirectiveExamples("//> using sourceGenerator.[hello].command python ${.}/gen/hello.py")
@DirectiveExamples("//> using sourceGenerator.[hello].unmanaged ${.}/gen/hello.py")
@DirectiveUsage(
  """using sourceGenerator.[name].input <directory>
    |using sourceGenerator.[name].output <directory>
    |using sourceGenerator.[name].glob <globs>
    |using sourceGenerator.[name].command <command>
    |using sourceGenerator.[name].unmanaged <files>
    |""".stripMargin,
  """`//> using sourceGenerator.[`_name_`].input` _directory_
    |
    |`//> using sourceGenerator.[`_name_`].output` _directory_
    |
    |`//> using sourceGenerator.[`_name_`].glob` _glob_
    |
    |`//> using sourceGenerator.[`_name_`].globs` _glob1_ _glob2_ …
    |
    |`//> using sourceGenerator.[`_name_`].command` _command_
    |
    |`//> using sourceGenerator.[`_name_`].unmanaged` _file_
    |
    |`//> using sourceGenerator.[`_name_`].unmanaged` _file1_ _file2_ …
    |
    |""".stripMargin
)
@DirectiveDescription("Configure source generators")
@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)
final case class SourceGenerator(
  command: Named[List[String]] = Named.none(Nil),
  input: Named[DirectiveValueParser.WithScopePath[Option[Positioned[String]]]] =
    Named.none(DirectiveValueParser.WithScopePath.empty(None)),
  output: Named[DirectiveValueParser.WithScopePath[Option[Positioned[String]]]] =
    Named.none(DirectiveValueParser.WithScopePath.empty(None)),
  @DirectiveName("globs")
  glob: Named[List[String]] = Named.none(Nil),
  unmanaged: Named[DirectiveValueParser.WithScopePath[List[Positioned[String]]]] =
    Named.none(DirectiveValueParser.WithScopePath.empty(Nil))
) extends HasBuildOptions {

  private def resolve(cwd: ScopePath, s: Positioned[String]): Either[BuildException, os.Path] =
    for {
      root <- Directive.osRoot(cwd, s.positions.headOption)
      res <- Try(os.Path(s.value, root)).toEither
        .left.map(new WrongSourcePathError(s.value, _, s.positions))
    } yield res

  private def resolveDir(
    path: DirectiveValueParser.WithScopePath[Option[Positioned[String]]]
  ): Either[BuildException, Option[os.Path]] =
    path.value.map { s =>
      resolve(path.scopePath, s)
        .filterOrElse(os.isDir(_), new NotADirectoryError(s.value, s.positions))
    }.sequence

  private def resolveFiles(
    path: DirectiveValueParser.WithScopePath[List[Positioned[String]]]
  ): Either[BuildException, List[os.Path]] =
    path.value.map { s =>
      resolve(path.scopePath, s)
        .filterOrElse(os.isFile(_), new NotAFileError(s.value, s.positions))
    }.sequence
      .left.map(CompositeBuildException(_))
      .map(_.toList)

  def buildOptions: Either[BuildException, BuildOptions] = either {
    val configs =
      Seq[Named[SourceGeneratorConfig]](
        command.map(v => SourceGeneratorConfig(command = v)),
        input.map(v => SourceGeneratorConfig(inputDir = value(resolveDir(v)))),
        output.map(v => SourceGeneratorConfig(outputDir = value(resolveDir(v)))),
        glob.map(v => SourceGeneratorConfig(glob = v)),
        unmanaged.map(v => SourceGeneratorConfig(unmanaged = value(resolveFiles(v))))
      ).flatten.toMap

    val options =
      BuildOptions(sourceGeneratorOptions = SourceGeneratorOptions(configs = configs))

    options
  }
}

object SourceGenerator {
  val handler: DirectiveHandler[SourceGenerator] = DirectiveHandler.derive
}
