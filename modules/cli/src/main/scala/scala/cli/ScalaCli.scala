package scala.cli

import caseapp.core.app.CommandsEntryPoint
import caseapp.core.help.RuntimeCommandsHelp

import scala.cli.commands._
import scala.cli.internal.Argv0
import scala.util.Properties

object ScalaCli extends CommandsEntryPoint {

  def actualDefaultCommand = Default

  val commands: Seq[ScalaCommand[_]] = Seq(
    About,
    AddPath,
    Bsp,
    Clean,
    Compile,
    Directories,
    InstallCompletions,
    Metabrowse,
    Repl,
    Package,
    Run,
    SetupIde,
    Test
  )

  lazy val progName                 = (new Argv0).get("scala-cli")
  override def description          = "Compile, run, package Scala code."
  final override def defaultCommand = Some(actualDefaultCommand)

  // FIXME Report this in case-app default NameFormatter
  override lazy val help: RuntimeCommandsHelp = {
    val parent = super.help
    parent.withDefaultHelp(
      parent.defaultHelp.withNameFormatter(actualDefaultCommand.nameFormatter)
    )
  }

  override def enableCompleteCommand    = true
  override def enableCompletionsCommand = true

  override def helpFormat = actualDefaultCommand.helpFormat

  private def isGraalvmNativeImage: Boolean =
    sys.props.contains("org.graalvm.nativeimage.imagecode")

  override def main(args: Array[String]): Unit = {

    if (Properties.isWin && isGraalvmNativeImage) {
      // The DLL loaded by LoadWindowsLibrary is statically linked in
      // the Scala CLI native image, no need to manually load it.
      coursier.jniutils.LoadWindowsLibrary.assumeInitialized()
    }

    if (Properties.isWin && System.console() != null && coursier.paths.Util.useJni())
      // Enable ANSI output in Windows terminal
      coursier.jniutils.WindowsAnsiTerminal.enableAnsiOutput()

    // quick hack, until the raw args are kept in caseapp.RemainingArgs by case-app
    actualDefaultCommand.anyArgs = args.nonEmpty

    commands.foreach {
      case c: NeedsArgvCommand => c.setArgv(progName +: args)
      case _                   =>
    }

    super.main(args)
  }
}
