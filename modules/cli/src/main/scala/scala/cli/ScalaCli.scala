package scala.cli

import scala.cli.commands._

object ScalaCli extends ScalaCliBase {
  def actualDefaultCommand = Default
  val commands = ScalaCliCore.commands ++ Seq(
    Metabrowse
  )
}
