package scala.cli.commands

import caseapp._
import caseapp.core.complete.{Bash, Zsh}

import java.io.File
import java.nio.charset.Charset
import java.util.Arrays

import scala.cli.CurrentParams
import scala.cli.internal.{Argv0, ProfileFileUpdater}

object InstallCompletions extends ScalaCommand[InstallCompletionsOptions] {
  override def names = List(
    List("install", "completions"),
    List("install-completions")
  )
  private lazy val home = os.Path(sys.props("user.home"), os.pwd)
  def run(options: InstallCompletionsOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.logging.verbosity

    lazy val completionsDir =
      options.output
        .map(os.Path(_, os.pwd))
        .getOrElse(options.directories.directories.completionsDir)

    val logger = options.logging.logger

    val name = options.name.getOrElse {
      val baseName = (new Argv0).get("scala-cli")
      val idx      = baseName.lastIndexOf(File.separator)
      if (idx < 0) baseName
      else baseName.drop(idx + 1)
    }

    val format = options.format.map(_.trim).filter(_.nonEmpty)
      .orElse {
        Option(System.getenv("SHELL")).map(_.split(File.separator).last).map {
          case "bash" => Bash.id
          case "zsh"  => Zsh.id
          case other  => other
        }
      }
      .getOrElse {
        System.err.println(
          "Cannot determine current shell, pass the shell you use with --shell, like"
        )
        System.err.println(s"  $name install completions --shell zsh")
        System.err.println(s"  $name install completions --shell bash")
        sys.exit(1)
      }

    val (rcScript, defaultRcFile) = format match {
      case Bash.id | "bash" =>
        val script        = Bash.script(name)
        val defaultRcFile = home / ".bashrc"
        (script, defaultRcFile)
      case Zsh.id | "zsh" =>
        val completionScript = Zsh.script(name)
        val zDotDir = Option(System.getenv("ZDOTDIR"))
          .map(os.Path(_, os.pwd))
          .getOrElse(home)
        val defaultRcFile        = zDotDir / ".zshrc"
        val dir                  = completionsDir / "zsh"
        val completionScriptDest = dir / s"_$name"
        val content              = completionScript.getBytes(Charset.defaultCharset())
        val needsWrite = !os.exists(completionScriptDest) ||
          !Arrays.equals(os.read.bytes(completionScriptDest), content)
        if (needsWrite) {
          logger.log(s"Writing $completionScriptDest")
          os.write.over(completionScriptDest, content, createFolders = true)
        }
        val script = Seq(
          s"""fpath=("$dir" $$fpath)""",
          "compinit"
        ).map(_ + System.lineSeparator()).mkString
        (script, defaultRcFile)
      case _ =>
        System.err.println(s"Unrecognized or unsupported shell: $format")
        sys.exit(1)
    }

    if (options.env)
      println(rcScript)
    else {

      val rcFile = options.rcFile
        .map(os.Path(_, os.pwd))
        .getOrElse(defaultRcFile)

      val banner = options.banner.replace("{NAME}", name)

      val updated = ProfileFileUpdater.addToProfileFile(
        rcFile.toNIO,
        banner,
        rcScript,
        Charset.defaultCharset()
      )

      if (options.logging.verbosity >= 0)
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
}
