package scala.cli.commands

import caseapp.core.help.RuntimeCommandsHelp
import caseapp.core.{Error, RemainingArgs}
import scala.cli.commands.util.SharedOptionsUtil.*

import scala.build.internal.Constants
import scala.cli.{CurrentParams, ScalaCliHelp}

class Default(
  actualHelp: => RuntimeCommandsHelp,
  isSipScala: Boolean
) extends ScalaCommand[DefaultOptions] {

  private def defaultHelp: String     = actualHelp.help(ScalaCliHelp.helpFormat)
  private def defaultFullHelp: String = actualHelp.help(ScalaCliHelp.helpFormat, showHidden = true)

  override protected def commandLength = 0

  override def group                                                         = "Main"
  override def sharedOptions(options: DefaultOptions): Option[SharedOptions] = Some(options.shared)
  private[cli] var rawArgs                                                   = Array.empty[String]
  override def helpAsked(progName: String, maybeOptions: Either[Error, DefaultOptions]): Nothing = {
    println(defaultHelp)
    sys.exit(0)
  }
  override def fullHelpAsked(progName: String): Nothing = {
    println(defaultFullHelp)
    sys.exit(0)
  }

  def run(options: DefaultOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.shared.logging.verbosity
    if options.version then println(Version.versionInfo(isSipScala))
    else
      {
        val shouldDefaultToRun =
          args.remaining.nonEmpty || options.shared.snippet.executeScript.nonEmpty ||
          options.shared.snippet.executeScala.nonEmpty || options.shared.snippet.executeJava.nonEmpty
        if shouldDefaultToRun then RunOptions.parser else ReplOptions.parser
      }.parse(rawArgs) match
        case Left(e)                              => error(e)
        case Right((replOptions: ReplOptions, _)) => Repl.run(replOptions, args)
        case Right((runOptions: RunOptions, _))   => Run.run(runOptions, args)
  }
}
