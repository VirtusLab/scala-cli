package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException, UnexpectedDirectiveError}
import scala.build.options.publish.{Developer, License, Vcs}
import scala.build.options.{BuildOptions, PostBuildOptions, PublishOptions}

case object UsingPublishDirectiveHandler extends UsingDirectiveHandler {

  private def prefix = "publish."

  def name        = "Publish"
  def description = "Set parameters for publishing"
  def usage       = s"//> using $prefix(organization|name|version) [value]"

  override def usageMd =
    s"""`//> using ${prefix}organization `"value"
       |`//> using ${prefix}name `"value"
       |`//> using ${prefix}version `"value"
       |""".stripMargin

  private def q = "\""
  override def examples = Seq(
    s"//> using ${prefix}organization ${q}io.github.myself$q",
    s"//> using ${prefix}name ${q}my-library$q",
    s"//> using ${prefix}version ${q}0.1.1$q"
  )
  override def isRestricted = false

  def keys = Seq(
    "organization",
    "name",
    "moduleName",
    "version",
    "url",
    "license",
    "versionControl",
    "version-control",
    "scm",
    "description",
    "developer",
    "scalaVersionSuffix",
    "scala-version-suffix",
    "scalaPlatformSuffix",
    "scala-platform-suffix"
  ).map(prefix + _)

  override def getValueNumberBounds(key: String) = key match {
    case _ => UsingDirectiveValueNumberBounds(1, 1)
  }

  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] = either {

    val groupedScopedValuesContainer = value(checkIfValuesAreExpected(scopedDirective))

    val severalValues = groupedScopedValuesContainer.scopedStringValues.map(_.positioned)
    val singleValue   = severalValues.head

    val strippedKey =
      if (scopedDirective.directive.key.startsWith(prefix))
        scopedDirective.directive.key.stripPrefix(prefix)
      else
        value(Left(new UnexpectedDirectiveError(scopedDirective.directive.key)))

    val publishOptions = strippedKey match {
      case "organization" =>
        PublishOptions(organization = Some(singleValue))
      case "name" =>
        PublishOptions(name = Some(singleValue))
      case "moduleName" | "module-name" =>
        PublishOptions(moduleName = Some(singleValue))
      case "version" =>
        PublishOptions(version = Some(singleValue))
      case "url" =>
        PublishOptions(url = Some(singleValue))
      case "license" =>
        value {
          License.parse(singleValue).map { license =>
            PublishOptions(license = Some(license))
          }
        }
      case "versionControl" | "version-control" | "scm" =>
        value {
          Vcs.parse(singleValue).map { versionControl =>
            PublishOptions(versionControl = Some(versionControl))
          }
        }
      case "description" =>
        PublishOptions(description = Some(singleValue.value))
      case "developer" =>
        val developers = value {
          severalValues
            .map(Developer.parse)
            .sequence
            .left.map(CompositeBuildException(_))
        }
        PublishOptions(developers = developers)
      case "scalaVersionSuffix" | "scala-version-suffix" =>
        PublishOptions(scalaVersionSuffix = Some(singleValue.value))
      case "scalaPlatformSuffix" | "scala-platform-suffix" =>
        PublishOptions(scalaPlatformSuffix = Some(singleValue.value))
      case _ =>
        value(Left(new UnexpectedDirectiveError(scopedDirective.directive.key)))
    }

    val options = BuildOptions(
      notForBloopOptions = PostBuildOptions(
        publishOptions = publishOptions
      )
    )

    ProcessedDirective(Some(options), Seq.empty)
  }
}
