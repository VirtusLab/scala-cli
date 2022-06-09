package scala.cli.commands
package util

import coursier.cache.{CacheLogger, FileCache}
import sttp.model.Uri

import scala.build.{Logger, Os}
import scala.cli.commands.pgp.SharedPgpPushPullOptions
import scala.cli.internal.CliLogger
import scala.concurrent.duration.Duration

object CommonOps {
  implicit class SharedWorkspaceOptionsOps(private val v: SharedWorkspaceOptions) extends AnyVal {
    def forcedWorkspaceOpt: Option[os.Path] =
      v.workspace
        .filter(_.trim.nonEmpty)
        .map(os.Path(_, Os.pwd))
  }

  implicit class LoggingOptionsOps(private val v: LoggingOptions) extends AnyVal {
    def logger: Logger = cached(v)(new CliLogger(v.verbosity, v.quiet, v.progress, System.err))
  }

  implicit class SharedDirectoriesOptionsOps(private val v: SharedDirectoriesOptions)
      extends AnyVal {

    def directories: scala.build.Directories = cached(v) {
      v.homeDirectory.filter(_.trim.nonEmpty) match {
        case None =>
          scala.build.Directories.default()
        case Some(homeDir) =>
          val homeDir0 = os.Path(homeDir, Os.pwd)
          scala.build.Directories.under(homeDir0)
      }
    }
  }

  implicit class CoursierOptionsOps(private val v: CoursierOptions) extends AnyVal {
    import v._

    private def validateChecksums =
      coursierValidateChecksums.getOrElse(true)

    def coursierCache(logger: CacheLogger) = {
      var baseCache = FileCache().withLogger(logger)
      if (!validateChecksums)
        baseCache = baseCache.withChecksums(Nil)
      val ttlOpt = ttl.map(_.trim).filter(_.nonEmpty).map(Duration(_))
      for (ttl0 <- ttlOpt)
        baseCache = baseCache.withTtl(ttl0)
      for (loc <- cache.filter(_.trim.nonEmpty))
        baseCache = baseCache.withLocation(loc)
      baseCache
    }
  }

  implicit class SharedPgpPushPullOptionsOps(private val options: SharedPgpPushPullOptions)
      extends AnyVal {
    def keyServerUriOptOrExit(logger: Logger): Option[Uri] =
      options.keyServer
        .filter(_.trim.nonEmpty)
        .lastOption
        .map { addr =>
          Uri.parse(addr) match {
            case Left(err) =>
              if (logger.verbosity >= 0)
                System.err.println(s"Error parsing key server address '$addr': $err")
              sys.exit(1)
            case Right(uri) => uri
          }
        }
  }
}
