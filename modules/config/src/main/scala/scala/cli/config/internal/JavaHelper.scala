package scala.cli.config.internal

import java.lang.{Boolean => JBoolean}
import java.nio.file.Path

import scala.cli.config._

object JavaHelper {

  private var dbOpt = Option.empty[ConfigDb]

  def open(dbPath: Path): Unit =
    if (dbOpt.isEmpty) {
      val db0 = ConfigDb.open(dbPath) match {
        case Left(ex)   => throw new Exception(ex)
        case Right(db1) => db1
      }
      dbOpt = Some(db0)
    }

  def close(): Unit =
    if (dbOpt.nonEmpty)
      dbOpt = None

  private def split(key: String): (Seq[String], String) = {
    val elems = key.split("\\.")
    (elems.init.toSeq, elems.last)
  }

  def getString(key: String): String = {
    val db             = dbOpt.getOrElse(sys.error("DB not open"))
    val (prefix, name) = split(key)
    val key0           = new Key.StringEntry(prefix, name)
    db.get(key0) match {
      case Left(ex)         => throw new Exception(ex)
      case Right(None)      => null
      case Right(Some(str)) => str
    }
  }

  def getBoolean(key: String): JBoolean = {
    val db             = dbOpt.getOrElse(sys.error("DB not open"))
    val (prefix, name) = split(key)
    val key0           = new Key.BooleanEntry(prefix, name)
    db.get(key0) match {
      case Left(ex)           => throw new Exception(ex)
      case Right(None)        => null
      case Right(Some(value)) => value
    }
  }

  def getStringList(key: String): Array[String] = {
    val db             = dbOpt.getOrElse(sys.error("DB not open"))
    val (prefix, name) = split(key)
    val key0           = new Key.StringListEntry(prefix, name)
    db.get(key0) match {
      case Left(ex)           => throw new Exception(ex)
      case Right(None)        => null
      case Right(Some(value)) => value.toArray
    }
  }

  def getPassword(key: String): String = {
    val db             = dbOpt.getOrElse(sys.error("DB not open"))
    val (prefix, name) = split(key)
    val key0           = new Key.PasswordEntry(prefix, name)
    db.get(key0) match {
      case Left(ex)         => throw new Exception(ex)
      case Right(None)      => null
      case Right(Some(str)) => str.get().value
    }
  }

  def getPasswordBytes(key: String): Array[Byte] = {
    val db             = dbOpt.getOrElse(sys.error("DB not open"))
    val (prefix, name) = split(key)
    val key0           = new Key.PasswordEntry(prefix, name)
    db.get(key0) match {
      case Left(ex)         => throw new Exception(ex)
      case Right(None)      => null
      case Right(Some(str)) => str.getBytes().value
    }
  }
}
