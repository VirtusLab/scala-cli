package scala.cli

import scala.cli.commands._

object ScalaCliLight extends ScalaCliBase {
  val commands = Seq(
    About,
    Clean,
    Compile,
    InstallCompletions,
    Repl,
    Package,
    Run,
    Test
  )
}
