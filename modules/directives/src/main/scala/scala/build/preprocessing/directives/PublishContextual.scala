package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.Positioned
import scala.build.directives.*
import scala.build.errors.{BuildException, CompositeBuildException, MalformedInputError}
import scala.build.options.*
import scala.build.options.publish.ConfigPasswordOption
import scala.cli.commands.SpecificationLevel
import scala.cli.signing.shared.PasswordOption

trait PublishContextual {
  def computeVersion: Option[Positioned[String]]
  def repository: Option[String]
  def gpgKey: Option[String]
  def gpgOptions: List[String]
  def secretKey: Option[Positioned[String]]
  def secretKeyPassword: Option[Positioned[String]]
  def publicKey: Option[Positioned[String]]
  def user: Option[Positioned[String]]
  def password: Option[Positioned[String]]
  def realm: Option[String]
  def doc: Option[Boolean]
  def directivePrefix: String
  def connectionTimeoutSeconds: Option[Positioned[String]]
  def connectionTimeoutRetries: Option[Positioned[String]]
  def connectionRetryWaitTimeMillis: Option[Positioned[String]]
  def responseTimeoutSeconds: Option[Positioned[String]]
  def stagingRepoRetries: Option[Positioned[String]]
  def stagingRepoWaitTimeMillis: Option[Positioned[String]]
  def stagingRepoStatusPollAttempts: Option[Positioned[String]]
  def stagingRepoStatusPollWaitTimeMillis: Option[Positioned[String]]
  def uploadRetries: Option[Positioned[String]]
  def uploadRetryWaitTimeMillis: Option[Positioned[String]]

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
    val maybePublicKey = publicKey
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

    val (
      computeVersionOpt,
      secretKeyOpt,
      secretKeyPasswordOpt,
      publicKeyOpt,
      userOpt,
      passwordOpt
    ) = value {
      (
        maybeComputeVersion,
        maybeSecretKey,
        maybeSecretKeyPassword,
        maybePublicKey,
        maybeUser,
        maybePassword
      )
        .traverseN
        .left.map(CompositeBuildException(_))
    }

    val integers: Map[String, Int] = value {
      import PublishContextual.IntKind.*
      Seq(
        ("connectionTimeoutSeconds", connectionTimeoutSeconds, NonNegative),
        ("connectionTimeoutRetries", connectionTimeoutRetries, Positive),
        ("connectionRetryWaitTimeMillis", connectionRetryWaitTimeMillis, NonNegative),
        ("responseTimeoutSeconds", responseTimeoutSeconds, NonNegative),
        ("stagingRepoRetries", stagingRepoRetries, Positive),
        ("stagingRepoWaitTimeMillis", stagingRepoWaitTimeMillis, NonNegative),
        ("stagingRepoStatusPollAttempts", stagingRepoStatusPollAttempts, Positive),
        ("stagingRepoStatusPollWaitTimeMillis", stagingRepoStatusPollWaitTimeMillis, NonNegative),
        ("uploadRetries", uploadRetries, Positive),
        ("uploadRetryWaitTimeMillis", uploadRetryWaitTimeMillis, NonNegative)
      )
        .flatMap { (name, input, kind) =>
          input.map(PublishContextual.parseInt(directivePrefix + name, _, kind).map(name -> _))
        }
        .sequence
        .left.map(CompositeBuildException(_))
        .map(_.toMap)
    }

