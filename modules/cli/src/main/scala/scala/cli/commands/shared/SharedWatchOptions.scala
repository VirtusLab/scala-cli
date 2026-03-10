package scala.cli.commands.shared

import caseapp.*

import scala.build.options.{BuildOptions, WatchOptions}
import scala.cli.commands.tags

// format: off
final case class SharedWatchOptions(

  @Group(HelpGroup.Watch.toString)
  @HelpMessage("Run the application in the background, automatically wake the thread and re-run if sources have been changed")
  @Tag(tags.should)
  @Tag(tags.inShortHelp)
  @Name("w")
    watch: Boolean = false,
  @Group(HelpGroup.Watch.toString)
  @HelpMessage("Run the application in the background, automatically kill the process and restart if sources have been changed")
  @Tag(tags.should)
  @Tag(tags.inShortHelp)
  @Name("revolver")
    restart: Boolean = false,
  @Group(HelpGroup.Watch.toString)
  @HelpMessage("Watch additional paths for changes (used together with --watch or --restart)")
  @Tag(tags.experimental)
  @Name("watchingPath")
    watching: List[String] = Nil
) { // format: on

  lazy val watchMode: Boolean = watch || restart

  def buildOptions(cwd: os.Path = os.pwd): BuildOptions =
    BuildOptions(
      watchOptions = WatchOptions(
        extraWatchPaths = watching.map(os.Path(_, cwd))
      )
    )
}

object SharedWatchOptions {
  implicit lazy val parser: Parser[SharedWatchOptions] = Parser.derive
  implicit lazy val help: Help[SharedWatchOptions]     = Help.derive
}
