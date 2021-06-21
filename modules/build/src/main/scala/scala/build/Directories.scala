package scala.build

import coursier.cache.shaded.dirs.dev.dirs.ProjectDirectories

trait Directories {
  def localRepoDir: os.Path
  def completionsDir: os.Path
  def virtualProjectsDir: os.Path
  def bspSocketDir: os.Path
}

object Directories {

  final case class OsLocations(projDirs: ProjectDirectories) extends Directories {
    lazy val localRepoDir: os.Path =
      os.Path(projDirs.cacheDir, Os.pwd) / "local-repo"
    lazy val completionsDir: os.Path =
      os.Path(projDirs.dataLocalDir, Os.pwd) / "completions"
    lazy val virtualProjectsDir: os.Path =
      os.Path(projDirs.cacheDir, Os.pwd) / "virtual-projects"
    lazy val bspSocketDir: os.Path =
      // FIXME I would have preferred to use projDirs.dataLocalDir, but it seems either ipcsocket
      // or the bloop named socket support, or name sockets in general, aren't fine with it.
      os.Path(projDirs.cacheDir, Os.pwd) / "bsp-sockets"
  }

  final case class SubDir(dir: os.Path) extends Directories {
    lazy val localRepoDir: os.Path =
      dir / "cache" / "local-repo"
    lazy val completionsDir: os.Path =
      dir / "data-local" / "completions"
    lazy val virtualProjectsDir: os.Path =
      dir / "cache" / "virtual-projects"
    lazy val bspSocketDir: os.Path =
      dir / "data-local" / "bsp-sockets"
  }

  def default(): Directories =
    // TODO When bumping the coursier version, pass the 4th argument too (more stable Windows env var stuff)
    OsLocations(ProjectDirectories.from(null, null, "ScalaCli"))

  def under(dir: os.Path): Directories =
    SubDir(dir)
}
