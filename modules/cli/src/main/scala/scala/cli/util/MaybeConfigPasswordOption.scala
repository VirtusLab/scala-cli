package scala.cli.util

import scala.build.options.publish.ConfigPasswordOption
import scala.cli.signing.shared.PasswordOption

/** Can be either a [[PasswordOption]], or something like "config:…" pointing at a config entry */
sealed abstract class MaybeConfigPasswordOption extends Product with Serializable {
  def configPasswordOptions() =
    this match {
      case MaybeConfigPasswordOption.ActualOption(option) =>
        ConfigPasswordOption.ActualOption(option)
      case MaybeConfigPasswordOption.ConfigOption(fullName) =>
        ConfigPasswordOption.ConfigOption(fullName)
    }
}

object MaybeConfigPasswordOption {
  final case class ActualOption(option: PasswordOption) extends MaybeConfigPasswordOption
  final case class ConfigOption(fullName: String)       extends MaybeConfigPasswordOption

  def parse(input: String): Either[String, MaybeConfigPasswordOption] =
    if (input.startsWith("config:"))
      Right(ConfigOption(input.stripPrefix("config:")))
    else
      PasswordOption.parse(input).map(ActualOption(_))
}
