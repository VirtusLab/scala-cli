package scala.build.preprocessing.directives

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
  ): Either[BuildException, ProcessedUsingDirective] =
    checkIfValuesAreExpected(scopedDirective).flatMap { groupedScopedValuesContainer =>
      val severalValues = groupedScopedValuesContainer.scopedStringValues.map(_.positioned)
      val singleValue   = severalValues.head

      if (!scopedDirective.directive.key.startsWith(prefix))
        Left(new UnexpectedDirectiveError(scopedDirective.directive.key))
      else scopedDirective.directive.key.stripPrefix(prefix) match {
        case "organization" =>
          Right(PublishOptions(organization = Some(singleValue)))
        case "name" =>
          Right(PublishOptions(name = Some(singleValue)))
        case "version" =>
          Right(PublishOptions(version = Some(singleValue)))
        case "computeVersion" | "compute-version" =>
          ComputeVersion.parse(singleValue).map {
            computeVersion =>
              PublishOptions(
                computeVersion = Some(
                  computeVersion
                )
              )
          }

        case "url" =>
          Right(PublishOptions(url = Some(singleValue)))
        case "license" =>
          License.parse(singleValue).map { license =>
            PublishOptions(license = Some(license))
          }
        case "versionControl" | "version-control" | "scm" =>
          Vcs.parse(singleValue).map { versionControl =>
            PublishOptions(versionControl = Some(versionControl))
          }
        case "description" =>
          Right(PublishOptions(description = Some(singleValue.value)))
        case "developer" =>
          Developer.parse(singleValue).map {
            developer => PublishOptions(developers = Seq(developer))

          }
        case "scalaVersionSuffix" | "scala-version-suffix" =>
          Right(PublishOptions(scalaVersionSuffix = Some(singleValue.value)))
        case "scalaPlatformSuffix" | "scala-platform-suffix" =>
          Right(PublishOptions(scalaPlatformSuffix = Some(singleValue.value)))
        case "repository" =>
          Right(PublishOptions(repository = Some(singleValue.value)))
        case "gpgKey" | "gpg-key" =>
          Right(PublishOptions(gpgSignatureId = Some(singleValue.value)))
        case "gpgOptions" | "gpg-options" | "gpgOption" | "gpg-option" =>
          Right(PublishOptions(gpgOptions = severalValues.map(_.value).toList))
        case _ =>
          Left(new UnexpectedDirectiveError(scopedDirective.directive.key))
      }
    }.map { publishOptions =>
      val options = BuildOptions(
        notForBloopOptions = PostBuildOptions(
          publishOptions = publishOptions
        )
      )
      ProcessedDirective(Some(options), Seq.empty)
    }
}
