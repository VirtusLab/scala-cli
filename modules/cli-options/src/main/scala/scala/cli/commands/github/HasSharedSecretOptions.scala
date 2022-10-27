package scala.cli.commands.github

import scala.cli.commands.LoggingOptions
import scala.cli.commands.common.HasLoggingOptions

trait HasSharedSecretOptions extends HasLoggingOptions {
  def shared: SharedSecretOptions
  override def logging: LoggingOptions = shared.logging
}
