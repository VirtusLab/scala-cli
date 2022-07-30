package scala.build.internal

import coursier.cache.{ArchiveCache, ArtifactError, CacheLogger}
import coursier.error.FetchError
import coursier.parse.RepositoryParser
import coursier.util.{Artifact, Task}

import java.util.Locale

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.{BuildException, FetchingDependenciesError, RepositoryFormatError}
import scala.build.internal.OsLibc
import scala.build.internal.Util.{DependencyOps, ModuleOps}
import scala.util.Properties

object FetchExternalBinary {

  def fetch(
    params: ExternalBinaryParams,
    archiveCache: ArchiveCache[Task],
    logger: Logger,
    javaCommand: () => String
  ): Either[BuildException, ExternalBinary] = either {

    val binaryOpt = value {
      fetchLauncher(
        params.binaryUrl,
        params.changing,
        archiveCache,
        logger,
        params.launcherPrefix
      )
    }

    binaryOpt match {
      case Some(binary) =>
        ExternalBinary.Native(binary)
      case None =>
        val extraRepositories0 = value {
          RepositoryParser.repositories(params.extraRepos)
            .either
            .left.map(errors => new RepositoryFormatError(errors))
        }

        val classPath = coursier.Fetch()
          .withCache(archiveCache.cache)
          .addDependencies(params.dependencies.map(_.toCs)*)
          .mapResolutionParams { params0 =>
            params0.addForceVersion(
              params.forcedVersions.map { case (m, v) => m.toCs -> v }*
            )
          }
          .addRepositories(extraRepositories0*)
          .run()(archiveCache.cache.ec)
          .map(os.Path(_, os.pwd))
        ExternalBinary.ClassPath(javaCommand(), classPath, params.mainClass)
    }
  }

  def fetchLauncher(
    url: String,
    changing: Boolean,
    archiveCache: ArchiveCache[Task],
    logger: Logger,
    launcherPrefix: String,
    launcherPathOpt: Option[os.RelPath] = None,
    makeExecutable: Boolean = true
  ): Either[BuildException, Option[os.Path]] = either {

    val artifact = Artifact(url).withChanging(changing)
    val res = archiveCache.cache.loggerOpt.getOrElse(CacheLogger.nop).use {
      logger.log(s"Getting $url")
      archiveCache.get(artifact)
        .unsafeRun()(archiveCache.cache.ec)
    }
    val fileOpt = res match {
      case Left(nf: ArtifactError.NotFound) =>
        logger.debug(s"$url not found ($nf)") // FIXME Log the whole stack trace of nf
        None
      case Left(err) =>
        val err0 = new FetchError.DownloadingArtifacts(Seq((artifact, err)))
        value(Left(new FetchingDependenciesError(err0, Nil)))
      case Right(f) => Some(os.Path(f, os.pwd))
    }

    fileOpt.map { f =>
      logger.debug(s"$url is available locally at $f")

      val launcher = launcherPathOpt match {
        case Some(launcherPath) =>
          f / launcherPath
        case None =>
          if (os.isDir(f)) {
            val dirContent = os.list(f)
            if (dirContent.length == 1) dirContent.head
            else dirContent.filter(_.last.startsWith(launcherPrefix)).head
          }
          else
            f
      }

      if (makeExecutable && !Properties.isWin)
        os.perms.set(launcher, "rwxr-xr-x")

      launcher
    }
  }

  def maybePlatformSuffix(supportsMusl: Boolean = true): Either[String, String] = {
    val arch = sys.props("os.arch").toLowerCase(Locale.ROOT) match {
      case "amd64" => "x86_64"
      case other   => other
    }
    val maybeOs =
      if (Properties.isWin) Right("pc-win32")
      else if (Properties.isLinux)
        Right {
          if (supportsMusl && OsLibc.isMusl.getOrElse(false))
            "pc-linux-static"
          else
            "pc-linux"
        }
      else if (Properties.isMac) Right("apple-darwin")
      else Left(s"Unrecognized OS: ${sys.props("os.name")}")
    maybeOs.map(os => s"$arch-$os")
  }

  def platformSuffix(supportsMusl: Boolean = true): String =
    maybePlatformSuffix(supportsMusl) match {
      case Left(err)    => sys.error(err)
      case Right(value) => value
    }
// Warning: somehow also in settings.sc in the build
  lazy val condaPlatform = {
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
}
