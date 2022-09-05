package scala.build.options.publish

import scala.cli.signing.shared.PasswordOption

/** Can be either a [[PasswordOption]], or something like "config:…" pointing at a config entry */
sealed abstract class ConfigPasswordOption extends Product with Serializable

object ConfigPasswordOption {
  final case class ActualOption(option: PasswordOption) extends ConfigPasswordOption
  final case class ConfigOption(fullName: String) extends ConfigPasswordOption {
    private lazy val split  = fullName.split('.')
    def prefix: Seq[String] = split.dropRight(1).toSeq
    def name: String        = split.last
  }
}
