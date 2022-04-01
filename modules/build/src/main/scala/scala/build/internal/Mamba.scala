package scala.build.internal

import coursier.cache.ArchiveCache
import coursier.util.Task

import java.io.InputStream
import java.util.Locale

import scala.build.EitherCps.{either, value}
import scala.build.errors.{BuildException, MicroMambaError}
import scala.build.internal.FetchExternalBinary
import scala.build.options.ScalaNativeOptions
import scala.build.{Directories, Logger}
import scala.util.Properties

object Mamba {
  def launcher(
    microMambaVersion: String,
    microMambaSuffix: String,
    condaPlatform: String,
    cache: ArchiveCache[Task],
    logger: Logger
  ): Either[BuildException, os.Path] = either {
    val url =
      s"https://anaconda.org/conda-forge/micromamba/$microMambaVersion/download/$condaPlatform/micromamba-$microMambaVersion$microMambaSuffix.tar.bz2"
    val distribPath = value(FetchExternalBinary.fetch(url, changing = false, cache, logger))
    val ext         = if (Properties.isWin) ".exe" else ""
    val relPath     = os.rel / "bin" / s"micromamba$ext"
    val microMamba  = distribPath / relPath
    if (!os.exists(microMamba))
      value(Left(new MicroMambaError(s"Expected $relPath to exist under $distribPath")))
    microMamba
  }

  // Warning: somehow also in settings.sc in the build
  lazy val localPlatform = {
    val mambaOs =
      if (Properties.isWin) "win"
      else if (Properties.isMac) "osx"
      else if (Properties.isLinux) "linux"
      else sys.error(s"Unsupported mamba OS: ${sys.props("os.name")}")
    val arch = sys.props("os.arch").toLowerCase(Locale.ROOT)
    val mambaArch = arch match {
      case "x86_64" | "amd64"  => "64"
      case "arm64" | "aarch64" => "arm64"
      case "ppc64le"           => "ppc64le"
      case _ =>
        sys.error(s"Unsupported mamba architecture: $arch")
    }
    s"$mambaOs-$mambaArch"
  }

  private def scalaNativeCondaLockFileContent(condaPlatform: String): Array[Byte] = {
    val resPath = s"scala/build/internal/mamba/$condaPlatform-packages.txt"
    val res     = Thread.currentThread().getContextClassLoader.getResource(resPath)
    if (res == null)
      sys.error(s"Error: $resPath resource not found")
    var is: InputStream = null
    try {
      is = res.openStream()
      is.readAllBytes()
    }
    finally
      if (is != null)
        is.close()
  }

  def managedNativePrefix(
    microMambaVersion: String,
    microMambaSuffix: String,
    condaPlatform: String,
    cache: ArchiveCache[Task],
    logger: Logger,
    directories: Directories
  ): Either[BuildException, os.Path] = either {
    val condaEnvsDir = directories.condaEnvsDir
    val dir          = condaEnvsDir / "native" / Constants.nativeCondaLockFileChecksum

    // FIXME Concurrency
    if (!os.isDir(dir)) {
      val content      = scalaNativeCondaLockFileContent(condaPlatform)
      val lockFilePath = os.temp(content, prefix = "packages", suffix = ".txt")

      val launcher0 = value {
        launcher(microMambaVersion, microMambaSuffix, condaPlatform, cache, logger)
      }

      os.proc(launcher0, "create", "-p", dir, "-c", "conda-forge", "-y", "-f", lockFilePath)
        .call(stdin = os.Inherit, stdout = os.Inherit)

      os.remove(lockFilePath)
    }

    dir
  }

  def updateOptions(
    cache: ArchiveCache[Task],
    logger: Logger,
    directories: Directories
  )(
    options: ScalaNativeOptions
  ): Either[BuildException, ScalaNativeOptions] = either {
    if (options.useManagedClang.getOrElse(false)) {
      val microMambaVersion = options.finalMicroMambaVersion
      val microMambaSuffix  = options.finalMicroMambaSuffix
      val condaPlatform     = options.condaPlatform.getOrElse(localPlatform)
      val prefix = value(managedNativePrefix(
        microMambaVersion,
        microMambaSuffix,
        condaPlatform,
        cache,
        logger,
        directories
      ))
      val ext = if (Properties.isWin) ".exe" else ""
      // not sure about Windows
      val clang   = prefix / "bin" / s"clang$ext"
      val clangpp = prefix / "bin" / s"clang++$ext"
      options.copy(
        clang = Some(clang.toString),
        clangpp = Some(clangpp.toString),
        compileOptions =
          if (Properties.isLinux && options.compileDefaults.getOrElse(true))
            options.compileOptions ++ Seq(
              s"-I${prefix / "x86_64-conda-linux-gnu" / "sysroot" / "usr" / "include"}"
            )
          else
            options.compileOptions
      )
    }
    else
      options
  }
}
