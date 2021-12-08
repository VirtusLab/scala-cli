package scala.cli.commands

import caseapp.RemainingArgs

object Shebang extends ScalaCommand[ShebangOptions] {
  override def stopAtFirstUnrecognized: Boolean = true

  def run(options: ShebangOptions, args: RemainingArgs): Unit = {
    Run.run(
      options.runOptions,
      args.remaining.headOption.toSeq,
      args.remaining.drop(1).toSeq,
      () => None
    )
  }
}
