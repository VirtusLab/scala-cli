package scala.cli.commands

import caseapp._
import caseapp.core.Error

import scala.build.internal
//import scala.cli.commands.SharedOptions

case class DefaultOptions(
  @Recurse
  runOptions: RunOptions = RunOptions(),
  version: Option[Boolean] = None
)

object DefaultOptions {
  implicit lazy val parser: Parser[DefaultOptions] = Parser.derive
  implicit lazy val help: Help[DefaultOptions]     = Help.derive
//  implicit lazy val jsonCodec: ReadWriter[DefaultOptions] = macroRW
}

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
  def run(options: DefaultOptions, args: RemainingArgs): Unit =
    if (options.version.isDefined) {
      println(internal.Constants.version)
      sys.exit(0)
    }
    else if (anyArgs)
      Run.run(
        options.runOptions,
        args
      )
    else
      helpAsked(finalHelp.progName, Right(options))
}
