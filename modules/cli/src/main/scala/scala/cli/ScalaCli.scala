package scala.cli

import caseapp.core.app.CommandsEntryPoint
import caseapp.core.help.RuntimeCommandsHelp

import java.nio.file.InvalidPathException

import scala.cli.commands._
import scala.cli.internal.Argv0
import scala.util.Properties

object ScalaCli extends CommandsEntryPoint {

  def actualDefaultCommand = Default

  val commands: Seq[ScalaCommand[_]] = Seq(
    About,
    AddPath,
    BloopExit,
    BloopStart,
    Bsp,
    Clean,
    Compile,
    Directories,
    Export,
    Fmt,
    HelpCmd,
    InstallCompletions,
    InstallHome,
    Metabrowse,
    Repl,
    Package,
    Run,
    SetupIde,
    Shebang,
    Test,
    Update,
    Version
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

  private def isShebangFile(arg: String): Boolean = {
    val pathOpt =
      try Some(os.Path(arg, os.pwd))
      catch {
        case _: InvalidPathException => None
      }
    pathOpt.filter(os.isFile(_)).filter(_.toIO.canRead).exists { path =>
      val content = os.read(path) // FIXME Charset?
      content.startsWith(s"#!/usr/bin/env $progName" + System.lineSeparator())
    }
  }

  private def partitionArgs(args: Array[String]): (Array[String], Array[String]) = {
    val systemProps = args.takeWhile(_.startsWith("-D"))
    (systemProps, args.drop(systemProps.size))
  }

  private def setSystemProps(systemProps: Array[String]): Unit = {
    systemProps.map(_.stripPrefix("-D")).foreach { prop =>
      prop.split("=", 2) match {
        case Array(key, value) =>
          System.setProperty(key, value.stripPrefix("=")) // todo log
      }
    }
  }

  override def main(args: Array[String]): Unit = {
    val (systemProps, scalaCliArgs) = partitionArgs(args)
    setSystemProps(systemProps)

    if (Properties.isWin && isGraalvmNativeImage)
      // The DLL loaded by LoadWindowsLibrary is statically linked in
      // the Scala CLI native image, no need to manually load it.
      coursier.jniutils.LoadWindowsLibrary.assumeInitialized()

    if (isGraalvmNativeImage)
      org.scalasbt.ipcsocket.NativeLoader.assumeLoaded()

    if (Properties.isWin && System.console() != null && coursier.paths.Util.useJni())
      // Enable ANSI output in Windows terminal
      coursier.jniutils.WindowsAnsiTerminal.enableAnsiOutput()

    // quick hack, until the raw args are kept in caseapp.RemainingArgs by case-app
    actualDefaultCommand.anyArgs = scalaCliArgs.nonEmpty

    commands.foreach {
      case c: NeedsArgvCommand => c.setArgv(progName +: scalaCliArgs)
      case _                   =>
    }

    val processedArgs =
      if (scalaCliArgs.lengthCompare(1) > 0 && isShebangFile(scalaCliArgs(0)))
        Array(scalaCliArgs(0), "--") ++ scalaCliArgs.tail
      else
        scalaCliArgs
    super.main(processedArgs)
  }
}
