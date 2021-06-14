package scala.build

import coursier.cache.shaded.dirs.dev.dirs.ProjectDirectories

trait Directories {
  def localRepoDir: os.Path
  def completionsDir: os.Path
}

object Directories {

  final case class OsLocations(projDirs: ProjectDirectories) extends Directories {
    def localRepoDir: os.Path =
      os.Path(projDirs.cacheDir, Os.pwd) / "local-repo"
    def completionsDir: os.Path =
      os.Path(projDirs.dataLocalDir, Os.pwd) / "completions"
  }

  final case class SubDir(dir: os.Path) extends Directories {
    def localRepoDir: os.Path =
      dir / "cache" / "local-repo"
    def completionsDir: os.Path =
      dir / "data-local" / "completions"
  }

  def default(): Directories =
    // TODO When bumping the coursier version, pass the 4th argument too (more stable Windows env var stuff)
    OsLocations(ProjectDirectories.from(null, null, "ScalaCli"))

  def under(dir: os.Path): Directories =
    SubDir(dir)
}
