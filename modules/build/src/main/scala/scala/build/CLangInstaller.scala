package scala.build

import os.Path

import java.util.Locale

import scala.build.errors.BuildException
import scala.build.internal.Runner
import scala.util.Properties

final class CLangInstallException(message: String)
    extends BuildException(s"Failed to Install CLang: $message")

object CLangInstaller {

  def directories = scala.build.Directories.default()
  /*
  Installer based on micromamba attempts to install mamba and llvm based on that
  Following https://gist.github.com/wolfv/fe1ea521979973ab1d016d95a589dcde

  micromamba is available for 6 architectures: https://anaconda.org/search?q=micromamba
   */

  val mambaApiUrl = "https://micromamba.snakepit.net/api/micromamba"

  def mambaBinaryUrl = {
    val os = Properties.osName match {
      case os if os.startsWith("Windows")  => "win"
      case os if os.startsWith("Mac OS X") => "osx"
      case os if os.startsWith("Linux")    => "linux"
    }
    // parsing os.arch example =>  https://github.com/trustin/os-maven-plugin/blob/master/src/main/java/kr/motd/maven/os/Detector.java
    val arch = sys.props("os.arch").toLowerCase(Locale.ROOT)
    (os, arch) match {
      case ("win", "x86_64")    => Right(s"$mambaApiUrl/win-64/latest")
      case ("osx", "x86_64")    => Right(s"$mambaApiUrl/osx-64/latest")
      case ("osx", "arm64")     => Right(s"$mambaApiUrl/osx-arm64/latest")
      case ("linux", "x86_64")  => Right(s"$mambaApiUrl/linux-64/latest")
      case ("linux", "aarch64") => Right(s"$mambaApiUrl/linux-aarch64/latest")
      case ("linux", "ppc64le") => Right(s"$mambaApiUrl/linux-ppc64le/latest")
      case _ => Left(new CLangInstallException("Unsupported architecture/system combination."))
    }
  }

  def install(microMambaArchive: Path, logger: Logger): Either[BuildException, Path] =
    install(microMambaArchive, directories.mambaBaseDir, logger = logger)

  def install(
    microMambaArchive: Path,
    mambaBaseDir: Path,
    logger: Logger
  ): Either[BuildException, Path] =
    // TODO: we should relay on coursier to give us decompressed archive file but there is no bzip2 support yet
    // for ArchiveCache => https://github.com/coursier/coursier/blob/master/modules/cache/jvm/src/main/scala/coursier/cache/ArchiveCache.scala#L151

    TarArchive.decompress(os.read.inputStream(microMambaArchive), mambaBaseDir)
      .left.map(_ => new CLangInstallException("Failed to decompress minimamba archive."))
      .right.map { miniMambaPath: Path =>
        val mambaPath     = miniMambaPath / 'bin / "micromamba"
        val installScript = directories.mambaBaseDir / "installScript.sh"
        val baseDir       = directories.mambaBaseDir

        if (Properties.isWin)
          throw new CLangInstallException("Not Implemented yet.")

        os.perms.set(mambaPath, "rwxr-xr-x")

        val activateAndInstall =
          s"""
             |#!/bin/sh
             |$mambaPath -r $baseDir shell -s posix activate > $baseDir/activate_env
             |. $baseDir/activate_env
             |$mambaPath install llvm -r $baseDir -n base -c conda-forge -y
             |""".stripMargin
        os.write.over(installScript, activateAndInstall)
        Runner.run(
          "install llvm",
          s"sh $installScript".split(" ").toSeq,
          logger,
          cwd = Some(miniMambaPath)
        )

        miniMambaPath
      }
}
