package scala.cli.commands.installcompletions

import caseapp.*
import caseapp.core.complete.{Bash, Fish, Zsh}
import caseapp.core.help.HelpFormat

import java.nio.charset.Charset
import java.nio.file.Paths
import java.util

import scala.build.internals.EnvVar
import scala.build.{Directories, Logger}
import scala.cli.ScalaCli
import scala.cli.commands.shared.HelpGroup
import scala.cli.commands.{ScalaCommand, SpecificationLevel}
import scala.cli.internal.ProfileFileUpdater
import scala.cli.util.ArgHelpers.*
object InstallCompletions extends ScalaCommand[InstallCompletionsOptions] {
  override def names = List(
    List("install", "completions"),
    List("install-completions")
  )

  override def helpFormat: HelpFormat =
    super.helpFormat.withPrimaryGroup(HelpGroup.Install)

  override def scalaSpecificationLevel: SpecificationLevel = SpecificationLevel.IMPLEMENTATION

  override def runCommand(
    options: InstallCompletionsOptions,
    args: RemainingArgs,
    logger: Logger
  ): Unit = {
    val interactive = options.global.logging.verbosityOptions.interactiveInstance()
    lazy val completionsDir =
      options.output
        .map(os.Path(_, os.pwd))
        .getOrElse(Directories.directories.completionsDir)

    val name = getName(options.name)
    val format = getFormat(options.format).getOrElse {
      val msg = "Cannot determine current shell. Which would you like to use?"
      interactive.chooseOne(msg, List("zsh", "bash")).getOrElse {
        System.err.println(
          "Cannot determine current shell, pass the shell you use with --shell, like"
        )
        System.err.println(s"$name install completions --shell zsh")
        System.err.println(s"$name install completions --shell bash")
        System.err.println(s"$name install completions --shell fish")
        sys.exit(1)
      }
    }

    val (rcScript, defaultRcFile) = format match {
      case Bash.id | "bash" =>
        val script        = Bash.script(name)
        val defaultRcFile = os.home / ".bashrc"
        (script, defaultRcFile)
      case Zsh.id | "zsh" =>
        val completionScript = Zsh.script(name)
        val zDotDir = EnvVar.Misc.zDotDir.valueOpt
          .map(os.Path(_, os.pwd))
          .getOrElse(os.home)
        val defaultRcFile        = zDotDir / ".zshrc"
        val dir                  = completionsDir / "zsh"
        val completionScriptDest = dir / s"_$name"
        val content              = completionScript.getBytes(Charset.defaultCharset())
        val needsWrite = !os.exists(completionScriptDest) ||
          !util.Arrays.equals(os.read.bytes(completionScriptDest), content)
        if (needsWrite) {
          logger.log(s"Writing $completionScriptDest")
          os.write.over(completionScriptDest, content, createFolders = true)
        }
        val script = Seq(
          s"""fpath=("$dir" $$fpath)""",
          "compinit"
        ).map(_ + System.lineSeparator()).mkString
        (script, defaultRcFile)
      case Fish.id | "fish" =>
        val script        = Fish.script(name)
        val defaultRcFile = os.home / ".config" / "fish" / "config.fish"
        (script, defaultRcFile)
      case _ =>
        System.err.println(s"Unrecognized or unsupported shell: $format")
        sys.exit(1)
    }

    if (options.env)
      println(rcScript)
    else {
      val rcFile = options.rcFile.map(os.Path(_, os.pwd)).getOrElse(defaultRcFile)
      val banner = options.banner.replace("{NAME}", name)
      val updated = ProfileFileUpdater.addToProfileFile(
        rcFile.toNIO,
        banner,
        rcScript,
        Charset.defaultCharset()
      )

      if (options.global.logging.verbosity >= 0)
        if (updated) {
          System.err.println(s"Updated $rcFile")
          System.err.println(
            s"It is recommended to reload your shell, or source $rcFile in the " +
              "current session, for its changes to be taken into account."
          )
        }
        else
          System.err.println(s"$rcFile already up-to-date")
    }
  }

  def getName(name: Option[String]): String =
    name.getOrElse {
      val progName = ScalaCli.progName
      Paths.get(progName).getFileName.toString
    }

  def getFormat(format: Option[String]): Option[String] =
    format.map(_.trim).filter(_.nonEmpty)
      .orElse {
        EnvVar.Misc.shell.valueOpt.map(_.split("[\\/]+").last).map {
          case "bash" => Bash.id
          case "zsh"  => Zsh.id
          case "fish" => Fish.id
          case other  => other
        }
      }
}
