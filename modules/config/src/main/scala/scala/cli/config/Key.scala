package scala.cli.config

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

import scala.cli.config.Util._ // only used in 2.12, unused import in 2.13

/** A configuration key
  */
abstract class Key[T] {

  /** Key prefix, such as "foo.a" in "foo.a.b" */
  def prefix: Seq[String]

  /** Key name, such as "b" in "foo.a.b" */
  def name: String

  /** Try to parse a value of this key */
  def parse(json: Array[Byte]): Either[Key.EntryError, T]

  /** Converts a value of this key to JSON
    *
    * @return
    *   UTF-8 encoded JSON
    */
  def write(value: T): Array[Byte]

  /** Converts a value of this key to a sequence of strings
    *
    * Such a sequence can be printed in the console, and converted back to a [[T]] with
    * [[fromString]].
    */
  def asString(value: T): Seq[String]

  /** Reads a value of this key from a sequence of string */
  def fromString(values: Seq[String]): Either[Key.MalformedValue, T]

  /** The fully qualified name of this key */
  final def fullName: String = (prefix :+ name).mkString(".")

  /** A short description of a particular key's purpose and syntax for its values. */
  def description: String

  /** A flag indicating whether the key should by default be hidden in help outputs or not. */
  def hidden: Boolean = false

  /** Whether this key corresponds to a password (see [[Key.PasswordEntry]]) */
  def isPasswordOption: Boolean = false
}

object Key {

  abstract class EntryError(
    message: String,
    causeOpt: Option[Throwable] = None
  ) extends Exception(message, causeOpt.orNull)

  final class JsonReaderError(cause: JsonReaderException)
      extends EntryError("Error parsing config JSON", Some(cause))

  final class MalformedValue(
    entry: Key[_],
    input: Seq[String],
    messageOrExpectedShape: Either[String, String],
    cause: Option[Throwable] = None
  ) extends EntryError(
        s"Malformed values ${input.mkString(", ")} for ${entry.fullName}, " +
          messageOrExpectedShape.fold(shape => s"expected $shape", identity),
        cause
      )

  final class MalformedEntry(
    entry: Key[_],
    messages: ::[String]
  ) extends EntryError(
        s"Malformed entry ${entry.fullName}, " +
          messages.mkString(", ")
      )

  private val stringCodec: JsonValueCodec[String]   = JsonCodecMaker.make
  private val booleanCodec: JsonValueCodec[Boolean] = JsonCodecMaker.make

  final class StringEntry(
    val prefix: Seq[String],
    val name: String,
    val description: String = "",
    override val hidden: Boolean = false
  ) extends Key[String] {
    def parse(json: Array[Byte]): Either[EntryError, String] =
      try Right(readFromArray(json)(stringCodec))
      catch {
        case e: JsonReaderException =>
          Left(new JsonReaderError(e))
      }
    def write(value: String): Array[Byte] =
      writeToArray(value)(stringCodec)
    def asString(value: String): Seq[String] =
      Seq(value)
    def fromString(values: Seq[String]): Either[MalformedValue, String] =
      values match {
        case Seq(value) => Right(value)
        case _          => Left(new MalformedValue(this, values, Left("value")))
      }
  }

  final class BooleanEntry(
    val prefix: Seq[String],
    val name: String,
    val description: String = "",
    override val hidden: Boolean = false
  ) extends Key[Boolean] {
    def parse(json: Array[Byte]): Either[EntryError, Boolean] =
      try Right(readFromArray(json)(booleanCodec))
      catch {
        case e: JsonReaderException =>
          Left(new JsonReaderError(e))
      }
    def write(value: Boolean): Array[Byte] =
      writeToArray(value)(booleanCodec)
    def asString(value: Boolean): Seq[String] =
      Seq(value.toString())
    def fromString(values: Seq[String]): Either[MalformedValue, Boolean] =
      values match {
        case Seq(value) if value.toBooleanOption.isDefined => Right(value.toBoolean)
        case _ => Left(new MalformedValue(this, values, Left("value")))
      }
  }

  final class PasswordEntry(
    val prefix: Seq[String],
    val name: String,
    val description: String = "",
    override val hidden: Boolean = false
  ) extends Key[PasswordOption] {
    def parse(json: Array[Byte]): Either[EntryError, PasswordOption] =
      try {
        val str = readFromArray(json)(stringCodec)
        PasswordOption.parse(str).left.map { e =>
          new MalformedValue(this, Seq(str), Right(e))
        }
      }
      catch {
        case e: JsonReaderException =>
          Left(new JsonReaderError(e))
      }
    def write(value: PasswordOption): Array[Byte] =
      writeToArray(value.asString.value)(stringCodec)
    def asString(value: PasswordOption): Seq[String] = Seq(value.asString.value)
    def fromString(values: Seq[String]): Either[MalformedValue, PasswordOption] =
      values match {
        case Seq(value) =>
          PasswordOption.parse(value).left.map { err =>
            new MalformedValue(this, values, Right(err))
          }
        case _ => Left(new MalformedValue(this, values, Left("value")))
      }

    override def isPasswordOption: Boolean = true
  }

  private val stringListCodec: JsonValueCodec[List[String]] = JsonCodecMaker.make

  final class StringListEntry(
    val prefix: Seq[String],
    val name: String,
    val description: String = "",
    override val hidden: Boolean = false
  ) extends Key[List[String]] {
    def parse(json: Array[Byte]): Either[EntryError, List[String]] =
      try Right(readFromArray(json)(stringListCodec))
      catch {
        case e: JsonReaderException =>
          Left(new JsonReaderError(e))
      }
    def write(value: List[String]): Array[Byte] =
      writeToArray(value)(stringListCodec)
    def asString(value: List[String]): Seq[String] = value
    def fromString(values: Seq[String]): Either[MalformedValue, List[String]] =
      Right(values.toList)
  }

}
