package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.{BuildException, MalformedInputError, UnexpectedDirectiveError}
import scala.build.options.publish.{ComputeVersion, ConfigPasswordOption}
import scala.build.options.{
  BuildOptions,
  PostBuildOptions,
  PublishContextualOptions,
  PublishOptions
}
import scala.cli.signing.shared.PasswordOption

case object UsingPublishContextualDirectiveHandler extends UsingDirectiveHandler {

  private def prefix   = "publish."
  private def ciPrefix = "ci."

  def name        = "Publish (contextual)"
  def description = "Set contextual parameters for publishing"
  def usage       = s"//> using $prefix[$ciPrefix](computeVersion|repository|secretKey|…) [value]"
  override def isRestricted = true

  override def usageMd =
    s"""`//> using ${prefix}computeVersion `"value"
       |`//> using $prefix${ciPrefix}repository `"value"
       |`//> using ${prefix}secretKey `"value"
       |""".stripMargin

  private def q = "\""
  override def examples = Seq(
    s"//> using ${prefix}computeVersion ${q}git:tag$q",
    s"//> using $prefix${ciPrefix}repository ${q}central-s01$q",
    s"//> using ${prefix}secretKey ${q}env:PUBLISH_SECRET_KEY$q"
  )
  def keys = {
    val names = Seq(
      "computeVersion",
      "compute-version",
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
    )
    names.map(prefix + _) ++
      names.map(prefix + ciPrefix + _)
  }

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

    val (strippedKey, isCi) =
      if (scopedDirective.directive.key.startsWith(prefix)) {
        val key = scopedDirective.directive.key.stripPrefix(prefix)
        if (key.startsWith(ciPrefix)) (key.stripPrefix(ciPrefix), true)
        else (key, false)
      }
      else
        value(Left(new UnexpectedDirectiveError(scopedDirective.directive.key)))

    val publishContextualOptions = strippedKey match {
      case "computeVersion" | "compute-version" =>
        value {
          ComputeVersion.parse(singleValue).map {
            computeVersion =>
              PublishContextualOptions(
                computeVersion = Some(
                  computeVersion
                )
              )
          }
        }
      case "repository" =>
        PublishContextualOptions(repository = Some(singleValue.value))
      case "gpgKey" | "gpg-key" =>
        PublishContextualOptions(gpgSignatureId = Some(singleValue.value))
      case "gpgOptions" | "gpg-options" | "gpgOption" | "gpg-option" =>
        PublishContextualOptions(gpgOptions = severalValues.map(_.value).toList)
      case "secretKey" =>
        PublishContextualOptions(secretKey =
          Some(
            ConfigPasswordOption.ActualOption(value(parsePasswordOption(singleValue.value)))
          )
        )
      case "secretKeyPassword" =>
        PublishContextualOptions(
          secretKeyPassword = Some(
            ConfigPasswordOption.ActualOption(value(parsePasswordOption(singleValue.value)))
          )
        )
      case "user" =>
        PublishContextualOptions(repoUser = Some(value(parsePasswordOption(singleValue.value))))
      case "password" =>
        PublishContextualOptions(repoPassword = Some(value(parsePasswordOption(singleValue.value))))
      case "realm" =>
        PublishContextualOptions(repoRealm = Some(singleValue.value))
      case _ =>
        value(Left(new UnexpectedDirectiveError(scopedDirective.directive.key)))
    }

    val publishOptions =
      if (isCi) PublishOptions(ci = publishContextualOptions)
      else PublishOptions(local = publishContextualOptions)

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
