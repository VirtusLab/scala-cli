package scala.cli.config

import com.github.plokhotnyuk.jsoniter_scala.core.{Key as _, *}
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import coursier.parse.RawJson

import java.nio.file.attribute.PosixFilePermission

import scala.collection.immutable.ListMap
import scala.util.Properties

/** In-memory representation of a configuration DB content.
  *
  * Use [[ConfigDb.apply]] or [[ConfigDb.open]] to create an instance of it.
  *
  * [[set]], [[setFromString]], and [[remove]] only change values in memory.
  *
  * Use [[save]] to persist values on disk.
  */
final class ConfigDb private (
  var rawEntries: Map[String, Array[Byte]]
) {

  /** Gets an entry.
    *
    * If the value cannot be decoded, an error is returned on the left side of the either.
    *
    * If the key isn't in DB, None is returned on the right side of the either.
    *
    * Else, the value is returned wrapped in Some on the right side of the either.
    */
  def get[T](key: Key[T]): Either[ConfigDb.ConfigDbFormatError, Option[T]] =
    rawEntries.get(key.fullName) match {
      case None => Right(None)
      case Some(rawEntryContent) =>
        key.parse(rawEntryContent)
          .left.map { e =>
            new ConfigDb.ConfigDbFormatError(s"Error parsing ${key.fullName} value", Some(e))
          }
          .map(Some(_))
    }

  /** Sets an entry in memory */
  def set[T](key: Key[T], value: T): this.type = {
    val b = key.write(value)
    rawEntries += key.fullName -> b
    this
  }

  /** Removes an entry from memory */
  def remove(key: Key[_]): this.type = {
    rawEntries -= key.fullName
    this
  }

  /** Gets an entry in printable form.
    *
    * See [[get]] for when a left value, or a None on the right, can be returned.
    */
  def getAsString[T](key: Key[T]): Either[ConfigDb.ConfigDbFormatError, Option[Seq[String]]] =
    get(key).map(_.map(key.asString))

  /** Sets an entry in memory, from a printable / user-writable representation.
    */
  def setFromString[T](
    key: Key[T],
    values: Seq[String]
  ): Either[Key.MalformedValue, this.type] =
    key.fromString(values).map { typedValue =>
      set(key, typedValue)
    }

  /** Dumps this DB content as JSON */
  def dump: Array[Byte] = {

    def serializeMap(m: Map[String, Array[Byte]]): Array[Byte] = {
      val keyValues = m
        .groupBy(_._1.split("\\.", 2).apply(0))
        .toVector
        .sortBy(_._1)
        .map {
          case (k, v) =>
            val v0 = v.map {
              case (k1, v1) =>
                (k1.stripPrefix(k).stripPrefix("."), v1)
            }
            (k, serialize(v0))
        }
      val sortedMap: Map[String, RawJson] = ListMap.from(keyValues)
      writeToArray(sortedMap)(ConfigDb.codec)
    }

    def serialize(m: Map[String, Array[Byte]]): RawJson =
      m.get("") match {
        case Some(value) =>
          if (m.size == 1)
            RawJson(value)
          else
            sys.error(s"Inconsistent keys: ${m.keySet.toVector.sorted}")
        case None =>
          RawJson(serializeMap(m))
      }

    serializeMap(rawEntries)
  }

  def saveUnsafe(path: os.Path): Either[ConfigDb.ConfigDbPermissionsError, Unit] = {
    val dir = path / os.up

    if (Properties.isWin) {
      os.write.over(path, dump, createFolders = true)
      Right(())
    }
    else {
      if (!os.exists(dir))
        os.makeDir.all(dir, perms = "rwx------")
      val dirPerms = os.perms(dir)
      val permsOk =
        !dirPerms.contains(PosixFilePermission.GROUP_READ) &&
        !dirPerms.contains(PosixFilePermission.GROUP_WRITE) &&
        !dirPerms.contains(PosixFilePermission.GROUP_EXECUTE) &&
        !dirPerms.contains(PosixFilePermission.OTHERS_READ) &&
        !dirPerms.contains(PosixFilePermission.OTHERS_WRITE) &&
        !dirPerms.contains(PosixFilePermission.OTHERS_EXECUTE)
      if (permsOk) {
        os.write.over(path, dump, perms = "rw-------", createFolders = false)
        Right(())
      }
      else
        Left(new ConfigDb.ConfigDbPermissionsError(path, dirPerms))
    }
  }

  /** Saves this DB at the passed path */
  def save(path: os.Path): Either[Exception, Unit] =
    // no file locks…
    saveUnsafe(path)
}

object ConfigDb {

  final class ConfigDbFormatError(
    message: String,
    causeOpt: Option[Throwable] = None
  ) extends Exception(message, causeOpt.orNull)

  final class ConfigDbPermissionsError(path: os.Path, perms: os.PermSet)
      extends Exception(s"$path has wrong permissions $perms (expected rwx------)")

  private val codec: JsonValueCodec[Map[String, RawJson]] = JsonCodecMaker.make

  /** Create a ConfigDb instance from binary content
    *
    * @param dbContent:
    *   JSON, as a UTF-8 array of bytes
    * @param printablePath:
    *   DB location, for error messages
    * @return
    *   either an error on failure, or a ConfigDb instance on success
    */
  def apply(
    dbContent: Array[Byte],
    printablePath: Option[String] = None
  ): Either[ConfigDbFormatError, ConfigDb] = {

    def flatten(map: Map[String, RawJson]): Map[String, Array[Byte]] =
      map.flatMap {
        case (k, v) =>
          try {
            val subMap = flatten(readFromArray(v.value)(codec))
            subMap.toSeq.map {
              case (k0, v0) =>
                (k + "." + k0, v0)
            }
          }
          catch {
            case _: JsonReaderException =>
              Seq(k -> v.value)
          }
      }

    val maybeRawEntries =
      try Right(flatten(readFromArray(dbContent)(codec)))
      catch {
        case e: JsonReaderException =>
          Left(new ConfigDbFormatError(
            "Error parsing config DB" + printablePath.fold("")(" " + _),
            Some(e)
          ))
      }

    maybeRawEntries.map(rawEntries => new ConfigDb(rawEntries))
  }

  /** Creates a ConfigDb from a file
    *
    * @param path:
    *   path to a config UTF-8 JSON file
    * @return
    *   either an error on failure, or a ConfigDb instance on success
    */
  def open(path: os.Path): Either[Exception, ConfigDb] =
    if (os.exists(path))
      apply(os.read.bytes(path), Some(path.toString))
    else
      Right(empty)

  def empty: ConfigDb =
    new ConfigDb(Map())
}
