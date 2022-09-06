package scala.cli.util

import caseapp.core.argparser.ArgParser
import caseapp.core.argparser.SimpleArgParser
import scala.cli.commands.publish.MaybeConfigPasswordOption
import scala.cli.signing.shared.PasswordOption

abstract class LowPriorityArgParsers {

  /** case-app [[ArgParser]] for [[MaybeConfigPasswordOption]]
    *
    * Given a lower priority than the one for `Option[MaybeConfigPasswordOption]`, as the latter
    * falls back to `None` when given an empty string (like in `--password ""`), while letting it be
    * automatically derived from this one (with the former parser and the generic [[ArgParser]] for
    * `Option[T]` from case-app) would fail on such empty input.
    */
  implicit lazy val maybeConfigPasswordOptionArgParser: ArgParser[MaybeConfigPasswordOption] =
    SimpleArgParser.from("password") { str =>
      MaybeConfigPasswordOption.parse(str)
        .left.map(caseapp.core.Error.Other(_))
    }

}

object ArgParsers extends LowPriorityArgParsers {

  /** case-app [[ArgParser]] for `Option[MaybeConfigPasswordOption]`
    *
    * Unlike a parser automatically derived through case-app [[ArgParser]] for `Option[T]`, the
    * parser here accepts empty input (like in `--password ""`), and returns a `None` value in that
    * case.
    */
  implicit lazy val optionMaybeConfigPasswordOptionArgParser
    : ArgParser[Option[MaybeConfigPasswordOption]] =
    SimpleArgParser.from("password") { str =>
      if (str.trim.isEmpty) Right(None)
      else maybeConfigPasswordOptionArgParser(None, -1, -1, str).map(Some(_))
    }
}
