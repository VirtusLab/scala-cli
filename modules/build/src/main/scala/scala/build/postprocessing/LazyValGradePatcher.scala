package scala.build.postprocessing

import coursier.LocalRepositories
import coursier.cache.FileCache
import dependency.*
import os.Path

import java.io.File
import java.security.MessageDigest

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.build.internal.CsLoggerUtil.*
import scala.build.options.BuildOptions
import scala.build.{Artifacts, Directories, Logger, Positioned}

object LazyValGradePatcher {

  private val cacheDir = Directories.directories.cacheDir / "lazyvalgrade"

  def transformClassPath(
    classPath: Seq[Path],
    options: BuildOptions,
    javaCommand: String,
    logger: Logger
  ): Either[BuildException, Seq[Path]] =
    if options.notForBloopOptions.lazyValGradeOpt.contains(true) then
      either {
        val toolClassPath = value(fetchToolClassPath(options, logger))
        value(patchedClassPath(classPath, javaCommand, toolClassPath, logger))
      }
    else Right(classPath)

  private def fetchToolClassPath(
    options: BuildOptions,
    logger: Logger
  ): Either[BuildException, Seq[Path]] =
    either {
      val cache        = options.internal.cache.getOrElse(FileCache())
      val repositories = value(options.finalRepositories) ++ Seq(LocalRepositories.ivy2Local)
      val scalaParams  = value(options.scalaParams).getOrElse(
        ScalaParameters(Constants.defaultScalaVersion)
      )
      val lazyValGradeDeps =
        Seq(
          dep"${Constants.lazyvalgradeOrganization}::${Constants.lazyvalgradeModuleName}:${Constants.lazyvalgradeVersion}"
        )
      val artifacts = value(
        Artifacts.artifacts(
          lazyValGradeDeps.map(Positioned.none),
          repositories,
          Some(scalaParams),
          logger,
          cache.withMessage(s"Downloading lazyvalgrade ${Constants.lazyvalgradeVersion}")
        )
      )
      artifacts.map(_._2)
    }

  private def patchedClassPath(
    classPath: Seq[Path],
    javaCommand: String,
    toolClassPath: Seq[Path],
    logger: Logger
  ): Either[BuildException, Seq[Path]] =
    either {
      val toolCp = toolClassPath.map(_.toString).mkString(File.pathSeparator)
      classPath.map { entry =>
        if entry.ext == "jar" then value(patchJar(entry, javaCommand, toolCp, logger))
        else entry
      }
    }

  private def patchJar(
    jar: Path,
    javaCommand: String,
    toolClassPath: String,
    logger: Logger
  ): Either[BuildException, Path] =
    either {
      val digest    = sha1(jar)
      val cachedDir = cacheDir / Constants.lazyvalgradeVersion / digest
      val cached    = cachedDir / jar.last
      if os.exists(cached) then cached
      else {
        os.makeDir.all(cachedDir)
        os.copy(jar, cached, replaceExisting = true, createFolders = true)
        val exitCode = os.proc(
          javaCommand,
          "-cp",
          toolClassPath,
          "lazyvalgrade.cli.Main",
          cached.toString
        ).call(
          stdout = os.Pipe,
          stderr = os.Pipe,
          check = false
        ).exitCode
        if exitCode != 0 then
          value(
            Left(new BuildException(s"Failed to patch lazy vals in $jar (exit code $exitCode)") {})
          )
        logger.debug(s"Patched lazy vals in $jar -> $cached")
        cached
      }
    }

  private def sha1(path: Path): String = {
    val md     = MessageDigest.getInstance("SHA-1")
    val buffer = new Array[Byte](8192)
    val stream = os.read.inputStream(path)
    try
      var read = stream.read(buffer)
      while read >= 0 do
        md.update(buffer, 0, read)
        read = stream.read(buffer)
    finally stream.close()
    md.digest().map("%02x".format(_)).mkString
  }
}
