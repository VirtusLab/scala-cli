package scala.cli.commands
package util

import coursier.cache.{CacheLogger, FileCache}

import scala.build.internal.{Constants, FetchExternalBinary}
import scala.build.options.BuildOptions
import scala.build.{Logger, Os}
import scala.cli.internal.CliLogger
import scala.concurrent.duration.Duration
import scala.util.Properties

trait CommonOps {
  implicit class SharedWorkspaceOptionsOps(v: SharedWorkspaceOptions) {
    def forcedWorkspaceOpt: Option[os.Path] =
      v.workspace
        .filter(_.trim.nonEmpty)
        .map(os.Path(_, Os.pwd))
  }

  implicit class LoggingOptionsOps(v: LoggingOptions) {
    def logger: Logger = cached(this)(new CliLogger(v.verbosity, v.quiet, v.progress, System.err))
  }

  implicit class SharedDirectoriesOptionsOps(v: SharedDirectoriesOptions) {

    def directories: scala.build.Directories = cached(this) {
      v.homeDirectory.filter(_.trim.nonEmpty) match {
        case None =>
          scala.build.Directories.default()
        case Some(homeDir) =>
          val homeDir0 = os.Path(homeDir, Os.pwd)
          scala.build.Directories.under(homeDir0)
      }
    }
  }

  implicit class CoursierOptionsOps(v: CoursierOptions) {
    import v._

    def coursierCache(logger: CacheLogger) = {
      var baseCache = FileCache().withLogger(logger)
      val ttlOpt    = ttl.map(_.trim).filter(_.nonEmpty).map(Duration(_))
      for (ttl0 <- ttlOpt)
        baseCache = baseCache.withTtl(ttl0)
      for (loc <- cache.filter(_.trim.nonEmpty))
        baseCache = baseCache.withLocation(loc)
      baseCache
    }
  }

  implicit class FmtOptionsOps(v: FmtOptions) {
    import v._
    def binaryUrl(versionMaybe: Option[String]): (String, Boolean) = {
      val defaultVersion = versionMaybe.getOrElse(Constants.defaultScalafmtVersion)
      val osArchSuffix0 = osArchSuffix.map(_.trim).filter(_.nonEmpty)
        .getOrElse(FetchExternalBinary.platformSuffix())
      val tag0           = scalafmtTag.getOrElse("v" + defaultVersion)
      val gitHubOrgName0 = scalafmtGithubOrgName.getOrElse("alexarchambault/scalafmt-native-image")
      val extension0     = if (Properties.isWin) ".zip" else ".gz"
      val url =
        s"https://github.com/$gitHubOrgName0/releases/download/$tag0/scalafmt-$osArchSuffix0$extension0"
      (url, !tag0.startsWith("v"))
    }

    def buildOptions: BuildOptions =
      shared.buildOptions(enableJmh = false, jmhVersion = None, ignoreErrors = false)

    def scalafmtCliOptions: List[String] =
      scalafmtArg ::: (if (check) List("--check") else Nil)
  }
}
