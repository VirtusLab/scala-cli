package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.directives.*
import scala.build.errors.{BuildException, CompositeBuildException, MalformedInputError}
import scala.build.options.publish.{ComputeVersion, ConfigPasswordOption}
import scala.build.options.{
  BuildOptions,
  JavaOpt,
  PostBuildOptions,
  PublishContextualOptions,
  PublishOptions,
  ShadowingSeq
}
import scala.build.{Logger, Positioned, options}
import scala.cli.commands.SpecificationLevel
import scala.cli.signing.shared.PasswordOption

trait PublishContextual {
  def computeVersion: Option[Positioned[String]]
  def repository: Option[String]
  def gpgKey: Option[String]
  def gpgOptions: List[String]
  def secretKey: Option[Positioned[String]]
  def secretKeyPassword: Option[Positioned[String]]
  def user: Option[Positioned[String]]
  def password: Option[Positioned[String]]
  def realm: Option[String]

  def buildOptions(isCi: Boolean): Either[BuildException, BuildOptions] = either {

    val maybeComputeVersion = computeVersion
      .map(ComputeVersion.parse)
      .sequence

    val maybeSecretKey = secretKey
      .map { input =>
        PublishContextual.parsePasswordOption(input)
          .map(ConfigPasswordOption.ActualOption(_))
      }
      .sequence
    val maybeSecretKeyPassword = secretKeyPassword
      .map { input =>
        PublishContextual.parsePasswordOption(input)
          .map(ConfigPasswordOption.ActualOption(_))
      }
      .sequence

    val maybeUser = user
      .map(PublishContextual.parsePasswordOption)
      .sequence
    val maybePassword = password
      .map(PublishContextual.parsePasswordOption)
      .sequence

    val (computeVersionOpt, secretKeyOpt, secretKeyPasswordOpt, userOpt, passwordOpt) = value {
      (maybeComputeVersion, maybeSecretKey, maybeSecretKeyPassword, maybeUser, maybePassword)
        .traverseN
        .left.map(CompositeBuildException(_))
    }

    val publishContextualOptions = PublishContextualOptions(
      computeVersion = computeVersionOpt,
      repository = repository,
      gpgSignatureId = gpgKey,
      gpgOptions = gpgOptions,
      secretKey = secretKeyOpt,
      secretKeyPassword = secretKeyPasswordOpt,
      repoUser = userOpt,
      repoPassword = passwordOpt,
      repoRealm = realm
    )

    val publishOptions =
      if (isCi) PublishOptions(ci = publishContextualOptions)
      else PublishOptions(local = publishContextualOptions)

    BuildOptions(
      notForBloopOptions = PostBuildOptions(
        publishOptions = publishOptions
      )
    )
  }
}

object PublishContextual {

  @DirectiveGroupName("Publish (contextual)")
  @DirectivePrefix("publish.")
  @DirectiveExamples("//> using publish.computeVersion \"git:tag\"")
  @DirectiveExamples("//> using publish.repository \"central-s01\"")
  @DirectiveExamples("//> using publish.secretKey \"env:PUBLISH_SECRET_KEY\"")
  @DirectiveUsage(
    "//> using publish.(computeVersion|repository|secretKey|…) [value]",
    """`//> using publish.computeVersion `"value"
      |`//> using publish.repository `"value"
      |`//> using publish.secretKey `"value"
      |""".stripMargin
  )
  @DirectiveDescription("Set contextual parameters for publishing")
  @DirectiveLevel(SpecificationLevel.EXPERIMENTAL)
  // format: off
  final case class Local(
    computeVersion: Option[Positioned[String]] = None,
    repository: Option[String] = None,
    gpgKey: Option[String] = None,
    gpgOptions: List[String] = Nil,
    secretKey: Option[Positioned[String]] = None,
    secretKeyPassword: Option[Positioned[String]] = None,
    user: Option[Positioned[String]] = None,
    password: Option[Positioned[String]] = None,
    realm: Option[String] = None
  ) extends HasBuildOptions with PublishContextual {
    // format: on
    def buildOptions: Either[BuildException, BuildOptions] =
      buildOptions(isCi = false)
  }

  object Local {
    val handler: DirectiveHandler[Local] = DirectiveHandler.derive
  }

  @DirectiveGroupName("Publish (CI)")
  @DirectivePrefix("publish.ci.")
  @DirectiveExamples("//> using publish.ci.computeVersion \"git:tag\"")
  @DirectiveExamples("//> using publish.ci.repository \"central-s01\"")
  @DirectiveExamples("//> using publish.ci.secretKey \"env:PUBLISH_SECRET_KEY\"")
  @DirectiveUsage(
    "//> using publish.[.ci](computeVersion|repository|secretKey|…) [value]",
    """`//> using publish.ci.computeVersion `"value"
      |`//> using publish.ci.repository `"value"
      |`//> using publish.ci.secretKey `"value"
      |""".stripMargin
  )
  @DirectiveDescription("Set CI parameters for publishing")
  @DirectiveLevel(SpecificationLevel.RESTRICTED)
  // format: off
  final case class CI(
    computeVersion: Option[Positioned[String]] = None,
    repository: Option[String] = None,
    gpgKey: Option[String] = None,
    gpgOptions: List[String] = Nil,
    secretKey: Option[Positioned[String]] = None,
    secretKeyPassword: Option[Positioned[String]] = None,
    user: Option[Positioned[String]] = None,
    password: Option[Positioned[String]] = None,
    realm: Option[String] = None
  ) extends HasBuildOptions with PublishContextual {
    // format: on
    def buildOptions: Either[BuildException, BuildOptions] =
      buildOptions(isCi = true)
  }

  object CI {
    val handler: DirectiveHandler[CI] = DirectiveHandler.derive
  }

  private def parsePasswordOption(input: Positioned[String])
    : Either[BuildException, PasswordOption] =
    PasswordOption.parse(input.value).left.map { _ =>
      new MalformedInputError(
        "secret",
        input.value,
        "file:_path_|value:_value_|env:_env_var_name_",
        positions = input.positions
      )
    }
}
