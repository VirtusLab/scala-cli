package scala.build

import coursier.cache.{ArchiveCache, FileCache}
import coursier.util.Artifact
import os.Path

import java.net.{HttpURLConnection, URL}
import java.util.Locale

import scala.build.errors.CLangInstallException
import scala.build.internal.Runner
import scala.util.{Properties, Try}

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
      .flatMap(binaryUrl => fetchAndDecompress(binaryUrl, logger))
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
      case _ =>
        Left(new CLangInstallException(s"Unsupported architecture/system combination: $os $arch"))
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

  def fetchAndDecompress(url: String, logger: Logger) = {
    val fileCache = FileCache().withLogger(logger.coursierLogger)
    val cache     = ArchiveCache().withCache(fileCache)
    val artifact  = Artifact(url).withChanging(false)
    cache.get(artifact).unsafeRun()(fileCache.ec).map(file => Path(file.toPath))
  }

  def doInstall(
    uncompressedArchive: os.Path,
    logger: Logger
  ) = Try {
    val baseDir       = directories.mambaBaseDir
    val mambaPath     = uncompressedArchive / 'bin / "micromamba"
    val installScript = directories.mambaBaseDir / "installScript.sh"

    if (Properties.isWin)
      throw new CLangInstallException("Not Implemented yet.")

    os.perms.set(mambaPath, "rwxr-xr-x")

    val activateAndInstall =
      s"""
         |#!/bin/sh
         |"$mambaPath" -r "$baseDir" shell -s posix activate > "$baseDir/activate_env"
         |. "$baseDir/activate_env"
         |"$mambaPath" install compilers -r "$baseDir" -n base -c conda-forge -y
         |""".stripMargin
    os.write.over(installScript, activateAndInstall)
    Runner.run(
      "unused",
      Seq("sh", installScript.toString),
      logger,
      cwd = Some(uncompressedArchive)
    )
    uncompressedArchive
  }.toEither
}
