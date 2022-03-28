package scala.cli.signing.util

import caseapp.core.argparser.ArgParser
import caseapp.core.argparser.SimpleArgParser

import scala.cli.signing.shared.PasswordOption

abstract class LowPriorityArgParsers {

  implicit lazy val argParser: ArgParser[PasswordOption] =
    SimpleArgParser.from("password") { str =>
      PasswordOption.parse(str)
        .left.map(caseapp.core.Error.Other(_))
    }

}

object ArgParsers extends LowPriorityArgParsers {

  implicit lazy val optionArgParser: ArgParser[Option[PasswordOption]] =
    SimpleArgParser.from("password") { str =>
      if (str.trim.isEmpty) Right(None)
      else argParser(None, -1, -1, str).map(Some(_))
    }
}
