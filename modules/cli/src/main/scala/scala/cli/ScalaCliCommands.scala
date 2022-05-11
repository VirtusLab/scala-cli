package scala.cli

import caseapp.core.app.CommandsEntryPoint
import caseapp.core.help.{Help, RuntimeCommandsHelp}

import java.nio.file.InvalidPathException

import scala.cli.commands._
import scala.cli.commands.bloop.BloopOutput
import scala.cli.commands.github.{SecretCreate, SecretList}
import scala.cli.commands.pgp.{PgpCommands, PgpCommandsSubst}
import scala.cli.commands.publish.Publish

class ScalaCliCommands(
  val progName: String,
  isSipScala: Boolean
) extends CommandsEntryPoint {

  lazy val actualDefaultCommand = new Default(help)

  // for debugging purposes - allows to run the scala-cli-signing binary from the Scala CLI JVM launcher
  private lazy val pgpUseBinaryCommands =
    java.lang.Boolean.getBoolean("scala-cli.pgp.binary-commands")
  private def pgpCommands       = new PgpCommands
  private def pgpBinaryCommands = new PgpCommandsSubst

  private def allCommands = Seq[ScalaCommand[_]](
    new About(isSipScala = isSipScala),
    AddPath,
    BloopExit,
    BloopOutput,
    BloopStart,
    Bsp,
    Clean,
    Compile,
    Directories,
    Doc,
    Doctor,
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
    SecretCreate,
    SecretList,
    SetupIde,
    Shebang,
    Test,
    Update,
    Version
  ) ++ (if (pgpUseBinaryCommands) Nil else pgpCommands.allScalaCommands.toSeq) ++
    (if (pgpUseBinaryCommands) pgpBinaryCommands.allScalaCommands.toSeq else Nil)

  def commands =
    allCommands.filter(c => !isSipScala || c.inSipScala) ++
      (if (pgpUseBinaryCommands) Nil else pgpCommands.allExternalCommands.toSeq) ++
      (if (pgpUseBinaryCommands) pgpBinaryCommands.allExternalCommands.toSeq else Nil)

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
