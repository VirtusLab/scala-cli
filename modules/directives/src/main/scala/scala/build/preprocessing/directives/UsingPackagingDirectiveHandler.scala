package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.{BuildException, MalformedInputError, UnexpectedDirectiveError}
import scala.build.options.{BuildOptions, PackageOptions, PackageType, PostBuildOptions}

case object UsingPackagingDirectiveHandler extends UsingDirectiveHandler {

  def name        = "Packaging"
  def description = "Set parameters for packaging"
  def usage       = "using packaging.packageType [package type]"

  override def usageMd =
    "`using packaging.packageType `\"package type\""

  override def examples = Seq(
    "using packaging.packageType \"assembly\""
  )

  def keys = Seq(
    "packageType"
  ).map("packaging." + _)

  override def getValueNumberBounds(key: String) =
    UsingDirectiveValueNumberBounds(1, 1)

  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] = either {

    def getValue = either {
      val groupedScopedValuesContainer = value(checkIfValuesAreExpected(scopedDirective))
      val severalValues = groupedScopedValuesContainer.scopedStringValues.map(_.positioned)
      severalValues.head
    }

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
