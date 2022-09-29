package scala.cli.commands.publish

import scala.cli.config.ConfigDbException

object ConfigUtil {

  extension [T](sec: scala.cli.signing.shared.Secret[T]) {
    def toConfig: scala.cli.config.Secret[T] =
      scala.cli.config.Secret(sec.value)
  }
  extension [T](sec: scala.cli.config.Secret[T]) {
    def toCliSigning: scala.cli.signing.shared.Secret[T] =
      scala.cli.signing.shared.Secret(sec.value)
  }
  extension (opt: scala.cli.signing.shared.PasswordOption) {
    def toConfig: scala.cli.config.PasswordOption =
      opt match {
        case v: scala.cli.signing.shared.PasswordOption.Value =>
          scala.cli.config.PasswordOption.Value(v.value.toConfig)
        case v: scala.cli.signing.shared.PasswordOption.Env =>
          scala.cli.config.PasswordOption.Env(v.name)
        case v: scala.cli.signing.shared.PasswordOption.File =>
          scala.cli.config.PasswordOption.File(v.path.toNIO)
        case v: scala.cli.signing.shared.PasswordOption.Command =>
          scala.cli.config.PasswordOption.Command(v.command)
      }
  }
  extension (opt: scala.cli.config.PasswordOption) {
    def toCliSigning: scala.cli.signing.shared.PasswordOption =
      opt match {
        case v: scala.cli.config.PasswordOption.Value =>
          scala.cli.signing.shared.PasswordOption.Value(v.value.toCliSigning)
        case v: scala.cli.config.PasswordOption.Env =>
          scala.cli.signing.shared.PasswordOption.Env(v.name)
        case v: scala.cli.config.PasswordOption.File =>
          scala.cli.signing.shared.PasswordOption.File(os.Path(v.path, os.pwd))
        case v: scala.cli.config.PasswordOption.Command =>
          scala.cli.signing.shared.PasswordOption.Command(v.command)
      }
  }

  extension [T](value: Either[Exception, T]) {
    def wrapConfigException: Either[ConfigDbException, T] =
      value.left.map(ConfigDbException(_))
  }

}
