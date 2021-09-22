package scala.build

import dev.dirs.{GetWinDirs, ProjectDirectories}

trait Directories {
  def localRepoDir: os.Path
  def binRepoDir: os.Path
  def completionsDir: os.Path
  def virtualProjectsDir: os.Path
  def bspSocketDir: os.Path
}

object Directories {

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
      // FIXME I would have preferred to use projDirs.dataLocalDir, but it seems either ipcsocket
      // or the bloop named socket support, or name sockets in general, aren't fine with it.
      os.Path(projDirs.cacheDir, Os.pwd) / "bsp-sockets"
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
  }

  def default(): Directories = {
    val getWinDirs: GetWinDirs =
      if (coursier.paths.Util.useJni())
        new GetWinDirs {
          def getWinDirs(guids: String*) =
            guids
              .map { guid =>
                coursier.jniutils.WindowsKnownFolders.knownFolderPath("{" + guid + "}")
              }
              .toArray
        }
      else
        GetWinDirs.powerShellBased

    OsLocations(ProjectDirectories.from(null, null, "ScalaCli", getWinDirs))
  }

  def under(dir: os.Path): Directories =
    SubDir(dir)
}
