package scala.cli.commands.publish

import org.eclipse.jgit.api.Git

import scala.build.Logger
import scala.jdk.CollectionConverters._
import scala.util.{Properties, Using}

object GitRepo {

  private lazy val user = os.owner(os.home)
  private def trusted(path: os.Path): Boolean =
    if (Properties.isWin)
      path.toIO.canWrite()
    else
      os.owner(path) == user

  def gitRepoOpt(workspace: os.Path): Option[os.Path] =
    if (trusted(workspace))
      if (os.isDir(workspace / ".git")) Some(workspace)
      else if (workspace.segmentCount > 0)
        gitRepoOpt(workspace / os.up)
      else
        None
    else
      None

  def ghRepoOrgName(
    workspace: os.Path,
    logger: Logger
  ): Either[GitRepoError, (String, String)] =
    gitRepoOpt(workspace) match {
      case Some(repo) =>
        remotes(repo, logger) match {
          case Seq() =>
            Left(new GitRepoError(s"Cannot determine GitHub organization and name for $workspace"))
          case Seq((_, orgName)) =>
            Right(orgName)
          case more =>
            val map = more.toMap
            map.get("upstream").orElse(map.get("origin")).toRight {
              new GitRepoError(
                s"Cannot determine default GitHub organization and name for $workspace"
              )
            }
        }
      case None =>
        Left(new GitRepoError(s"$workspace is not in a git repository"))
    }

  private def remotes(repo: os.Path, logger: Logger): Seq[(String, (String, String))] = {

    val remoteList = Using.resource(Git.open(repo.toIO)) { git =>
      git.remoteList().call().asScala
    }
    logger.debug(s"Found ${remoteList.length} remotes in Git repo $repo")

    remoteList
      .iterator
      .flatMap { remote =>
        val name = remote.getName
        remote
          .getURIs
          .asScala
          .iterator
          .map(_.toASCIIString)
          .flatMap(maybeGhOrgName)
          .map((name, _))
      }
      .toVector
  }

  def maybeGhRepoOrgName(
    workspace: os.Path,
    logger: Logger
  ): Option[(String, String)] =
    gitRepoOpt(workspace).flatMap { repo =>
      remotes(repo, logger) match {
        case Seq() =>
          logger.debug(s"No GitHub remote found in $workspace")
          None
        case Seq((_, orgName)) =>
          Some(orgName)
        case more =>
          val map = more.toMap
          val res = map.get("upstream").orElse(map.get("origin"))
          if (res.isEmpty)
            new GitRepoError(
              s"Cannot determine default GitHub organization and name for $workspace"
            )
          res
      }
    }

  def maybeGhOrgName(uri: String): Option[(String, String)] =
    if (uri.startsWith("https://github.com/")) {
      val pathPart = uri.stripPrefix("https://github.com/").stripSuffix(".git")
      pathPart.split("/") match {
        case Array(org, name) => Some((org, name))
        case _                => None
      }
    }
    else
      None
}
