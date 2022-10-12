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
    a.tags.exists(_.name == tags.restricted) || a.tags.exists(_.name == tags.experimental)

  def level(a: Arg) = a.tags.flatMap(t => tags.levelFor(t.name)).headOption.getOrElse {
    SpecificationLevel.IMPLEMENTATION
  }

  def apply[T, TD](parser: Parser.Aux[T, TD]): Parser.Aux[T, TD] = new Parser[T] {

    type D = TD

    private def isArgSupported(a: Arg): Boolean =
      scala.cli.ScalaCli.allowRestrictedFeatures || !isExperimentalOrRestricted(a)

    def args: Seq[caseapp.core.Arg] = parser.args.filter(isArgSupported)

    def get(
      d: D,
      nameFormatter: caseapp.core.util.Formatter[caseapp.Name]
    ): Either[caseapp.core.Error, T] =
      parser.get(d, nameFormatter)

    def init: D = parser.init

    def withDefaultOrigin(origin: String): caseapp.core.parser.Parser.Aux[T, D] =
      RestrictedCommandsParser(parser.withDefaultOrigin(origin))

    override def step(
      args: List[String],
      index: Int,
      d: D,
      nameFormatter: Formatter[Name]
    ): Either[(Error, Arg, List[String]), Option[(D, Arg, List[String])]] =
      parser.step(args, index, d, nameFormatter) match {
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
}
