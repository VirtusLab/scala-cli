package scala.cli.commands

import caseapp.Name
import caseapp.core.app.Command
import caseapp.core.parser.Parser
import caseapp.core.util.Formatter
import caseapp.core.{Arg, Error}

import scala.cli.util.ArgHelpers.*

object RestrictedCommandsParser {
  def apply[T](parser: Parser[T]): Parser[T] = new Parser[T] {

    type D = parser.D

    def args: Seq[caseapp.core.Arg] = parser.args.filter(_.isSupported)

    def get(
      d: D,
      nameFormatter: caseapp.core.util.Formatter[caseapp.Name]
    ): Either[caseapp.core.Error, T] =
      parser.get(d, nameFormatter)

    def init: D = parser.init

    def withDefaultOrigin(origin: String): caseapp.core.parser.Parser[T] =
      RestrictedCommandsParser(parser.withDefaultOrigin(origin))

    override def step(
      args: List[String],
      index: Int,
      d: D,
      nameFormatter: Formatter[Name]
    ): Either[(Error, Arg, List[String]), Option[(D, Arg, List[String])]] =
      (parser.step(args, index, d, nameFormatter), args) match {
        case (Right(Some(_, arg, _)), passedOption :: _) if !arg.isSupported =>
          Left((
            Error.UnrecognizedArgument(
              s"""`$passedOption` option is not supported in `scala` command.
                 |Please run it with `scala-cli` command or with `--power` flag or turn on this flag globally running command `config power true`.""".stripMargin
            ),
            arg,
            Nil
          ))
        case (other, _) =>
          other
      }
  }
}
