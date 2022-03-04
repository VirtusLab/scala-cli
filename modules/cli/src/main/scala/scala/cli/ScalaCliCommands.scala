package scala.cli

import caseapp.core.app.CommandsEntryPoint
import caseapp.core.help.{Help, RuntimeCommandsHelp}

import java.nio.file.InvalidPathException

import scala.cli.commands._

class ScalaCliCommands(
  val progName: String,
  isSipScala: Boolean
) extends CommandsEntryPoint {

  val appName =
    if (isSipScala) "Scala command"
    else "Scala CLI"

  lazy val actualDefaultCommand = new Default(help)

  private def allCommands = Seq[ScalaCommand[_]](
    new About(appName),
    AddPath,
    BloopExit,
    BloopStart,
    Bsp,
    Clean,
    Compile,
    Directories,
    Export,
    Fmt,
    new HelpCmd(help),
    InstallCompletions,
    InstallHome,
    Metabrowse,
    Repl,
    Package,
    Publish,
    Run,
    SetupIde,
    Shebang,
    Test,
    Update,
    Version
  )

  def commands = allCommands.filter(c => !isSipScala || c.inSipScala)

  override def description =
    "Scala CLI is a command-line tool to interact with the Scala language. It lets you compile, run, test, and package your Scala code."
  override def summaryDesc =
    """|See 'scala-cli <command> --help' to read about a specific subcommand. To see full help run 'scala-cli <command> --help-full'.
       |To run another Scala CLI version, specify it with '--cli-version' before any other argument, like 'scala-cli --cli-version <version> args'.""".stripMargin
  final override def defaultCommand = Some(actualDefaultCommand)

  // FIXME Report this in case-app default NameFormatter
  override lazy val help: RuntimeCommandsHelp = {
    val parent = super.help
    parent.withDefaultHelp(Help[Unit]())
  }

  override def enableCompleteCommand    = true
  override def enableCompletionsCommand = true

  override def helpFormat = ScalaCliHelp.helpFormat

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

  override def main(args: Array[String]): Unit = {

    // quick hack, until the raw args are kept in caseapp.RemainingArgs by case-app
    actualDefaultCommand.anyArgs = args.nonEmpty

    commands.foreach {
      case c: NeedsArgvCommand => c.setArgv(progName +: args)
      case _                   =>
    }

    val processedArgs =
      if (args.lengthCompare(1) > 0 && isShebangFile(args(0)))
        Array(args(0), "--") ++ args.tail
      else
        args
    super.main(processedArgs)
  }
}
