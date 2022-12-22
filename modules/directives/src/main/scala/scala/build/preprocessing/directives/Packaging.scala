package scala.build.preprocessing.directives

import dependency.parser.ModuleParser

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.directives.*
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  MalformedInputError,
  ModuleFormatError
}
import scala.build.options.{
  BuildOptions,
  JavaOpt,
  PackageOptions,
  PackageType,
  PostBuildOptions,
  ShadowingSeq
}
import scala.build.{Logger, Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Packaging")
@DirectivePrefix("packaging.")
@DirectiveExamples("//> using packaging.packageType \"assembly\"")
@DirectiveExamples("//> using packaging.output \"foo\"")
@DirectiveExamples("//> using packaging.provided \"org.apache.spark::spark-sql\"")
@DirectiveUsage(
  """using packaging.packageType [package type]
    |using packaging.output [destination path]
    |using packaging.provided [module]
    |""".stripMargin,
  """`//> using packaging.packageType `"package type"
    |
    |`//> using packaging.output `"destination path"
    |
    |""".stripMargin
)
@DirectiveDescription("Set parameters for packaging")
@DirectiveLevel(SpecificationLevel.RESTRICTED)
// format: off
final case class Packaging(
  packageType: Option[Positioned[String]] = None,
  output: Option[String] = None,
  provided: List[Positioned[String]] = Nil
) extends HasBuildOptions {
  // format: on
  def buildOptions: Either[BuildException, BuildOptions] = either {
    val maybePackageTypeOpt = packageType
      .map { input =>
        PackageType.parse(input.value).toRight {
          new MalformedInputError(
            "package-type",
            input.value,
            PackageType.mapping.map(_._1).mkString("|"),
            positions = input.positions
          )
        }
      }
      .sequence
    val maybeOutput = output
      .map { path =>
        try Right(os.Path(path, os.pwd)) // !!!
        catch {
          case e: IllegalArgumentException =>
            Left(???)
        }
      }
      .sequence
    val maybeProvided = provided
      .map { input =>
        ModuleParser.parse(input.value)
          .left.map { err =>
            new ModuleFormatError(input.value, err, positions = input.positions)
          }
      }
      .sequence
      .left.map(CompositeBuildException(_))

    val (packageTypeOpt, output0, provided0) = value {
      (maybePackageTypeOpt, maybeOutput, maybeProvided)
        .traverseN
        .left.map(CompositeBuildException(_))
    }

    BuildOptions(
      notForBloopOptions = PostBuildOptions(
        packageOptions = PackageOptions(
          packageTypeOpt = packageTypeOpt,
          output = output0.map(_.toString),
          provided = provided0
        )
      )
    )
  }
}

object Packaging {
  val handler: DirectiveHandler[Packaging] = DirectiveHandler.derive
}
