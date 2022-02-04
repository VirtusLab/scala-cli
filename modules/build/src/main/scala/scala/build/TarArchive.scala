package scala.build

import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveInputStream}
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.utils.IOUtils
import os.{Path, RelPath}

import java.io.{FileOutputStream, InputStream}
import java.nio.file.Files

import scala.util.Try

object TarArchive {
  def decompress(inputStream: InputStream, outputPath: Path): Either[Throwable, Path] = Try {
    val content = new TarArchiveInputStream(new BZip2CompressorInputStream(inputStream))
    Stream.continually(Option(content.getNextTarEntry))
      .takeWhile(_.isDefined)
      .flatten
      .foreach {
        case directory: TarArchiveEntry if directory.isDirectory() =>
          val directoryPath = outputPath / RelPath(directory.getName())
          Files.createDirectories(directoryPath.toNIO)
        case file: TarArchiveEntry if file.isFile() =>
          val filePath     = outputPath / RelPath(file.getName())
          val fileBasePath = filePath / os.up
          Files.createDirectories(fileBasePath.toNIO)
          val entryOs = new FileOutputStream(filePath.toString())
          IOUtils.copy(content, entryOs)
          entryOs.close()
      }
    outputPath
  }.toEither
}
