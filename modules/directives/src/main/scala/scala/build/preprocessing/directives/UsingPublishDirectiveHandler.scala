package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.Ops._
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  MalformedInputError,
  UnexpectedDirectiveError
}
import scala.build.options.publish.{
  ComputeVersion,
  Developer,
  License,
  MaybeConfigPasswordOption,
  Vcs
}
import scala.build.options.{BuildOptions, PostBuildOptions, PublishOptions}
import scala.cli.signing.shared.PasswordOption

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
  def keys = Seq(
    "organization",
    "name",
    "moduleName",
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
    "gpg-options",
    "secretKey",
    "secretKeyPassword",
    "user",
    "password",
    "realm"
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
      case "name" =>
        PublishOptions(name = Some(singleValue))
      case "moduleName" | "module-name" =>
        PublishOptions(moduleName = Some(singleValue))
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
      case "repository" =>
        PublishOptions(repository = Some(singleValue.value))
      case "gpgKey" | "gpg-key" =>
        PublishOptions(gpgSignatureId = Some(singleValue.value))
      case "gpgOptions" | "gpg-options" | "gpgOption" | "gpg-option" =>
        PublishOptions(gpgOptions = severalValues.map(_.value).toList)
      case "secretKey" =>
        PublishOptions(secretKey =
          Some(
            MaybeConfigPasswordOption.ActualOption(value(parsePasswordOption(singleValue.value)))
          )
        )
      case "secretKeyPassword" =>
        PublishOptions(secretKeyPassword =
          Some(
            MaybeConfigPasswordOption.ActualOption(value(parsePasswordOption(singleValue.value)))
          )
        )
      case "user" =>
        PublishOptions(repoUser = Some(value(parsePasswordOption(singleValue.value))))
      case "password" =>
        PublishOptions(repoPassword = Some(value(parsePasswordOption(singleValue.value))))
      case "realm" =>
        PublishOptions(repoRealm = Some(singleValue.value))
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

  private def parsePasswordOption(input: String): Either[BuildException, PasswordOption] =
    PasswordOption.parse(input)
      .left.map(_ =>
        new MalformedInputError("secret", input, "file:_path_|value:_value_|env:_env_var_name_")
      )
}
