package scala.cli.commands.publish

import coursier.cache.ArchiveCache

import java.net.URI
import java.time.Instant
import java.util.function.Supplier

import scala.build.Ops.*
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.internal.CoursierUserAgent
import scala.build.options.{BuildOptions, ComputeVersion, PublishContextualOptions, PublishOptions}
import scala.build.{Logger, ScalaArtifacts}
import scala.cli.commands.pgp.PgpExternalCommand
import scala.cli.commands.publish.ConfigUtil.*
import scala.cli.config.{ConfigDb, Keys, PasswordOption, PublishCredentials}
import scala.cli.errors.MissingPublishOptionError
import scala.cli.publish.BouncycastleSignerMaker
import scala.cli.util.ConfigPasswordOptionHelpers.*
import scala.concurrent.ExecutionContextExecutorService
import scala.concurrent.duration.DurationInt
import scala.publish.Pom
import scala.publish.checksum.logger.ChecksumLogger
import scala.publish.checksum.{ChecksumType, Checksums}
import scala.publish.fileset.FileSet
import scala.publish.signing.{GpgSigner, Signer}
import scala.publish.upload.{HttpURLConnectionUpload, RetryingUpload, Upload}
import scala.publish.util.EmaRetryParams

object PublishUtils {

  /** The [[Upload]] used for HTTP(S) repositories.
    *
    * @param connectionTimeoutSeconds
    *   timeout for establishing the connection (`--connection-timeout-seconds`)
    * @param responseTimeoutSeconds
    *   timeout for reading the response once connected (`--response-timeout-seconds`)
    * @param retries
    *   how many times to attempt uploads failing with transient errors (`--upload-retries`)
    * @param retryWaitTimeMillis
    *   time to wait before the first retry (`--upload-retry-wait-time-millis`)
    */
  def httpUpload(
    connectionTimeoutSeconds: Option[Int],
    responseTimeoutSeconds: Option[Int],
    retries: Option[Int] = None,
    retryWaitTimeMillis: Option[Int] = None
  ): Upload =
    RetryingUpload(
      underlying = HttpURLConnectionUpload.create(
        HttpURLConnectionUpload.Timeouts(
          connect = connectionTimeoutSeconds.map(_.seconds),
          read = responseTimeoutSeconds.map(_.seconds)
        )
      ),
      retryParams = retryParams(
        defaults = RetryingUpload.defaultRetryParams,
        attempts = retries,
        initialWaitDurationMs = retryWaitTimeMillis
      )
    )

  /** `defaults`, with the passed values overridden */
  def retryParams(
    defaults: EmaRetryParams,
    attempts: Option[Int],
    initialWaitDurationMs: Option[Int]
  ): EmaRetryParams =
    defaults.copy(
      attempts = attempts.getOrElse(defaults.attempts),
      initialWaitDurationMs = initialWaitDurationMs.fold(defaults.initialWaitDurationMs)(_.toLong)
    )

  /** Using directives setting the POM fields reported by [[Pom.centralRequirementErrors]] */
  private val centralRequirementDirectives = Map(
    "Missing project name"        -> "publish.name",
    "Missing project description" -> "publish.description",
    "Missing project URL"         -> "publish.url",
    "Missing license"             -> "publish.license",
    "Missing developer"           -> "publish.developer",
    "Missing SCM information"     -> "publish.scm"
  )

  /** Warnings about the fields of `project` that Maven Central requires, but which are missing */
  def centralRequirementDiagnostics(project: Pom.Project): Seq[String] =
    Pom.centralRequirementErrors(project).map { error =>
      val hint = centralRequirementDirectives
        .get(error)
        .fold("")(directive => s" (set it with the '//> using $directive' directive)")
      s"Publishing to Sonatype, but: $error$hint."
    }

  /** Compute checksum files for the given [[FileSet]].
    *
    * Signature files (`.asc`) are excluded — only raw artifacts should carry checksums.
    */
  def computeChecksums(
    fileSet: FileSet,
    types: Seq[ChecksumType],
    now: Instant,
    pool: ExecutionContextExecutorService,
    logger: => ChecksumLogger
  ): FileSet = {
    val withoutSignatures = FileSet(
      fileSet.elements.filterNot((path, _) =>
        path.elements.lastOption.exists(_.endsWith(".asc"))
      )
    )
    Checksums(
      types = types,
      fileSet = withoutSignatures,
      now = now,
      pool = pool,
      logger = logger
    ).unsafeRun()(using pool)
  }

  def getBouncyCastleSigner(
    secretKey: PasswordOption,
    secretKeyPasswordOpt: Option[PasswordOption],
    buildOptions: Option[BuildOptions],
    forceSigningExternally: Boolean,
    logger: Logger
  ): Signer = {
    val getLauncher: Supplier[Array[String]] = { () =>
      val fileCache    = buildOptions.map(_.finalCache).getOrElse(CoursierUserAgent.defaultCache())
      val archiveCache = buildOptions.map(_.archiveCache)
        .getOrElse(ArchiveCache().copy(cache = fileCache))
      PgpExternalCommand.launcher(
        fileCache,
        archiveCache,
        logger,
        buildOptions.getOrElse(BuildOptions())
      ) match {
        case Left(e)              => throw new Exception(e)
        case Right(binaryCommand) => binaryCommand.toArray
      }
    }

    (new BouncycastleSignerMaker).get(
      forceSigningExternally,
      secretKeyPasswordOpt.fold(null)(_.toCliSigning),
      secretKey.toCliSigning,
      getLauncher,
      logger
    )
  }

