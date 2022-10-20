package scala.build

import coursier.cache.shaded.dirs.{GetWinDirs, ProjectDirectories}

import scala.build.internal.JniGetWinDirs
import scala.util.Properties

trait Directories {
  def localRepoDir: os.Path
  def binRepoDir: os.Path
  def completionsDir: os.Path
  def virtualProjectsDir: os.Path
  def bspSocketDir: os.Path
  def bloopDaemonDir: os.Path
  def bloopWorkingDir: os.Path
  def secretsDir: os.Path
  def cacheDir: os.Path

  final def dbPath: os.Path =
    Option(System.getenv("SCALA_CLI_CONFIG"))
      .filter(_.trim.nonEmpty)
      .map(os.Path(_, os.pwd))
      .getOrElse(secretsDir / Directories.defaultDbFileName)
}

object Directories {

  def defaultDbFileName: String =
    "config.json"

  final case class OsLocations(projDirs: ProjectDirectories) extends Directories {
    lazy val localRepoDir: os.Path =
      os.Path(projDirs.cacheDir, Os.pwd) / "local-repo"
    lazy val binRepoDir: os.Path =
      os.Path(localRepoDir, Os.pwd) / "bin"
    lazy val completionsDir: os.Path =
      os.Path(projDirs.dataLocalDir, Os.pwd) / "completions"
    lazy val virtualProjectsDir: os.Path =
      os.Path(projDirs.cacheDir, Os.pwd) / "virtual-projects"
    lazy val bspSocketDir: os.Path =
      // FIXME I would have preferred to use projDirs.dataLocalDir, but it seems named socket
      // support, or name sockets in general, aren't fine with it.
      os.Path(projDirs.cacheDir, Os.pwd) / "bsp-sockets"
    lazy val bloopDaemonDir: os.Path =
      bloopWorkingDir / "daemon"
    lazy val bloopWorkingDir: os.Path = {
      val baseDir =
        if (Properties.isMac) projDirs.cacheDir
        else projDirs.dataLocalDir
      os.Path(baseDir, Os.pwd) / "bloop"
    }
    lazy val secretsDir: os.Path =
      os.Path(projDirs.dataLocalDir, Os.pwd) / "secrets"

    lazy val cacheDir: os.Path =
      os.Path(projDirs.cacheDir, os.pwd)
  }

  final case class SubDir(dir: os.Path) extends Directories {
    lazy val localRepoDir: os.Path =
      dir / "cache" / "local-repo"
    lazy val binRepoDir: os.Path =
      localRepoDir / "bin"
    lazy val completionsDir: os.Path =
      dir / "data-local" / "completions"
    lazy val virtualProjectsDir: os.Path =
      dir / "cache" / "virtual-projects"
    lazy val bspSocketDir: os.Path =
      dir / "data-local" / "bsp-sockets"
    lazy val bloopDaemonDir: os.Path =
      bloopWorkingDir / "daemon"
    lazy val bloopWorkingDir: os.Path =
      dir / "data-local" / "bloop"
    lazy val secretsDir: os.Path =
      dir / "data-local" / "secrets"
    lazy val cacheDir: os.Path =
      dir / "cache"
  }

  def default(): Directories = {
    val getWinDirs: GetWinDirs =
      if (coursier.paths.Util.useJni())
        new JniGetWinDirs
      else
        GetWinDirs.powerShellBased

    OsLocations(ProjectDirectories.from(null, null, "ScalaCli", getWinDirs))
  }

  def under(dir: os.Path): Directories =
    SubDir(dir)
}
