package scala.cli.config

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

import scala.cli.commands.SpecificationLevel
import scala.cli.config.PublishCredentialsAsJson._
import scala.cli.config.RepositoryCredentialsAsJson._
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

  /** The [[SpecificationLevel]] of the key. [[SpecificationLevel.RESTRICTED]] &&
    * [[SpecificationLevel.EXPERIMENTAL]] keys are only available in `power` mode.
    */
  def specificationLevel: SpecificationLevel

  def isExperimental: Boolean = specificationLevel == SpecificationLevel.EXPERIMENTAL
  def isRestricted: Boolean   = specificationLevel == SpecificationLevel.RESTRICTED
}

object Key {
  private implicit lazy val stringJsonCodec: JsonValueCodec[String]           = JsonCodecMaker.make
  private implicit lazy val stringListJsonCodec: JsonValueCodec[List[String]] = JsonCodecMaker.make
  private implicit lazy val booleanJsonCodec: JsonValueCodec[Boolean]         = JsonCodecMaker.make

  abstract class EntryError(
    message: String,
    causeOpt: Option[Throwable] = None
  ) extends Exception(message, causeOpt.orNull)

  private final class JsonReaderError(cause: JsonReaderException)
      extends EntryError("Error parsing config JSON", Some(cause))

  final class MalformedValue(
    entry: Key[_],
    input: Seq[String],
    messageOrExpectedShape: Either[String, String],
    cause: Option[Throwable] = None
  ) extends EntryError(
        {
          val valueWord    = if (input.length > 1) "values" else "value"
          val valuesString = input.map(s => s"'$s'").mkString(", ")
          val errorMessage = messageOrExpectedShape
            .fold(shape => s", expected $shape", errorMessage => s". $errorMessage")
          s"Malformed $valueWord $valuesString for the '${entry.fullName}' entry$errorMessage"
        },
        cause
      )

  private final class MalformedEntry(
    entry: Key[_],
    messages: ::[String]
  ) extends EntryError(
        s"Malformed entry ${entry.fullName}, " +
          messages.mkString(", ")
      )

  abstract class KeyWithJsonCodec[T](implicit jsonCodec: JsonValueCodec[T]) extends Key[T] {
    def parse(json: Array[Byte]): Either[Key.EntryError, T] =
      try Right(readFromArray(json))
      catch {
        case e: JsonReaderException =>
          Left(new Key.JsonReaderError(e))
      }

    def write(value: T): Array[Byte] = writeToArray(value)
  }

  final class StringEntry(
    val prefix: Seq[String],
    val name: String,
    override val specificationLevel: SpecificationLevel,
    val description: String = "",
    override val hidden: Boolean = false
  ) extends KeyWithJsonCodec[String] {
    def asString(value: String): Seq[String] =
      Seq(value)
    def fromString(values: Seq[String]): Either[MalformedValue, String] =
      values match {
        case Seq(value) => Right(value)
        case _          => Left(new MalformedValue(this, values, Left("a single string value.")))
      }
  }

  final class BooleanEntry(
    val prefix: Seq[String],
    val name: String,
    override val specificationLevel: SpecificationLevel,
    val description: String = "",
    override val hidden: Boolean = false
  ) extends KeyWithJsonCodec[Boolean] {
    def asString(value: Boolean): Seq[String] =
      Seq(value.toString)
    def fromString(values: Seq[String]): Either[MalformedValue, Boolean] =
      values match {
        case Seq(value) if value.toBooleanOption.isDefined => Right(value.toBoolean)
        case _                                             =>
          Left(new MalformedValue(
            this,
            values,
            Left("a single boolean value ('true' or 'false').")
          ))
      }
  }

