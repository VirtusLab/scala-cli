package scala.cli.commands.shared

import scala.build.errors.BuildException

trait HasSharedWatchOptions { this: HasSharedOptions =>
  def watch: SharedWatchOptions

  def buildOptions(ignoreErrors: Boolean =
    false): Either[BuildException, scala.build.options.BuildOptions] =
    shared.buildOptions(ignoreErrors = ignoreErrors, watchOptions = watch)
}
