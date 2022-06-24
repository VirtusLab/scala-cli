package scala.cli.util

import scala.build.errors.BuildException
import scala.build.options.publish.MaybeConfigPasswordOption
import scala.cli.config.{ConfigDb, Key}
import scala.cli.errors.MissingConfigEntryError
import scala.cli.signing.shared.PasswordOption

object MaybeConfigPasswordOptionHelpers {

  implicit class MaybeConfigPasswordOptionOps(private val opt: MaybeConfigPasswordOption)
      extends AnyVal {
    def get(configDb: => ConfigDb): Either[BuildException, PasswordOption] =
      opt match {
        case a: MaybeConfigPasswordOption.ActualOption =>
          Right(a.option)
        case c: MaybeConfigPasswordOption.ConfigOption =>
          val key = new Key.PasswordEntry(c.prefix, c.name)
          configDb.get(key).flatMap {
            case None        => Left(new MissingConfigEntryError(c.fullName))
            case Some(value) => Right(value)
          }
      }
  }

}
