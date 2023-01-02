package scala.cli.commands.shared

import caseapp.core.help.HelpFormat

import scala.util.{Properties, Try}

object ScalaCliHelp {
  val helpFormat: HelpFormat = HelpFormat.default()
    .copy(
      sortedGroups = Some(
        Seq(
          "Help",
          "Scala",
          "Java",
          "Repl",
          "Package",
          "Metabrowse server",
          "Logging",
          "Runner",
          "Launcher"
        )
      ),
      sortedCommandGroups = Some(
        Seq(
          "Main",
          "Miscellaneous",
          ""
        )
      ),
      hiddenGroups = Some(
        Seq(
          "Scala.js",
          "Scala Native"
        )
      ),
      terminalWidthOpt =
        if (Properties.isWin)
          if (coursier.paths.Util.useJni())
            Try(coursier.jniutils.WindowsAnsiTerminal.terminalSize()).toOption.map(
              _.getWidth
            ).orElse {
              val fallback = 120
              if (java.lang.Boolean.getBoolean("scala.cli.windows-terminal.verbose"))
                System.err.println(s"Could not get terminal width, falling back to $fallback")
              Some(fallback)
            }
          else None
        else
          // That's how Ammonite gets the terminal width, but I'd rather not spawn a sub-process upfront in Scala CLI…
          //   val pathedTput = if (os.isFile(os.Path("/usr/bin/tput"))) "/usr/bin/tput" else "tput"
          //   val width = os.proc("sh", "-c", s"$pathedTput cols 2>/dev/tty").call(stderr = os.Pipe).out.trim().toInt
          //   Some(width)
          // Ideally, we should do an ioctl, like jansi does here:
          //   https://github.com/fusesource/jansi/blob/09722b7cccc8a99f14ac1656db3072dbeef34478/src/main/java/org/fusesource/jansi/AnsiConsole.java#L344
          // This requires writing our own minimal JNI library, that publishes '.a' files too for static linking in the executable of Scala CLI.
          None
    )
}
