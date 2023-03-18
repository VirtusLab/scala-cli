package scala.cli.commands.github

import scala.cli.commands.shared.{GlobalOptions, HasGlobalOptions}

trait HasSharedSecretOptions extends HasGlobalOptions {
  def shared: SharedSecretOptions

  override def global: GlobalOptions = shared.global
}
