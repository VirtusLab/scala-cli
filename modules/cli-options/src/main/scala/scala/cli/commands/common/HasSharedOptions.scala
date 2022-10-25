package scala.cli.commands.common

import scala.cli.commands.{LoggingOptions, SharedOptions}

trait HasSharedOptions extends HasLoggingOptions {
  def shared: SharedOptions
  override def logging: LoggingOptions = shared.logging
}
