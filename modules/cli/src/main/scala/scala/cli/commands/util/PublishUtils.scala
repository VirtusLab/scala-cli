package scala.cli.commands.util

import scala.build.options.publish.ConfigPasswordOption
import scala.cli.util.MaybeConfigPasswordOption

object PublishUtils {

  implicit class ConfigPasswordOptionsOps(v: MaybeConfigPasswordOption) {
    def configPasswordOptions() =
      v match {
        case MaybeConfigPasswordOption.ActualOption(option) =>
          ConfigPasswordOption.ActualOption(option)
        case MaybeConfigPasswordOption.ConfigOption(fullName) =>
          ConfigPasswordOption.ConfigOption(fullName)
      }
  }
}
