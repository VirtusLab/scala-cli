package scala.cli.commands.util

import scala.cli.commands.{
  DefaultOptions,
  ReplOptions,
  RunOptions,
  SharedOptions,
  SharedReplOptions,
  SharedRunOptions
}

object DefaultOptionsUtil {

  extension (sharedRunOptions: SharedRunOptions) {
    def runOptions(sharedOptions: SharedOptions): RunOptions =
      RunOptions(sharedOptions, sharedRunOptions)
  }

  extension (sharedReplOptions: SharedReplOptions) {
    def replOptions(sharedOptions: SharedOptions): ReplOptions =
      ReplOptions(sharedOptions, sharedReplOptions)
  }

  extension (defaultOptions: DefaultOptions) {
    def runOptions: RunOptions   = defaultOptions.sharedRun.runOptions(defaultOptions.shared)
    def replOptions: ReplOptions = defaultOptions.sharedRepl.replOptions(defaultOptions.shared)
  }
}
