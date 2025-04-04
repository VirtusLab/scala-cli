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
import scala.build.options._
import scala.build.options.packaging.{DockerOptions, NativeImageOptions}
import scala.build.{Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Packaging")
@DirectivePrefix("packaging.")
@DirectiveExamples("//> using packaging.packageType assembly")
@DirectiveExamples("//> using packaging.output foo")
@DirectiveExamples("//> using packaging.provided org.apache.spark::spark-sql")
@DirectiveExamples("//> using packaging.graalvmArgs --no-fallback")
@DirectiveExamples("//> using packaging.dockerFrom openjdk:11")
@DirectiveExamples("//> using packaging.dockerImageTag 1.0.0")
@DirectiveExamples("//> using packaging.dockerImageRegistry virtuslab")
@DirectiveExamples("//> using packaging.dockerImageRepository scala-cli")
@DirectiveExamples("//> using packaging.dockerCmd sh")
@DirectiveExamples("//> using packaging.dockerCmd node")
@DirectiveUsage(
  """using packaging.packageType [package type]
    |using packaging.output [destination path]
    |using packaging.provided [module]
    |using packaging.graalvmArgs [args]
    |using packaging.dockerFrom [base docker image]
    |using packaging.dockerImageTag [image tag]
    |using packaging.dockerImageRegistry [image registry]
    |using packaging.dockerImageRepository [image repository]
    |using packaging.dockerCmd [docker command]
    |""".stripMargin,
  """`//> using packaging.packageType` _package-type_
    |
    |`//> using packaging.output` _destination-path_
    |
    |`//> using packaging.provided` _module_
    |
    |`//> using packaging.graalvmArgs` _args_
    |
    |`//> using packaging.dockerFrom` _base-docker-image_
    |
    |`//> using packaging.dockerImageTag` _image-tag_
    |
    |`//> using packaging.dockerImageRegistry` _image-registry_
    |
    |`//> using packaging.dockerImageRepository` _image-repository_
    |
    |`//> using packaging.dockerCmd` _docker-command_
    |
    |""".stripMargin
)
@DirectiveDescription("Set parameters for packaging")
@DirectiveLevel(SpecificationLevel.RESTRICTED)
final case class Packaging(
  packageType: Option[Positioned[String]] = None,
  output: Option[String] = None,
  provided: List[Positioned[String]] = Nil,
  graalvmArgs: List[Positioned[String]] = Nil,
  dockerFrom: Option[String] = None,
  dockerImageTag: Option[String] = None,
  dockerImageRegistry: Option[String] = None,
  dockerImageRepository: Option[String] = None,
  dockerCmd: Option[String] = None
) extends HasBuildOptions {
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
      internal = InternalOptions(
        keepResolution = provided0.nonEmpty || packageTypeOpt.contains(PackageType.Spark)
      ),
      notForBloopOptions = PostBuildOptions(
        packageOptions = PackageOptions(
          packageTypeOpt = packageTypeOpt,
          output = output0.map(_.toString),
          provided = provided0,
          dockerOptions = DockerOptions(
            from = dockerFrom,
            imageRegistry = dockerImageRegistry,
            imageRepository = dockerImageRepository,
            imageTag = dockerImageTag,
            cmd = dockerCmd
          ),
          nativeImageOptions = NativeImageOptions(
            graalvmArgs = graalvmArgs
          )
        )
      )
    )
  }
}

object Packaging {
  val handler: DirectiveHandler[Packaging] = DirectiveHandler.derive
}
