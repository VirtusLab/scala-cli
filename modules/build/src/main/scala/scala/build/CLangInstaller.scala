package scala.build

import coursier.cache.FileCache
import coursier.util.{Artifact, Task}
import os.Path

import java.net.{HttpURLConnection, URL}
import java.util.Locale

import scala.build.EitherCps.either
import scala.build.errors.BuildException
import scala.build.internal.Runner
import scala.util.{Properties, Try}

final class CLangInstallException(message: String)
    extends BuildException(s"Failed to Install CLang: $message")

object CLangInstaller {
  val mambaApiUrl = "https://micromamba.snakepit.net/api/micromamba"

  def directories = scala.build.Directories.default()
  /*
  Installer based on micromamba attempts to install mamba and llvm based on that
  Following https://gist.github.com/wolfv/fe1ea521979973ab1d016d95a589dcde

  micromamba is available for 6 architectures: https://anaconda.org/search?q=micromamba
   */

  def install(logger: Logger) =
    platform
      .flatMap(resolveMicroMambaBinaryUrl(_))
      .flatMap(binaryUrl => fetchFile(binaryUrl, logger))
      .flatMap(archive => doInstall(archive, logger))

  def platform = {
    val os = Properties.osName match {
      case os if os.startsWith("Windows")  => "win"
      case os if os.startsWith("Mac OS X") => "osx"
      case os if os.startsWith("Linux")    => "linux"
    }
    // parsing os.arch example =>  https://github.com/trustin/os-maven-plugin/blob/master/src/main/java/kr/motd/maven/os/Detector.java
    val arch = sys.props("os.arch").toLowerCase(Locale.ROOT)
    (os, arch) match {
      case ("win", "x86_64")    => Right("win-64")
      case ("osx", "x86_64")    => Right("osx-64")
      case ("osx", "arm64")     => Right("osx-arm64")
      case ("linux", "x86_64")  => Right("linux-64")
      case ("linux", "aarch64") => Right("linux-aarch64")
      case ("linux", "ppc64le") => Right("linux-ppc64le")
      case _ => Left(new CLangInstallException("Unsupported architecture/system combination."))
    }
  }

  def resolveMicroMambaBinaryUrl(platform: String) = Try {
    val con: HttpURLConnection = new URL(s"$mambaApiUrl/$platform/latest")
      .openConnection()
      .asInstanceOf[HttpURLConnection]
    con.setInstanceFollowRedirects(false)
    con.connect()
    if (con.getResponseCode != 307)
      new CLangInstallException("Failed to resolve micromamba archive url")
    s"https:${con.getHeaderField("Location")}"
  }.toEither

  def fetchFile(url: String, logger: Logger) = either {
    val cache = FileCache().withLogger(logger.coursierLogger)
    cache.logger.use {
      cache.file(Artifact(url).withChanging(true)).run.flatMap {
        case Left(e)  => Task.fail(new Exception(e))
        case Right(f) => Task.point(os.Path(f, os.pwd))
      }.unsafeRun()(cache.ec)
    }
  }.left.map(_ => new CLangInstallException("Failed to fetch Mamba binary."))

  def doInstall(
    microMambaArchive: Path,
    logger: Logger
  ): Either[BuildException, Path] =
    // TODO: we should relay on coursier to give us decompressed archive file but there is no bzip2 support yet
    // for ArchiveCache => https://github.com/coursier/coursier/blob/master/modules/cache/jvm/src/main/scala/coursier/cache/ArchiveCache.scala#L151

    TarArchive.decompress(os.read.inputStream(microMambaArchive), directories.mambaBaseDir)
      .left.map(_ => new CLangInstallException("Failed to decompress minimamba archive."))
      .right.map { miniMambaPath: Path =>
        val baseDir       = directories.mambaBaseDir
        val mambaPath     = miniMambaPath / 'bin / "micromamba"
        val installScript = directories.mambaBaseDir / "installScript.sh"

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
