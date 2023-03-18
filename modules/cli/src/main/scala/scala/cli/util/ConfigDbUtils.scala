package scala.cli.util

import scala.build.errors.BuildException
import scala.build.{Directories, Logger}
import scala.cli.commands.publish.ConfigUtil.wrapConfigException
import scala.cli.config.{ConfigDb, ConfigDbException, Key}

object ConfigDbUtils {
  lazy val configDb: Either[ConfigDbException, ConfigDb] =
    ConfigDb.open(Directories.directories.dbPath.toNIO).wrapConfigException

  extension [T](either: Either[Exception, T]) {
    private def handleConfigDbException(f: BuildException => Unit): Option[T] =
      either match
        case Left(e: BuildException) =>
          f(e)
          None
        case Left(e: Exception) =>
          f(new ConfigDbException(e))
          None
        case Right(value) => Some(value)
  }

  def getConfigDbOpt(logger: Logger): Option[ConfigDb] =
    configDb.handleConfigDbException(logger.debug)

  extension (db: ConfigDb) {
    def getOpt[T](configDbKey: Key[T], f: BuildException => Unit): Option[T] =
      db.get(configDbKey).handleConfigDbException(f).flatten
    def getOpt[T](configDbKey: Key[T], logger: Logger): Option[T] =
      getOpt(configDbKey, logger.debug(_))
    def getOpt[T](configDbKey: Key[T]): Option[T] =
      getOpt(configDbKey, _.printStackTrace(System.err))
  }

}
