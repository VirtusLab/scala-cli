package scala.cli.commands

import caseapp.core.help.RuntimeCommandsHelp
import caseapp.core.{Error, RemainingArgs}

import scala.build.internal.Constants
import scala.cli.{CurrentParams, ScalaCliHelp}

class Default(
  actualHelp: => RuntimeCommandsHelp,
  isSipScala: Boolean
) extends ScalaCommand[DefaultOptions] {

  private def defaultHelp: String     = actualHelp.help(ScalaCliHelp.helpFormat)
  private def defaultFullHelp: String = actualHelp.help(ScalaCliHelp.helpFormat, showHidden = true)

  override protected def commandLength = 0

  override def group = "Main"
  override def sharedOptions(options: DefaultOptions) =
    Some[scala.cli.commands.SharedOptions](options.runOptions.shared)
  private[cli] var anyArgs = false
  override def helpAsked(progName: String, maybeOptions: Either[Error, DefaultOptions]): Nothing = {
    println(defaultHelp)
    sys.exit(0)
  }
  override def fullHelpAsked(progName: String): Nothing = {
    println(defaultFullHelp)
    sys.exit(0)
  }
  def run(options: DefaultOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.runOptions.shared.logging.verbosity
    if (options.version)
      println(Version.versionInfo(isSipScala))
    else if (anyArgs)
      Run.run(
        options.runOptions,
        args
      )
    else
      helpAsked(finalHelp.progName, Right(options))
  }
}
