package scala.cli.commands.publish

import scala.cli.config.ConfigDbException

object ConfigUtil {

  extension [T](value: Either[Exception, T]) {
    def wrapConfigException: Either[ConfigDbException, T] =
      value.left.map(ConfigDbException(_))
  }

}
