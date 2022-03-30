package scala.cli.signing.shared

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

import scala.io.Codec

sealed abstract class PasswordOption extends Product with Serializable {
  def get(): Secret[String]
  def asString: Secret[String]
}

abstract class LowPriorityPasswordOption {

  protected lazy val commandCodec: JsonValueCodec[List[String]] =
    JsonCodecMaker.make

  def parse(str: String): Either[String, PasswordOption] =
    if (str.startsWith("value:"))
      Right(PasswordOption.Value(Secret(str.stripPrefix("value:"))))
    else if (str.startsWith("command:["))
      try {
        val command = readFromString(str.stripPrefix("command:"))(commandCodec)
        Right(PasswordOption.Command(command))
      }
      catch {
        case e: JsonReaderException =>
          Left(s"Error decoding password command: ${e.getMessage}")
      }
    else if (str.startsWith("command:")) {
      val command = str.stripPrefix("command:").split("\\s+").toSeq
      Right(PasswordOption.Command(command))
    }
    else
      Left("Malformed password value (expected \"value:...\")")

}

object PasswordOption extends LowPriorityPasswordOption {

  final case class Value(value: Secret[String]) extends PasswordOption {
    def get(): Secret[String]    = value
    def asString: Secret[String] = get().map(v => s"value:$v")
  }
  final case class Command(command: Seq[String]) extends PasswordOption {
    def get(): Secret[String] = {
      // should we add a timeout?
      val res = os.proc(command).call(stdin = os.Inherit)
      Secret(res.out.text(Codec.default)) // should we trim that?
    }
    def asString: Secret[String] = {
      val json = writeToString(command.toList)(commandCodec)
      Secret(s"command:$json")
    }
  }
}
