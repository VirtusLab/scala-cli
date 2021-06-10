package scala.cli.commands

import java.nio.charset.Charset
import java.util.Arrays

import caseapp._
import caseapp.core.complete.{Bash, Zsh}
import coursier.cache.shaded.dirs.dev.dirs.ProjectDirectories

import scala.cli.internal.ProfileFileUpdater

object InstallCompletions extends Command[InstallCompletionsOptions] {
  override def names = List(
    List("install", "completions")
  )
  private lazy val home = os.Path(sys.props("user.home"), os.pwd)
  def run(options: InstallCompletionsOptions, args: RemainingArgs): Unit = {

    lazy val completionsDir =
      options.directory
        .map(os.Path(_, os.pwd))
        .getOrElse {
          // TODO When bumping the coursier version, pass the 4th argument too (more stable Windows env var stuff)
          val projDirs = ProjectDirectories.from(null, null, "ScalaCli")
          os.Path(projDirs.dataLocalDir, os.pwd) / "completions"
        }

    val logger = options.logging.logger

    val (rcScript, defaultRcFile) = options.format match {
      case Bash.id | "bash" =>
        val script = Bash.script(options.name)
        val defaultRcFile = home / ".bashrc"
        (script, defaultRcFile)
      case Zsh.id | "zsh" =>
        val completionScript = Zsh.script(options.name)
        val zDotDir = Option(System.getenv("ZDOTDIR"))
          .map(os.Path(_, os.pwd))
          .getOrElse(home)
        val defaultRcFile = zDotDir / ".zshrc"
        val dir = completionsDir / "zsh"
        val completionScriptDest = dir / "_scala"
        val content = completionScript.getBytes(Charset.defaultCharset())
        if (!os.exists(completionScriptDest) || !Arrays.equals(os.read.bytes(completionScriptDest), content)) {
          logger.log(s"Writing $completionScriptDest")
          os.write.over(completionScriptDest, content, createFolders = true)
        }
        val script = Seq(
          s"""fpath=("$dir" $$fpath)""",
          "compinit"
        ).map(_ + System.lineSeparator()).mkString
        (script, defaultRcFile)
      case _ =>
        System.err.println(s"Unrecognized shell: ${options.format}")
        sys.exit(1)
    }

    val rcFile = options.rcFile
      .map(os.Path(_, os.pwd))
      .getOrElse(defaultRcFile)

    val banner = options.banner.replace("{NAME}", options.name)

    val updated = ProfileFileUpdater.addToProfileFile(rcFile.toNIO, banner, rcScript, Charset.defaultCharset())

    if (options.logging.verbosity >= 0) {
      if (updated) {
        System.err.println(s"Updated $rcFile")
        System.err.println(
          s"It is recommended to reload your shell, or source $rcFile in the " +
            "current session, for its changes to be taken into account."
        )
      } else
        System.err.println(s"$rcFile already up-to-date")
    }
  }
}
