package scala.cli.commands.github

import scala.cli.commands.shared.{HasLoggingOptions, LoggingOptions}

trait HasSharedSecretOptions extends HasLoggingOptions {
  def shared: SharedSecretOptions
  override def logging: LoggingOptions = shared.logging
}
