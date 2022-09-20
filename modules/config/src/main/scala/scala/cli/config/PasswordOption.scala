package scala.cli.config

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

sealed abstract class PasswordOption extends Product with Serializable {
  def get(): Secret[String]
  def getBytes(): Secret[Array[Byte]] = get().map(_.getBytes(StandardCharsets.UTF_8))
  def asString: Secret[String]
}

abstract class LowPriorityPasswordOption {

  protected lazy val commandCodec: JsonValueCodec[List[String]] =
    JsonCodecMaker.make

  def parse(str: String): Either[String, PasswordOption] =
    if (str.startsWith("value:"))
      Right(PasswordOption.Value(Secret(str.stripPrefix("value:"))))
    else if (str.startsWith("file:"))
      Right(PasswordOption.File(Paths.get(str.stripPrefix("file:"))))
    else if (str.startsWith("env:"))
      Right(PasswordOption.Env(str.stripPrefix("env:")))
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
  final case class Env(name: String) extends PasswordOption {
    def get(): Secret[String] = {
      val value = Option(System.getenv(name)).getOrElse {
        sys.error(s"Error: environment variable $name not set")
      }
      Secret(value)
    }
    def asString: Secret[String] =
      Secret(s"env:$name")
  }
  final case class File(path: Path) extends PasswordOption {
    def get(): Secret[String] = {
      val value = Files.readString(path) // trim that?
      Secret(value)
    }
    override def getBytes(): Secret[Array[Byte]] = {
      val value = Files.readAllBytes(path)
      Secret(value)
    }
    def asString: Secret[String] =
      Secret(s"file:$path")
  }
  final case class Command(command: Seq[String]) extends PasswordOption {
    def get(): Secret[String] = {
      // should we add a timeout?

      import sys.process._
      val res = command.!!<
      Secret(res) // should we trim that?
    }
    def asString: Secret[String] = {
      val json = writeToString(command.toList)(commandCodec)
      Secret(s"command:$json")
    }
  }
}
