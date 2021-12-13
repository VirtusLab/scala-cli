package scala.cli.commands

import caseapp._
import caseapp.core.{Error, RemainingArgs}

import scala.build.internal.Constants
import scala.cli.CurrentParams

class DefaultBase(
  defaultHelp: => String,
  defaultFullHelp: => String
) extends ScalaCommand[DefaultOptions] {

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
      println(Constants.version)
    else if (anyArgs)
      Run.run(
        options.runOptions,
        args
      )
    else
      helpAsked(finalHelp.progName, Right(options))
  }
}
