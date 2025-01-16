package scala.build

import coursier.cache.shaded.dirs.ProjectDirectories
import coursier.cache.shaded.dirs.impl.Windows
import coursier.cache.shaded.dirs.jni.WindowsJni

import java.util.function.Supplier

import scala.build.errors.ConfigDbException
import scala.build.internals.EnvVar
import scala.cli.config.ConfigDb
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
    EnvVar.ScalaCli.config.valueOpt
      .filter(_.trim.nonEmpty)
      .map(os.Path(_, os.pwd))
      .getOrElse(secretsDir / Directories.defaultDbFileName)

  lazy val configDb = ConfigDb.open(dbPath.toNIO).left.map(ConfigDbException(_))
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
    val windows: Supplier[Windows] =
      if coursier.paths.Util.useJni() then WindowsJni.getJdkAwareSupplier
      else Windows.getDefaultSupplier
    OsLocations(ProjectDirectories.from(null, null, "ScalaCli", windows))
  }

  def under(dir: os.Path): Directories =
    SubDir(dir)

  lazy val directories: Directories =
    EnvVar.ScalaCli.home.valueOpt.filter(_.trim.nonEmpty) match {
      case None =>
        scala.build.Directories.default()
      case Some(homeDir) =>
        val homeDir0 = os.Path(homeDir, Os.pwd)
        scala.build.Directories.under(homeDir0)
    }
}