  def getPublishCredentials(
    repo: String,
    configDb: () => ConfigDb
  ): Either[BuildException, Option[PublishCredentials]] = {
    val uri     = new URI(repo)
    val isHttps = uri.getScheme == "https"
    val hostOpt = Option.when(isHttps)(uri.getHost)
    hostOpt match {
      case None       => Right(None)
      case Some(host) =>
        configDb().get(Keys.publishCredentials).wrapConfigException.map { credListOpt =>
          credListOpt.flatMap { credList =>
            credList.find { cred =>
              cred.host == host &&
              (isHttps || cred.httpsOnly.contains(false))
            }
          }
        }
    }
  }

  extension (publishContextualOptions: PublishContextualOptions) {
    def getSecretKeyPasswordOpt(configDb: () => ConfigDb): Option[PasswordOption] =
      if publishContextualOptions.secretKeyPassword.isDefined then
        for {
          secretKeyPassConfigOpt <- publishContextualOptions.secretKeyPassword
          secretKeyPass          <- secretKeyPassConfigOpt.get(configDb()).toOption
        } yield secretKeyPass
      else
        for {
          secretKeyPassOpt <- configDb().get(Keys.pgpSecretKeyPassword).toOption
          secretKeyPass    <- secretKeyPassOpt
        } yield secretKeyPass

    def getGpgSigner: Either[MissingPublishOptionError, GpgSigner] =
      publishContextualOptions.gpgSignatureId.map { gpgSignatureId =>
        GpgSigner(
          key = GpgSigner.Key.Id(gpgSignatureId),
          extraOptions = publishContextualOptions.gpgOptions
        )
      }.toRight {
        new MissingPublishOptionError(
          name = "ID of the GPG key",
          optionName = "--gpgKey",
          directiveName = ""
        )
      }
  }
  case class ArtifactData(org: String, name: String, version: String)
  extension (publishOptions: PublishOptions) {

    /** Maven POM `name` / ivy `m:name`: `publish.name` when set, otherwise the published artifact
      * name.
      */
    def pomProjectNameForMaven(fallbackModuleName: String): String =
      publishOptions.name
        .map(_.value)
        .map(_.trim)
        .filter(_.nonEmpty)
        .getOrElse(fallbackModuleName)

    def artifactData(
      workspace: os.Path,
      logger: Logger,
      scalaArtifactsOpt: Option[ScalaArtifacts],
      isCi: Boolean
    ): Either[BuildException, ArtifactData] = {
      lazy val orgNameOpt = GitRepo.maybeGhRepoOrgName(workspace, logger)

      val maybeOrg = publishOptions.organization match {
        case Some(org0) => Right(org0.value)
        case None       => orgNameOpt.map(_._1) match {
            case Some(org) =>
              val mavenOrg = s"io.github.$org"
              logger.message(
                s"Using directive publish.organization not set, computed $mavenOrg from GitHub organization $org as default organization"
              )
              Right(mavenOrg)
            case None =>
              Left(new MissingPublishOptionError(
                "organization",
                "--organization",
                "publish.organization"
              ))
          }
      }

      val moduleName = publishOptions.moduleName match {
        case Some(name0) => name0.value
        case None        =>
          val name = publishOptions.name match {
            case Some(name0) => name0.value
            case None        =>
              val name = workspace.last
              logger.message(
                s"Using directive publish.name not specified, using workspace directory name $name as default name"
              )
              name
          }
          scalaArtifactsOpt.map(_.params) match {
            case Some(scalaParams) =>
              val pf = publishOptions.scalaPlatformSuffix.getOrElse {
                scalaParams.platform.fold("")("_" + _)
              }
              val sv = publishOptions.scalaVersionSuffix.getOrElse {
                // FIXME Allow full cross version too
                "_" + scalaParams.scalaBinaryVersion
              }
              name + pf + sv
            case None =>
              name
          }
      }

      val maybeVer = publishOptions.version match {
        case Some(ver0) => Right(ver0.value)
        case None       =>
          val computeVer = publishOptions.contextual(isCi).computeVersion.orElse {
            def isGitRepo = GitRepo.gitRepoOpt(workspace).isDefined

            val default = ComputeVersion.defaultComputeVersion(!isCi && isGitRepo)
            if default.isDefined then
              logger.message(
                s"Using directive ${MissingPublishOptionError.versionError.directiveName} not set, assuming git:tag as publish.computeVersion"
              )
            default
          }
          computeVer match {
            case Some(cv) => cv.get(workspace)
            case None     => Left(MissingPublishOptionError.versionError)
          }
      }

      (maybeOrg, maybeVer)
        .traverseN
        .left.map(CompositeBuildException(_))
        .map {
          case (org, ver) =>
            ArtifactData(org, moduleName, ver)
        }
    }
  }

}
