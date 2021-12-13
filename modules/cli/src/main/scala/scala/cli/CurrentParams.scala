package scala.cli

// Kind of meh to keep stuff in a global mutable state like this.
// This is only used by the stacktrace persisting stuff in ScalaCli.main
object CurrentParams {
  var workspaceOpt = Option.empty[os.Path]
  var verbosity    = 0
}
