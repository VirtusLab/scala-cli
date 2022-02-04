package scala.build

import os.Path

import scala.build.errors.BuildException
import scala.build.internal.Runner
import scala.util.Properties

final class CLangInstallException extends BuildException("Failed to Install CLang")

object CLangInstaller {

  def directories = scala.build.Directories.default()
  /*
  Installer based on micromamba attempts to install mamba and llvm based on that
  Following https://gist.github.com/wolfv/fe1ea521979973ab1d016d95a589dcde
   */
  val mambaApiUrl = "https://micromamba.snakepit.net/api/micromamba"
  def mambaBinaryUrl = Properties.osName match {
    case os if os startsWith "Windows"  => s"$mambaApiUrl/win-64/latest"
    case os if os startsWith "Mac OS X" => s"$mambaApiUrl/osx-64/latest"
    case os if os startsWith "Linux"    => s"$mambaApiUrl/linux-64/latest"
  }

  def install(microMambaArchive: Path, logger: Logger): Either[BuildException, Path] =
    install(microMambaArchive, directories.mambaBaseDir, logger = logger)

  def install(
    microMambaArchive: Path,
    mambaBaseDir: Path,
    logger: Logger
  ): Either[BuildException, Path] = {
    // TODO: we should relay on coursier to give us decompressed archive file but there is no bzip2 support
    // for ArchiveCache => https://github.com/coursier/coursier/blob/master/modules/cache/jvm/src/main/scala/coursier/cache/ArchiveCache.scala#L151

    TarArchive.decompress(os.read.inputStream(microMambaArchive), mambaBaseDir)
      .left.map(_ => new CLangInstallException)
      .right.map { miniMambaPath: Path =>
        val mambaPath = miniMambaPath / 'bin / "micromamba"

        if (!Properties.isWin)
          os.perms.set(mambaPath, "rwxr-xr-x")

        /*
        TODO: we need to have mamba initialized
        ./micromamba shell init -s bash -p ~/micromamba
        source ~/.bashrc
         */
        val installLLVM = s"$mambaPath install llvm -n base -c conda-forge -y".split(" ").toSeq
        Runner.run("install llvm", installLLVM, logger, cwd = Some(miniMambaPath))
        miniMambaPath
      }
  }
}
