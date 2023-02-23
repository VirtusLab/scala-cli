package scala.cli

import caseapp.core.app.CommandsEntryPoint
import caseapp.core.help.{Help, HelpFormat, RuntimeCommandsHelp}

import java.nio.file.InvalidPathException

import scala.cli.commands.*
import scala.cli.commands.shared.ScalaCliHelp

class ScalaCliCommands(
  val progName: String,
  baseRunnerName: String,
  fullRunnerName: String,
  isSipScala: Boolean
) extends CommandsEntryPoint {

  lazy val actualDefaultCommand = new default.Default(help, isSipScala)

  // for debugging purposes - allows to run the scala-cli-signing binary from the Scala CLI JVM launcher
  private lazy val pgpUseBinaryCommands =
    java.lang.Boolean.getBoolean("scala-cli.pgp.binary-commands")
  private def pgpCommands       = new pgp.PgpCommands
  private def pgpBinaryCommands = new pgp.PgpCommandsSubst

  private def allCommands = Seq[ScalaCommand[_]](
    addpath.AddPath,
    bloop.Bloop,
    bloop.BloopExit,
    bloop.BloopOutput,
    bloop.BloopStart,
    bsp.Bsp,
    clean.Clean,
    compile.Compile,
    config.Config,
    default.DefaultFile,
    dependencyupdate.DependencyUpdate,
    directories.Directories,
    doc.Doc,
    export0.Export,
    fmt.Fmt,
    new HelpCmd(help),
    installcompletions.InstallCompletions,
    installhome.InstallHome,
    repl.Repl,
    package0.Package,
    pgp.PgpPull,
    pgp.PgpPush,
    publish.Publish,
    publish.PublishLocal,
    publish.PublishSetup,
    run.Run,
    github.SecretCreate,
    github.SecretList,
    setupide.SetupIde,
    shebang.Shebang,
    test.Test,
    uninstall.Uninstall,
    uninstallcompletions.UninstallCompletions,
    update.Update,
    version.Version
  ) ++ (if (pgpUseBinaryCommands) Nil else pgpCommands.allScalaCommands.toSeq) ++
    (if (pgpUseBinaryCommands) pgpBinaryCommands.allScalaCommands.toSeq else Nil)

  def commands =
    allCommands ++
      (if (pgpUseBinaryCommands) Nil else pgpCommands.allExternalCommands.toSeq) ++
      (if (pgpUseBinaryCommands) pgpBinaryCommands.allExternalCommands.toSeq else Nil)

  override def description: String = {
    val coreFeaturesString =
      if ScalaCli.allowRestrictedFeatures then "compile, run, test and package"
      else "compile, run and test"
    s"$fullRunnerName is a command-line tool to interact with the Scala language. It lets you $coreFeaturesString your Scala code."
  }

  override def summaryDesc =
    s"""|See '$baseRunnerName <command> --help' to read about a specific subcommand. To see full help run '$baseRunnerName <command> --help-full'.
        |
        |To use launcher options, specify them before any other argument.
        |For example, to run another $fullRunnerName version, specify it with the '--cli-version' launcher option:
        |  ${Console.BOLD}$baseRunnerName --cli-version <version> args${Console.RESET}""".stripMargin
  final override def defaultCommand = Some(actualDefaultCommand)

  // FIXME Report this in case-app default NameFormatter
  override lazy val help: RuntimeCommandsHelp = {
    val parent = super.help
    parent.copy(defaultHelp = Help[Unit]())
  }

  override def enableCompleteCommand    = true
  override def enableCompletionsCommand = true

  override def helpFormat: HelpFormat = ScalaCliHelp.helpFormat

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
    actualDefaultCommand.rawArgs = args

    commands.foreach {
      case c: NeedsArgvCommand => c.setArgv(progName +: args)
      case _                   =>
    }
    actualDefaultCommand.setArgv(progName +: args)

    val processedArgs =
      if (args.lengthCompare(1) > 0 && isShebangFile(args(0)))
        Array(args(0), "--") ++ args.tail
      else
        args
    super.main(processedArgs)
  }
}
