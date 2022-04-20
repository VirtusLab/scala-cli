package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.{BuildException, UnexpectedDirectiveError}
import scala.build.options.publish.{ComputeVersion, Developer, License, Vcs}
import scala.build.options.{BuildOptions, PostBuildOptions, PublishOptions}

case object UsingPublishDirectiveHandler extends UsingDirectiveHandler {

  private def prefix = "publish."

  def name        = "Publish"
  def description = "Set parameters for publishing"
  def usage       = s"//> using $prefix(organization|moduleName|version) [value]"

  override def usageMd =
    s"""`//> using ${prefix}organization `"value"
       |`//> using ${prefix}moduleName `"value"
       |`//> using ${prefix}version `"value"
       |""".stripMargin

  private def q = "\""
  override def examples = Seq(
    s"//> using ${prefix}organization ${q}io.github.myself$q",
    s"//> using ${prefix}moduleName ${q}my-library$q",
    s"//> using ${prefix}version ${q}0.1.1$q"
  )
  def keys = Seq(
    "organization",
    "name",
    "version",
    "computeVersion",
    "compute-version",
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
    "scala-platform-suffix",
    "repository",
    "gpgKey",
    "gpg-key",
    "gpgOption",
    "gpg-option",
    "gpgOptions",
    "gpg-options"
  ).map(prefix + _)

  override def getValueNumberBounds(key: String) = key match {
    case "gpgOptions" | "gpg-options" | "gpgOption" | "gpg-option" =>
      UsingDirectiveValueNumberBounds(1, Int.MaxValue)
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
      case "name" | "moduleName" =>
        PublishOptions(name = Some(singleValue))
      case "version" =>
        PublishOptions(version = Some(singleValue))
      case "computeVersion" | "compute-version" =>
        value {
          ComputeVersion.parse(singleValue).map {
            computeVersion =>
              PublishOptions(
                computeVersion = Some(
                  computeVersion
                )
              )
          }
        }
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
        value {
          Developer.parse(singleValue).map {
            developer => PublishOptions(developers = Seq(developer))
          }
        }
      case "scalaVersionSuffix" | "scala-version-suffix" =>
        PublishOptions(scalaVersionSuffix = Some(singleValue.value))
      case "scalaPlatformSuffix" | "scala-platform-suffix" =>
        PublishOptions(scalaPlatformSuffix = Some(singleValue.value))
      case "repository" =>
        PublishOptions(repository = Some(singleValue.value))
      case "gpgKey" | "gpg-key" =>
        PublishOptions(gpgSignatureId = Some(singleValue.value))
      case "gpgOptions" | "gpg-options" | "gpgOption" | "gpg-option" =>
        PublishOptions(gpgOptions = severalValues.map(_.value).toList)
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
