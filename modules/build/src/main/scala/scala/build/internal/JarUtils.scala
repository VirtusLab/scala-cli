package scala.build.internal

import java.io.ByteArrayInputStream
import java.nio.file.NoSuchFileException

import scala.build.internal.zip.WrappedZipInputStream
import scala.build.{Logger, retry}

object JarUtils {

  /** Walk `.class` entries in a JAR */
  def walkClassEntries[A](jar: os.Path, logger: Logger)(
    extract: (String, () => Array[Byte]) => Iterator[A]
  ): Iterator[A] =
    try
      retry()(logger) {
        val content = os.read.bytes(jar)
        val zip     = WrappedZipInputStream.create(new ByteArrayInputStream(content))
        zip.entries().flatMap { ent =>
          if !ent.isDirectory && ent.getName.endsWith(".class") then
            extract(ent.getName, () => zip.readAllBytes())
          else Iterator.empty
        }
      }
    catch {
      case e: NoSuchFileException =>
        logger.debugStackTrace(e)
        logger.debug(s"JAR file $jar not found: $e, skipping.")
        Iterator.empty
    }

}
