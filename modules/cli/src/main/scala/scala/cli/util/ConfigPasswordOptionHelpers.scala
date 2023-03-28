package scala.cli.util

import scala.build.errors.BuildException
import scala.build.options.publish.ConfigPasswordOption
import scala.cli.commands.SpecificationLevel
import scala.cli.commands.publish.ConfigUtil._
import scala.cli.config.{ConfigDb, Key, PasswordOption}
import scala.cli.errors.MissingConfigEntryError
import scala.cli.util.MaybeConfigPasswordOption

object ConfigPasswordOptionHelpers {

  implicit class ConfigPasswordOptionOps(private val opt: ConfigPasswordOption)
      extends AnyVal {
    def get(configDb: => ConfigDb): Either[BuildException, PasswordOption] =
      opt match {
        case a: ConfigPasswordOption.ActualOption =>
          Right(a.option.toConfig)
        case c: ConfigPasswordOption.ConfigOption =>
          val key = new Key.PasswordEntry(c.prefix, c.name, SpecificationLevel.IMPLEMENTATION)
          configDb.get(key).wrapConfigException.flatMap {
            case None        => Left(new MissingConfigEntryError(c.fullName))
            case Some(value) => Right(value)
          }
      }
  }

}
