package scala.cli

import scala.cli.commands._

object ScalaCliCore extends ScalaCliBase {
  def actualDefaultCommand = DefaultCore
  val commands = Seq(
    About,
    AddPath,
    Bsp,
    Clean,
    Compile,
    Directories,
    InstallCompletions,
    Repl,
    Package,
    Run,
    Test
  )
}
