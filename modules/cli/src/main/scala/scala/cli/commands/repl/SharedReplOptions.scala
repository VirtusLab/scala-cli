package scala.cli.commands.repl

import caseapp.*
import caseapp.core.help.Help

import scala.cli.commands.shared.{CrossOptions, HelpGroup, SharedJavaOptions, SharedWatchOptions}
import scala.cli.commands.tags

// format: off
final case class SharedReplOptions(
  @Recurse
    sharedJava: SharedJavaOptions = SharedJavaOptions(),
  @Recurse
    watch: SharedWatchOptions = SharedWatchOptions(),
  @Recurse
    compileCross: CrossOptions = CrossOptions(),

  @Group(HelpGroup.Repl.toString)
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
  @HelpMessage("Use JShell as the REPL (default for pure-Java projects). Requires JDK >= 9.")
  @Name("jsh")
    jshell: Option[Boolean] = None,

  @Group(HelpGroup.Repl.toString)
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
  @ValueDescription("path")
  @HelpMessage("Read the REPL init script (--repl-init-script) from a file. Mutually exclusive with --repl-init-script.")
    replInitScriptFile: Option[String] = None,

  @Group(HelpGroup.Repl.toString)
  @Hidden
  @Tag(tags.implementation)
  @HelpMessage("Don't actually run the REPL, just fetch it")
    replDryRun: Boolean = false
)
// format: on

object SharedReplOptions {
  implicit lazy val parser: Parser[SharedReplOptions] = Parser.derive
  implicit lazy val help: Help[SharedReplOptions]     = Help.derive
}
