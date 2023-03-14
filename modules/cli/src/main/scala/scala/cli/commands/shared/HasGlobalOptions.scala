package scala.cli.commands.shared

trait HasGlobalOptions {
  def logging: LoggingOptions
  def globalSuppressWarning: GlobalSuppressWarningOptions
}
