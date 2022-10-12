package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.Ops._
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  MalformedInputError,
  ModuleFormatError,
  UnexpectedDirectiveError
}
import scala.build.options.{BuildOptions, PackageOptions, PackageType, PostBuildOptions}

case object UsingPackagingDirectiveHandler extends UsingDirectiveHandler {

  def name        = "Packaging"
  def description = "Set parameters for packaging"
  def usage =
    """using packaging.packageType [package type]
      |using packaging.output [destination path]
      |using packaging.provided [module]
      |""".stripMargin

  override def usageMd =
    """`//> using packaging.packageType `"package type"
      |
      |`//> using packaging.output `"destination path"
      |
      |""".stripMargin

  override def examples = Seq(
    "//> using packaging.packageType \"assembly\"",
    "//> using packaging.output \"foo\"",
    "//> using packaging.provided \"org.apache.spark::spark-sql\""
  )

  override def scalaSpecificationLevel = SpecificationLevel.RESTRICTED

  def keys = Seq(
    "packageType",
    "output",
    "provided"
  ).map("packaging." + _)

  override def getValueNumberBounds(key: String) =
    key match {
      case "packaging.provided" =>
        UsingDirectiveValueNumberBounds(1, Int.MaxValue)
      case _ =>
        UsingDirectiveValueNumberBounds(1, 1)
    }

  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] = either {

    def getValues = either {
      val groupedScopedValuesContainer = value(checkIfValuesAreExpected(scopedDirective))
      val severalValues = groupedScopedValuesContainer.scopedStringValues.map(_.positioned)
      severalValues
    }

    def getValue = getValues.map(_.head)

    val packageOptions = scopedDirective.directive.key match {
      case "packaging.packageType" =>
        val value0 = value(getValue)
        PackageType.parse(value0.value) match {
          case Some(tpe) =>
            PackageOptions(packageTypeOpt = Some(tpe))
          case None =>
            value(Left(
              new MalformedInputError(
                "package-type",
                value0.value,
                PackageType.mapping.map(_._1).mkString("|"),
                positions = value0.positions
              )
            ))
        }
      case "packaging.output" =>
        val value0 = value(getValue)
        val path   = os.Path(value0.value, os.pwd)
        PackageOptions(output = Option(path.toString))
      case "packaging.provided" =>
        val values0 = value(getValues)
        val modules = value {
          values0
            .map { posStr =>
              dependency.parser.ModuleParser.parse(posStr.value)
                .left.map { err =>
                  new ModuleFormatError(posStr.value, err, positions = posStr.positions)
                }
            }
            .sequence
            .left.map(CompositeBuildException(_))
        }
        PackageOptions(provided = modules)
      case other =>
        value(Left(new UnexpectedDirectiveError(other)))
    }

    val options = BuildOptions(
      notForBloopOptions = PostBuildOptions(
        packageOptions = packageOptions
      )
    )

    ProcessedDirective(Some(options), Seq.empty)
  }
}