    val publishContextualOptions = PublishContextualOptions(
      computeVersion = computeVersionOpt,
      repository = repository,
      docJar = doc,
      gpgSignatureId = gpgKey,
      gpgOptions = gpgOptions,
      secretKey = secretKeyOpt,
      secretKeyPassword = secretKeyPasswordOpt,
      publicKey = publicKeyOpt,
      repoUser = userOpt,
      repoPassword = passwordOpt,
      repoRealm = realm,
      connectionTimeoutSeconds = integers.get("connectionTimeoutSeconds"),
      connectionTimeoutRetries = integers.get("connectionTimeoutRetries"),
      connectionRetryWaitTimeMillis = integers.get("connectionRetryWaitTimeMillis"),
      responseTimeoutSeconds = integers.get("responseTimeoutSeconds"),
      stagingRepoRetries = integers.get("stagingRepoRetries"),
      stagingRepoWaitTimeMilis = integers.get("stagingRepoWaitTimeMillis"),
      stagingRepoStatusPollAttempts = integers.get("stagingRepoStatusPollAttempts"),
      stagingRepoStatusPollWaitTimeMillis = integers.get("stagingRepoStatusPollWaitTimeMillis"),
      uploadRetries = integers.get("uploadRetries"),
      uploadRetryWaitTimeMillis = integers.get("uploadRetryWaitTimeMillis")
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
  @DirectiveExamples("//> using publish.computeVersion git:tag")
  @DirectiveExamples("//> using publish.repository central-s01")
  @DirectiveExamples("//> using publish.secretKey env:PUBLISH_SECRET_KEY")
  @DirectiveExamples("//> using publish.doc false")
  @DirectiveExamples("//> using publish.uploadRetries 5")
  @DirectiveUsage(
    "//> using publish.(computeVersion|repository|secretKey|…) [value]",
    """`//> using publish.computeVersion` value
      |
      |`//> using publish.repository` value
      |
      |`//> using publish.secretKey` value
      |
      |`//> using publish.doc` boolean
      |
      |`//> using publish.uploadRetries` value
      |
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
    publicKey: Option[Positioned[String]] = None,
    user: Option[Positioned[String]] = None,
    password: Option[Positioned[String]] = None,
    realm: Option[String] = None,
    doc: Option[Boolean] = None,
    connectionTimeoutSeconds: Option[Positioned[String]] = None,
    connectionTimeoutRetries: Option[Positioned[String]] = None,
    connectionRetryWaitTimeMillis: Option[Positioned[String]] = None,
    responseTimeoutSeconds: Option[Positioned[String]] = None,
    stagingRepoRetries: Option[Positioned[String]] = None,
    stagingRepoWaitTimeMillis: Option[Positioned[String]] = None,
    stagingRepoStatusPollAttempts: Option[Positioned[String]] = None,
    stagingRepoStatusPollWaitTimeMillis: Option[Positioned[String]] = None,
    uploadRetries: Option[Positioned[String]] = None,
    uploadRetryWaitTimeMillis: Option[Positioned[String]] = None
  ) extends HasBuildOptions with PublishContextual {
    // format: on
    def directivePrefix: String                            = "publish."
    def buildOptions: Either[BuildException, BuildOptions] =
      buildOptions(isCi = false)
  }

  object Local {
    val handler: DirectiveHandler[Local] = DirectiveHandler.derive
  }

  @DirectiveGroupName("Publish (CI)")
  @DirectivePrefix("publish.ci.")
  @DirectiveExamples("//> using publish.ci.computeVersion git:tag")
  @DirectiveExamples("//> using publish.ci.repository central-s01")
  @DirectiveExamples("//> using publish.ci.secretKey env:PUBLISH_SECRET_KEY")
  @DirectiveExamples("//> using publish.ci.stagingRepoStatusPollAttempts 40")
  @DirectiveUsage(
    "//> using publish.[.ci](computeVersion|repository|secretKey|…) [value]",
    """`//> using publish.ci.computeVersion` value
      |
      |`//> using publish.ci.repository` value
      |
      |`//> using publish.ci.secretKey` value
      |
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
    publicKey: Option[Positioned[String]] = None,
    user: Option[Positioned[String]] = None,
    password: Option[Positioned[String]] = None,
    realm: Option[String] = None,
    doc: Option[Boolean] = None,
    connectionTimeoutSeconds: Option[Positioned[String]] = None,
    connectionTimeoutRetries: Option[Positioned[String]] = None,
    connectionRetryWaitTimeMillis: Option[Positioned[String]] = None,
    responseTimeoutSeconds: Option[Positioned[String]] = None,
    stagingRepoRetries: Option[Positioned[String]] = None,
    stagingRepoWaitTimeMillis: Option[Positioned[String]] = None,
    stagingRepoStatusPollAttempts: Option[Positioned[String]] = None,
    stagingRepoStatusPollWaitTimeMillis: Option[Positioned[String]] = None,
    uploadRetries: Option[Positioned[String]] = None,
    uploadRetryWaitTimeMillis: Option[Positioned[String]] = None
  ) extends HasBuildOptions with PublishContextual {
    // format: on
    def directivePrefix: String                            = "publish.ci."
    def buildOptions: Either[BuildException, BuildOptions] =
      buildOptions(isCi = true)
  }

  object CI {
    val handler: DirectiveHandler[CI] = DirectiveHandler.derive
  }

  private enum IntKind(val min: Int, val description: String) {
    case NonNegative extends IntKind(0, "a non-negative integer")
    case Positive    extends IntKind(1, "a positive integer")
  }

  private def parseInt(
    name: String,
    input: Positioned[String],
    kind: IntKind
  ): Either[BuildException, Int] =
    input.value.trim.toIntOption
      .filter(_ >= kind.min)
      .toRight {
        new MalformedInputError(name, input.value, kind.description, positions = input.positions)
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
