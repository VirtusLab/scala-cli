package scala.cli.commands.shared

trait HasSharedOptions extends HasLoggingOptions {
  def shared: SharedOptions
  override def logging: LoggingOptions = shared.logging
}
