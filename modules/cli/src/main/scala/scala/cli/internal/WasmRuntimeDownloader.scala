package scala.cli.internal

import coursier.cache.ArchiveCache
import coursier.util.Task

import java.util.Locale

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.internal.FetchExternalBinary
import scala.util.Properties

/** Resolves Deno binary for WASM execution.
  *
  * Deno is first looked up on the system PATH. If not found, it is downloaded from GitHub releases
  * and cached via Coursier's ArchiveCache.
  */
object WasmRuntimeDownloader {

  /** Returns the command to run Deno.
    *
    * First checks system PATH, otherwise downloads the binary.
    */
  def denoCommand(
    version: String,
    archiveCache: ArchiveCache[Task],
    logger: Logger
  ): Either[BuildException, Seq[String]] = either {
    findOnPath("deno") match {
      case Some(path) =>
        logger.debug(s"Using system deno at: $path")
        Seq(path)
      case None =>
        logger.message(s"Deno not found on PATH, downloading v$version...")
        val binary = value(fetchDeno(version, archiveCache, logger))
        Seq(binary.toString)
    }
  }

  /** Find an executable on the system PATH */
  private def findOnPath(name: String): Option[String] = {
    val exeName = if (Properties.isWin) s"$name.exe" else name
    sys.env.get("PATH").flatMap { pathEnv =>
      pathEnv.split(java.io.File.pathSeparator).view.map { dir =>
        val file = new java.io.File(dir, exeName)
        if (file.exists() && file.canExecute) Some(file.getAbsolutePath)
        else None
      }.find(_.isDefined).flatten
    }
  }

  private def detectOs(win: String, linux: String, mac: String): Either[BuildException, String] =
    if (Properties.isWin) Right(win)
    else if (Properties.isLinux) Right(linux)
    else if (Properties.isMac) Right(mac)
    else Left(new WasmRuntimeDownloadError(s"Unsupported OS: ${sys.props("os.name")}"))

  private def detectArch64(x86_64: String, aarch64: String): Either[BuildException, String] =
    sys.props("os.arch").toLowerCase(Locale.ROOT) match {
      case "amd64" | "x86_64"  => Right(x86_64)
      case "aarch64" | "arm64" => Right(aarch64)
      case other => Left(new WasmRuntimeDownloadError(s"Unsupported architecture: $other"))
    }

  /** Fetches Deno binary for the current platform.
    *
    * Deno releases are at:
    * https://github.com/denoland/deno/releases/download/v{version}/deno-{platform}.zip
    */
  private def fetchDeno(
    version: String,
    archiveCache: ArchiveCache[Task],
    logger: Logger
  ): Either[BuildException, os.Path] = either {
    val platform = value(denoPlatform)
    val url = s"https://github.com/denoland/deno/releases/download/v$version/deno-$platform.zip"

    val binaryOpt = value {
      FetchExternalBinary.fetchLauncher(
        url = url,
        changing = false,
        archiveCache = archiveCache,
        logger = logger,
        launcherPrefix = "deno",
        launcherPathOpt = None,
        makeExecutable = true
      )
    }

    binaryOpt.getOrElse {
      value(Left(new WasmRuntimeDownloadError(s"Could not download Deno v$version for $platform")))
    }
  }

  /** Platform suffix for Deno downloads */
  private def denoPlatform: Either[BuildException, String] = either {
    val arch = value(detectArch64("x86_64", "aarch64"))
    val os   = value(detectOs("pc-windows-msvc", "unknown-linux-gnu", "apple-darwin"))
    s"$arch-$os"
  }
}

class WasmRuntimeDownloadError(message: String) extends BuildException(message)
