package scala.cli.commands

import caseapp.Name
import caseapp.core.app.Command
import caseapp.core.parser.Parser
import caseapp.core.util.Formatter
import caseapp.core.{Arg, Error}

import scala.build.Logger
import scala.build.input.ScalaCliInvokeData
import scala.build.internal.util.WarningMessages
import scala.build.internals.FeatureType
import scala.cli.ScalaCli
import scala.cli.util.ArgHelpers.*

object RestrictedCommandsParser {
  def apply[T](
    parser: Parser[T],
    logger: Logger,
    shouldSuppressExperimentalWarnings: Boolean
  )(using ScalaCliInvokeData): Parser[T] =
    new Parser[T] {

      type D = parser.D

      def args: Seq[caseapp.core.Arg] = parser.args.filter(_.isSupported)

      def get(
        d: D,
        nameFormatter: caseapp.core.util.Formatter[caseapp.Name]
      ): Either[caseapp.core.Error, T] =
        parser.get(d, nameFormatter)

      def init: D = parser.init

      def withDefaultOrigin(origin: String): caseapp.core.parser.Parser[T] =
        RestrictedCommandsParser(
          parser.withDefaultOrigin(origin),
          logger,
          shouldSuppressExperimentalWarnings
        )

      override def step(
        args: List[String],
        index: Int,
        d: D,
        nameFormatter: Formatter[Name]
      ): Either[(Error, Arg, List[String]), Option[(D, Arg, List[String])]] =
        (parser.step(args, index, d, nameFormatter), args) match {
          case (Right(Some(_, arg: Arg, _)), passedOption :: _) if !arg.isSupported =>
            Left((
              Error.UnrecognizedArgument(arg.powerOptionUsedInSip(passedOption)),
              arg,
              Nil
            ))
          case (r @ Right(Some(_, arg: Arg, _)), passedOption :: _)
              if arg.isExperimental && !shouldSuppressExperimentalWarnings =>
            logger.experimentalWarning(passedOption, FeatureType.Option)
            r
          case (other, _) =>
            other
        }
    }
}
