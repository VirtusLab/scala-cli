package scala.cli.commands.publish

import scala.util.Properties

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
}
