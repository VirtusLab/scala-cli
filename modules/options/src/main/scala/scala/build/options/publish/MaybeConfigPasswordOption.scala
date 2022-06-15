package scala.build.options.publish

import scala.cli.signing.shared.PasswordOption

/** Can be either a [[PasswordOption]], or something like "config:…" pointing at a config entry */
sealed abstract class MaybeConfigPasswordOption extends Product with Serializable

object MaybeConfigPasswordOption {
  final case class ActualOption(option: PasswordOption) extends MaybeConfigPasswordOption
  final case class ConfigOption(fullName: String) extends MaybeConfigPasswordOption {
    private lazy val split  = fullName.split('.')
    def prefix: Seq[String] = split.dropRight(1).toSeq
    def name: String        = split.last
  }

  def parse(input: String): Either[String, MaybeConfigPasswordOption] =
    if (input.startsWith("config:"))
      Right(ConfigOption(input.stripPrefix("config:")))
    else
      PasswordOption.parse(input).map(ActualOption(_))
}
