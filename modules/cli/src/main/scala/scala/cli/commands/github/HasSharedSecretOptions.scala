package scala.cli.commands.github

import scala.cli.commands.shared.{GlobalSuppressWarningOptions, HasGlobalOptions, LoggingOptions}

trait HasSharedSecretOptions extends HasGlobalOptions {
  def shared: SharedSecretOptions
  override def logging: LoggingOptions = shared.logging

  override def globalSuppressWarning: GlobalSuppressWarningOptions = shared.globalSuppressWarning
}