  final class PasswordEntry(
    val prefix: Seq[String],
    val name: String,
    override val specificationLevel: SpecificationLevel,
    val description: String = "",
    override val hidden: Boolean = false
  ) extends Key[PasswordOption] {
    def parse(json: Array[Byte]): Either[EntryError, PasswordOption] =
      try {
        val str = readFromArray(json)(stringJsonCodec)
        PasswordOption.parse(str).left.map { e =>
          new MalformedValue(this, Seq(str), Right(e))
        }
      }
      catch {
        case e: JsonReaderException =>
          Left(new JsonReaderError(e))
      }
    def write(value: PasswordOption): Array[Byte] =
      writeToArray(value.asString.value)(stringJsonCodec)
    def asString(value: PasswordOption): Seq[String] = Seq(value.asString.value)
    def fromString(values: Seq[String]): Either[MalformedValue, PasswordOption] =
      values match {
        case Seq(value) =>
          PasswordOption.parse(value).left.map { err =>
            new MalformedValue(this, values, Right(err))
          }
        case _ => Left(new MalformedValue(
            this,
            values,
            Left("a single password value (format: 'value:password').")
          ))
      }

    override def isPasswordOption: Boolean = true
  }

  final class StringListEntry(
    val prefix: Seq[String],
    val name: String,
    override val specificationLevel: SpecificationLevel,
    val description: String = "",
    override val hidden: Boolean = false
  ) extends KeyWithJsonCodec[List[String]] {
    def asString(value: List[String]): Seq[String]                            = value
    def fromString(values: Seq[String]): Either[MalformedValue, List[String]] =
      Right(values.toList)
  }
  abstract class CredentialsEntry[T <: CredentialsValue, U <: CredentialsAsJson[T]](implicit
    jsonCodec: JsonValueCodec[List[U]]
  ) extends Key[List[T]] {
    protected def asJson(credentials: T): U
    def parse(json: Array[Byte]): Either[Key.EntryError, List[T]] =
      try {
        val list   = readFromArray(json).map(_.credentials)
        val errors = list.collect { case Left(errors) => errors }.flatten
        errors match {
          case Nil =>
            Right(list.collect { case Right(v) => v })
          case h :: t =>
            Left(new Key.MalformedEntry(this, ::(h, t)))
        }
      }
      catch {
        case e: JsonReaderException =>
          Left(new Key.JsonReaderError(e))
      }
    def write(value: List[T]): Array[Byte] = writeToArray(value.map(asJson))
    def fromString(values: Seq[String]): Either[MalformedValue, List[T]] =
      Left(new Key.MalformedValue(this, values, Right(ErrorMessages.inlineCredentialsError)))
    def asString(value: List[T]): Seq[String] = value.map(_.asString)
  }

  final class RepositoryCredentialsEntry(
    val prefix: Seq[String],
    val name: String,
    override val specificationLevel: SpecificationLevel,
    val description: String = "",
    override val hidden: Boolean = false
  ) extends CredentialsEntry[RepositoryCredentials, RepositoryCredentialsAsJson] {
    def asJson(credentials: RepositoryCredentials): RepositoryCredentialsAsJson =
      RepositoryCredentialsAsJson(
        credentials.host,
        credentials.user.map(_.asString.value),
        credentials.password.map(_.asString.value),
        credentials.realm,
        credentials.optional,
        credentials.matchHost,
        credentials.httpsOnly,
        credentials.passOnRedirect
      )

    override def asString(value: List[RepositoryCredentials]): Seq[String] =
      value
        .zipWithIndex
        .map {
          case (cred, idx) =>
            val prefix = s"configRepo$idx."
            cred.asString.linesWithSeparators.map(prefix + _).mkString
        }
  }

  class PublishCredentialsEntry(
    val prefix: Seq[String],
    val name: String,
    override val specificationLevel: SpecificationLevel,
    val description: String = "",
    override val hidden: Boolean = false
  ) extends CredentialsEntry[PublishCredentials, PublishCredentialsAsJson] {
    def asJson(credentials: PublishCredentials): PublishCredentialsAsJson =
      PublishCredentialsAsJson(
        credentials.host,
        credentials.user.map(_.asString.value),
        credentials.password.map(_.asString.value),
        credentials.realm,
        credentials.httpsOnly
      )
  }
}
