package scala.cli

import caseapp.core.app.CommandsEntryPoint

import scala.cli.commands._
import scala.cli.internal.Argv0
import scala.util.Properties

abstract class ScalaCliBase extends CommandsEntryPoint {

  def actualDefaultCommand: DefaultBase

  lazy val progName = (new Argv0).get("scala")
  override def description = "Compile, run, package Scala code."
  final override def defaultCommand = Some(actualDefaultCommand)

  override def enableCompleteCommand = true
  override def enableCompletionsCommand = true

  override def helpFormat = actualDefaultCommand.helpFormat

  override def main(args: Array[String]): Unit = {

    if (Properties.isWin && System.console() != null)
      // Enable ANSI output in Windows terminal
      coursier.jniutils.WindowsAnsiTerminal.enableAnsiOutput()

    // quick hack, until the raw args are kept in caseapp.RemainingArgs by case-app
    actualDefaultCommand.anyArgs = args.nonEmpty

    super.main(args)
  }
}
