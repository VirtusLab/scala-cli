package scala.cli.commands.publish

import coursier.core.Authentication
import coursier.maven.MavenRepository
import coursier.publish.sonatype.SonatypeApi
import coursier.publish.util.EmaRetryParams
import coursier.publish.{Hooks, PublishRepository}

import java.net.URI
import java.util.concurrent.ScheduledExecutorService

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internals.ConsoleUtils.ScalaCliConsole.warnPrefix
import scala.build.{Logger, SonatypeUtils}
import scala.cli.commands.util.ScalaCliSttpBackend

final case class RepoParams(
  repo: PublishRepository,
  targetRepoOpt: Option[String],
  hooks: Hooks,
  isIvy2LocalLike: Boolean,
  defaultParallelUpload: Boolean,
  supportsSig: Boolean,
  acceptsChecksums: Boolean,
  shouldSign: Boolean,
  shouldAuthenticate: Boolean
) {
  import RepoParams.*

  def withAuth(auth: Authentication): RepoParams =
    copy(
      repo = repo.withAuthentication(auth),
      hooks = hooks match {
        case s: Hooks.Sonatype =>
          s.copy(
            repo = s.repo.withAuthentication(auth),
            api = s.api.copy(
              authentication = Some(auth)
            )
          )
        case other => other
      }
    )
  def withAuth(authOpt: Option[Authentication]): RepoParams = authOpt.fold(this)(withAuth)

  lazy val isSonatype: Boolean =
    Option(new URI(repo.snapshotRepo.root))
      .filter(_.getScheme == "https")
      .map(_.getHost)
      .exists(sonatypeHosts.contains)
}

object RepoParams {
  private val sonatypeOssrhStagingApiBase = "https://ossrh-staging-api.central.sonatype.com"
  private val sonatypeSnapshotsBase       = s"${SonatypeUtils.snapshotsRepositoryUrl}/"
  private val sonatypeLegacyBase          = "https://oss.sonatype.org"
  private val sonatypeS01LegacyBase       = "https://s01.oss.sonatype.org"
  private def sonatypeHosts: Seq[String]  =
    Seq(
      sonatypeLegacyBase,
      sonatypeSnapshotsBase,
      sonatypeS01LegacyBase,
      sonatypeOssrhStagingApiBase
    ).map(new URI(_).getHost)

  def apply(
    repo: String,
    vcsUrlOpt: Option[String],
    workspace: os.Path,
    ivy2HomeOpt: Option[os.Path],
    isIvy2LocalLike: Boolean,
    es: ScheduledExecutorService,
    logger: Logger,
    connectionTimeoutRetries: Option[Int] = None,
    connectionTimeoutSeconds: Option[Int] = None,
    stagingRepoRetries: Option[Int] = None,
    stagingRepoWaitTimeMilis: Option[Int] = None
  ): Either[BuildException, RepoParams] = either {
    repo match {
      case "ivy2-local" =>
        RepoParams.ivy2Local(ivy2HomeOpt)
      case "sonatype" | "central" | "maven-central" | "mvn-central" =>
        logger.message(s"Using Portal OSSRH Staging API: $sonatypeOssrhStagingApiBase")
        RepoParams.centralRepo(
          base = sonatypeOssrhStagingApiBase,
          useLegacySnapshots = false,
          connectionTimeoutRetries = connectionTimeoutRetries,
          connectionTimeoutSeconds = connectionTimeoutSeconds,
          stagingRepoRetries = stagingRepoRetries,
          stagingRepoWaitTimeMilis = stagingRepoWaitTimeMilis,
          es = es,
          logger = logger
        )
      case "sonatype-legacy" | "central-legacy" | "maven-central-legacy" | "mvn-central-legacy" =>
        logger.message(s"$warnPrefix $sonatypeLegacyBase is EOL since 2025-06-30.")
        logger.message(s"$warnPrefix $sonatypeLegacyBase publishing is expected to fail.")
        RepoParams.centralRepo(
          base = sonatypeLegacyBase,
          useLegacySnapshots = true,
          connectionTimeoutRetries = connectionTimeoutRetries,
          connectionTimeoutSeconds = connectionTimeoutSeconds,
          stagingRepoRetries = stagingRepoRetries,
          stagingRepoWaitTimeMilis = stagingRepoWaitTimeMilis,
          es = es,
          logger = logger
        )
      case "sonatype-s01" | "central-s01" | "maven-central-s01" | "mvn-central-s01" =>
        logger.message(s"$warnPrefix $sonatypeS01LegacyBase is EOL since 2025-06-30.")
        logger.message(s"$warnPrefix it's expected publishing will fail.")
        RepoParams.centralRepo(
          base = sonatypeS01LegacyBase,
          useLegacySnapshots = true,
          connectionTimeoutRetries = connectionTimeoutRetries,
          connectionTimeoutSeconds = connectionTimeoutSeconds,
          stagingRepoRetries = stagingRepoRetries,
          stagingRepoWaitTimeMilis = stagingRepoWaitTimeMilis,
          es = es,
          logger = logger
        )
      case "github" =>
        value(RepoParams.gitHubRepo(vcsUrlOpt, workspace, logger))
      case repoStr if repoStr.startsWith("github:") && repoStr.count(_ == '/') == 1 =>
        val (org, name) = repoStr.stripPrefix("github:").split('/') match {
          case Array(org0, name0) => (org0, name0)
          case other              => sys.error(s"Cannot happen ('$repoStr' -> ${other.toSeq})")
        }
        RepoParams.gitHubRepoFor(org, name)
      case repoStr =>
        val repo0 = RepositoryParser.repositoryOpt(repoStr).getOrElse {
          val url =
            if (repoStr.contains("://")) repoStr
            else os.Path(repoStr, os.pwd).toNIO.toUri.toASCIIString
          MavenRepository(url)
        }

        RepoParams(
          repo = PublishRepository.Simple(repo0),
          targetRepoOpt = None,
          hooks = Hooks.dummy,
          isIvy2LocalLike = isIvy2LocalLike,
          defaultParallelUpload = true,
          supportsSig = true,
          acceptsChecksums = true,
          shouldSign = false,
          shouldAuthenticate = false
        )
    }
  }

