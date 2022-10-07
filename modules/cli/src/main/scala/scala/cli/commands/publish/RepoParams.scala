package scala.cli.commands.publish

import coursier.core.Authentication
import coursier.maven.MavenRepository
import coursier.publish.sonatype.SonatypeApi
import coursier.publish.{Hooks, PublishRepository}

import java.util.concurrent.ScheduledExecutorService

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.cli.commands.util.ScalaCliSttpBackend

final case class RepoParams(
  repo: PublishRepository,
  targetRepoOpt: Option[String],
  hooks: Hooks,
  isIvy2LocalLike: Boolean,
  defaultParallelUpload: Boolean,
  supportsSig: Boolean,
  acceptsChecksums: Boolean,
  shouldAuthenticate: Boolean
) {
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
  def withAuth(authOpt: Option[Authentication]): RepoParams =
    authOpt.fold(this)(withAuth(_))
}

object RepoParams {

  def apply(
    repo: String,
    vcsUrlOpt: Option[String],
    workspace: os.Path,
    ivy2HomeOpt: Option[os.Path],
    isIvy2LocalLike: Boolean,
    es: ScheduledExecutorService,
    logger: Logger
  ): Either[BuildException, RepoParams] = either {
    repo match {
      case "ivy2-local" =>
        RepoParams.ivy2Local(ivy2HomeOpt)
      case "sonatype" | "central" | "maven-central" | "mvn-central" =>
        RepoParams.centralRepo("https://oss.sonatype.org", es, logger)
      case "sonatype-s01" | "central-s01" | "maven-central-s01" | "mvn-central-s01" =>
        RepoParams.centralRepo("https://s01.oss.sonatype.org", es, logger)
      case "github" =>
        value(RepoParams.gitHubRepo(vcsUrlOpt, workspace, logger))
      case repoStr if repoStr.startsWith("github:") && repoStr.count(_ == '/') == 1 =>
        val (org, name) = repoStr.stripPrefix("github:").split('/') match {
          case Array(org0, name0) => (org0, name0)
          case other              => sys.error(s"Cannot happen ('$repoStr' -> ${other.toSeq})")
        }
        RepoParams.gitHubRepoFor(org, name)
      case repoStr =>
        val repo0 = RepositoryParser.repositoryOpt(repoStr)
          .collect {
            case m: MavenRepository =>
              m
          }
          .getOrElse {
            val url =
              if (repoStr.contains("://")) repoStr
              else os.Path(repoStr, os.pwd).toNIO.toUri.toASCIIString
            MavenRepository(url)
          }

        RepoParams(
          PublishRepository.Simple(repo0),
          None,
          Hooks.dummy,
          isIvy2LocalLike,
          true,
          true,
          true,
          false
        )
    }
  }

  def centralRepo(base: String, es: ScheduledExecutorService, logger: Logger) = {
    val repo0   = PublishRepository.Sonatype(MavenRepository(base))
    val backend = ScalaCliSttpBackend.httpURLConnection(logger)
    val api     = SonatypeApi(backend, base + "/service/local", None, logger.verbosity)
    val hooks0 = Hooks.sonatype(
      repo0,
      api,
      logger.compilerOutputStream, // meh
      logger.verbosity,
      batch = coursier.paths.Util.useAnsiOutput(), // FIXME Get via logger
      es
    )
    RepoParams(
      repo0,
      Some("https://repo1.maven.org/maven2"),
      hooks0,
      false,
      true,
      true,
      true,
      true
    )
  }

  def gitHubRepoFor(org: String, name: String) =
    RepoParams(
      PublishRepository.Simple(MavenRepository(s"https://maven.pkg.github.com/$org/$name")),
      None,
      Hooks.dummy,
      false,
      false,
      false,
      false,
      true
    )

  def gitHubRepo(vcsUrlOpt: Option[String], workspace: os.Path, logger: Logger) = either {
    val orgNameFromVcsOpt = vcsUrlOpt.flatMap(GitRepo.maybeGhOrgName)

    val (org, name) = orgNameFromVcsOpt match {
      case Some(orgName) => orgName
      case None          => value(GitRepo.ghRepoOrgName(workspace, logger))
    }

    gitHubRepoFor(org, name)
  }

  def ivy2Local(ivy2HomeOpt: Option[os.Path]) = {
    val home = ivy2HomeOpt.getOrElse(os.home / ".ivy2")
    val base = home / "local"
    // not really a Maven repo…
    RepoParams(
      PublishRepository.Simple(MavenRepository(base.toNIO.toUri.toASCIIString)),
      None,
      Hooks.dummy,
      true,
      true,
      true,
      true,
      false
    )
  }

}
