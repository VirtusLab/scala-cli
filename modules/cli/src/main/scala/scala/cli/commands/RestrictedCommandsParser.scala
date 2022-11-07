package scala.cli.commands

import caseapp.Name
import caseapp.core.app.Command
import caseapp.core.parser.Parser
import caseapp.core.util.Formatter
import caseapp.core.{Arg, Error}

final case class RestrictedCommandsParser[T](underlying: Parser[T]) extends Parser[T] {

  type D = underlying.D

  private def isArgSupported(a: Arg): Boolean =
    scala.cli.ScalaCli.allowRestrictedFeatures ||
    !RestrictedCommandsParser.isExperimentalOrRestricted(a)

  def args: Seq[Arg] = underlying.args.filter(isArgSupported)

  def get(
    d: D,
    nameFormatter: Formatter[Name]
  ): Either[Error, T] =
    underlying.get(d, nameFormatter)

  def init: D = underlying.init

  def withDefaultOrigin(origin: String): Parser[T] =
    copy(underlying = underlying.withDefaultOrigin(origin))

  override def step(
    args: List[String],
    index: Int,
    d: D,
    nameFormatter: Formatter[Name]
  ): Either[(Error, Arg, List[String]), Option[(D, Arg, List[String])]] =
    underlying.step(args, index, d, nameFormatter) match {
      case Right(Some(_, arg, _)) if !isArgSupported(arg) =>
        Left((
          Error.UnrecognizedArgument(
            s"`${args(index)}` option is not supported in `scala` command.\n  Please run it with `scala-cli` command or with `--power` flag."
          ),
          arg,
          Nil
        ))
      case other =>
        other
    }
}

object RestrictedCommandsParser {
  def isExperimentalOrRestricted(a: Arg) =
    a.helpMessage.exists(m =>
      m.message.contains("[experimental]") || m.message.contains("[restricted]")
    )
}
