package scala.cli.internal

import caseapp.core.argparser.{ArgParser, SimpleArgParser}
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

import scala.cli.signing.shared.{PasswordOption, Secret}

abstract class LowPriorityPasswordOptionParsers {

  private lazy val commandCodec: JsonValueCodec[List[String]] =
    JsonCodecMaker.make

  implicit lazy val argParser: ArgParser[PasswordOption] =
    SimpleArgParser.from("password") { str =>
      if (str.startsWith("value:"))
        Right(PasswordOption.Value(Secret(str.stripPrefix("value:"))))
      else if (str.startsWith("command:["))
        try {
          val command = readFromString(str.stripPrefix("command:"))(commandCodec)
          Right(PasswordOption.Command(command))
        }
        catch {
          case e: JsonReaderException =>
            Left(caseapp.core.Error.Other(s"Error decoding password command: ${e.getMessage}"))
        }
      else if (str.startsWith("command:")) {
        val command = str.stripPrefix("command:").split("\\s+").toSeq
        Right(PasswordOption.Command(command))
      }
      else
        Left(caseapp.core.Error.Other("Malformed password value (expected \"value:...\")"))
    }

}

object PasswordOptionParsers extends LowPriorityPasswordOptionParsers {

  implicit lazy val optionArgParser: ArgParser[Option[PasswordOption]] =
    SimpleArgParser.from("password") { str =>
      if (str.trim.isEmpty) Right(None)
      else argParser(None, -1, -1, str).map(Some(_))
    }
}
