package scala.cli.commands.shared

trait HasSharedOptions extends HasGlobalOptions {
  def shared: SharedOptions
  override def logging: LoggingOptions                             = shared.logging
  override def globalSuppressWarning: GlobalSuppressWarningOptions = shared.suppress.global
}
