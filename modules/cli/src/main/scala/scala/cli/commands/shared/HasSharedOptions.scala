package scala.cli.commands.shared

trait HasSharedOptions extends HasGlobalOptions {
  def shared: SharedOptions
  override def global: GlobalOptions = shared.global
}
