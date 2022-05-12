package scala.build.internal.zip

import java.io.{Closeable, InputStream}
import java.util.zip.ZipEntry

/*
 * juz.ZipInputStream is buggy on Arch Linux from native images (CRC32 calculation issues,
 * see oracle/graalvm#4479), so we use a custom ZipInputStream with disabled CRC32 calculation.
 */
final case class WrappedZipInputStream(
  wrapped: Either[io.github.scala_cli.zip.ZipInputStream, java.util.zip.ZipInputStream]
) extends Closeable {
  def entries(): Iterator[ZipEntry] = {
    val getNextEntry = wrapped match {
      case Left(zis)  => () => zis.getNextEntry()
      case Right(zis) => () => zis.getNextEntry()
    }
    Iterator.continually(getNextEntry()).takeWhile(_ != null)
  }
  def closeEntry(): Unit =
    wrapped match {
      case Left(zis)  => zis.closeEntry()
      case Right(zis) => zis.closeEntry()
    }
  def readAllBytes(): Array[Byte] =
    wrapped.merge.readAllBytes()
  def close(): Unit =
    wrapped.merge.close()
}

object WrappedZipInputStream {
  lazy val shouldUseVendoredImplem = {
    def toBoolean(input: String): Boolean =
      input match {
        case "true" | "1" => true
        case _            => false
      }
    Option(System.getenv("SCALA_CLI_VENDORED_ZIS")).map(toBoolean)
      .orElse(sys.props.get("scala-cli.zis.vendored").map(toBoolean))
      .getOrElse(false)
  }
  def create(is: InputStream): WrappedZipInputStream =
    WrappedZipInputStream {
      if (shouldUseVendoredImplem) Left(new io.github.scala_cli.zip.ZipInputStream(is))
      else Right(new java.util.zip.ZipInputStream(is))
    }
}
