package scala.cli.util

import scala.build.errors.BuildException
import scala.build.options.publish.ConfigPasswordOption
import scala.cli.commands.publish.ConfigUtil._
import scala.cli.commands.publish.MaybeConfigPasswordOption
import scala.cli.config.{ConfigDb, Key}
import scala.cli.errors.MissingConfigEntryError
import scala.cli.signing.shared.PasswordOption

object ConfigPasswordOptionHelpers {

  implicit class ConfigPasswordOptionOps(private val opt: ConfigPasswordOption)
      extends AnyVal {
    def get(configDb: => ConfigDb): Either[BuildException, PasswordOption] =
      opt match {
        case a: ConfigPasswordOption.ActualOption =>
          Right(a.option)
        case c: ConfigPasswordOption.ConfigOption =>
          val key = new Key.PasswordEntry(c.prefix, c.name)
          configDb.get(key).wrapConfigException.flatMap {
            case None        => Left(new MissingConfigEntryError(c.fullName))
            case Some(value) => Right(value)
          }
      }
  }

}