  def centralRepo(
    base: String,
    useLegacySnapshots: Boolean,
    connectionTimeoutRetries: Option[Int],
    connectionTimeoutSeconds: Option[Int],
    stagingRepoRetries: Option[Int],
    stagingRepoWaitTimeMilis: Option[Int],
    es: ScheduledExecutorService,
    logger: Logger
  ): RepoParams = {
    val repo0 = PublishRepository.Sonatype(
      base = MavenRepository(base),
      useLegacySnapshots = useLegacySnapshots
    )
    val backend = ScalaCliSttpBackend.httpURLConnection(logger, connectionTimeoutSeconds)
    val api     = SonatypeApi(
      backend = backend,
      base = base + "/service/local",
      authentication = None,
      verbosity = logger.verbosity,
      retryOnTimeout = connectionTimeoutRetries.getOrElse(3),
      stagingRepoRetryParams =
        EmaRetryParams(
          attempts = stagingRepoRetries.getOrElse(3),
          initialWaitDurationMs = stagingRepoWaitTimeMilis.getOrElse(10 * 1000),
          factor = 2.0f
        )
    )
    val hooks0 = Hooks.sonatype(
      repo = repo0,
      api = api,
      out = logger.compilerOutputStream, // meh
      verbosity = logger.verbosity,
      batch = coursier.paths.Util.useAnsiOutput(), // FIXME Get via logger
      es = es
    )
    RepoParams(
      repo = repo0,
      targetRepoOpt = Some("https://repo1.maven.org/maven2"),
      hooks = hooks0,
      isIvy2LocalLike = false,
      defaultParallelUpload = true,
      supportsSig = true,
      acceptsChecksums = true,
      shouldSign = true,
      shouldAuthenticate = true
    )
  }

  def gitHubRepoFor(org: String, name: String): RepoParams =
    RepoParams(
      repo = PublishRepository.Simple(MavenRepository(s"https://maven.pkg.github.com/$org/$name")),
      targetRepoOpt = None,
      hooks = Hooks.dummy,
      isIvy2LocalLike = false,
      defaultParallelUpload = false,
      supportsSig = false,
      acceptsChecksums = false,
      shouldSign = false,
      shouldAuthenticate = true
    )

  def gitHubRepo(
    vcsUrlOpt: Option[String],
    workspace: os.Path,
    logger: Logger
  ): Either[BuildException, RepoParams] = either {
    val orgNameFromVcsOpt = vcsUrlOpt.flatMap(GitRepo.maybeGhOrgName)

    val (org, name) = orgNameFromVcsOpt match {
      case Some(orgName) => orgName
      case None          => value(GitRepo.ghRepoOrgName(workspace, logger))
    }

    gitHubRepoFor(org, name)
  }

  def ivy2Local(ivy2HomeOpt: Option[os.Path]): RepoParams = {
    val home = ivy2HomeOpt
      .orElse(sys.props.get("ivy.home").map(prop => os.Path(prop)))
      .orElse(sys.props.get("user.home").map(prop => os.Path(prop) / ".ivy2"))
      .getOrElse(os.home / ".ivy2")
    val base = home / "local"
    // not really a Maven repo…
    RepoParams(
      repo = PublishRepository.Simple(MavenRepository(base.toNIO.toUri.toASCIIString)),
      targetRepoOpt = None,
      hooks = Hooks.dummy,
      isIvy2LocalLike = true,
      defaultParallelUpload = true,
      supportsSig = true,
      acceptsChecksums = true,
      shouldSign = false,
      shouldAuthenticate = false
    )
  }

}
