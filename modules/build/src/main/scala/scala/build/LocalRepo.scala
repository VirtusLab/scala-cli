package scala.build

import coursier.paths.Util

import java.io.{BufferedInputStream, ByteArrayOutputStream, Closeable, InputStream}
import java.nio.channels.{FileChannel, FileLock}
import java.nio.file.{Path, StandardOpenOption}
import java.util.zip.{ZipEntry, ZipInputStream}

import scala.build.internal.Constants

object LocalRepo {

  private def resourcePath = Constants.localRepoResourcePath
  private def version      = Constants.localRepoVersion

  private def using[S <: Closeable, T](is: => S)(f: S => T): T = {
    var is0 = Option.empty[S]
    try {
      is0 = Some(is)
      f(is0.get)
    }
    finally if (is0.nonEmpty) is0.get.close()
  }

  private def readContent(is: InputStream): Array[Byte] = {
    val baos = new ByteArrayOutputStream

    val buf  = Array.ofDim[Byte](16 * 1024)
    var read = -1
    while ({
      read = is.read(buf)
      read >= 0
    })
      baos.write(buf, 0, read)

    baos.toByteArray
  }

  private def extractZip(zis: ZipInputStream, dest: os.Path): Unit = {
    var ent: ZipEntry = null
    while ({
      ent = zis.getNextEntry()
      ent != null
    })
      if (!ent.isDirectory) {
        val content = readContent(zis)
        zis.closeEntry()
        os.write(
          dest / ent.getName.split('/').toSeq,
          content,
          createFolders = true
        )
      }
  }

  def localRepo(
    baseDir: os.Path,
    loader: ClassLoader = Thread.currentThread().getContextClassLoader
  ): Option[String] = {
    val archiveUrl = loader.getResource(resourcePath)

    if (archiveUrl == null) None
    else {
      val repoDir = baseDir / version

      if (!os.exists(repoDir))
        withLock((repoDir / os.up).toNIO, version) {
          val tmpRepoDir = repoDir / os.up / s".$version.tmp"
          os.remove.all(tmpRepoDir)
          using(archiveUrl.openStream()) { is =>
            using(new ZipInputStream(new BufferedInputStream(is))) { zis =>
              extractZip(zis, tmpRepoDir)
            }
          }
          os.move(tmpRepoDir, repoDir)
        }

      val repo = "ivy:" + repoDir.toNIO.toUri.toASCIIString + "/[defaultPattern]"
      Some(repo)
    }
  }

  private val intraProcessLock = new Object
  private def withLock[T](dir: Path, id: String)(f: => T): T =
    intraProcessLock.synchronized {
      val lockFile = dir.resolve(s".lock-$id");
      Util.createDirectories(lockFile.getParent)
      var channel: FileChannel = null

      try {
        channel = FileChannel.open(
          lockFile,
          StandardOpenOption.CREATE,
          StandardOpenOption.WRITE,
          StandardOpenOption.DELETE_ON_CLOSE
        )

        var lock: FileLock = null
        try {
          lock = channel.lock()

          try f
          finally {
            lock.release()
            lock = null
            channel.close()
            channel = null
          }
        }
        finally if (lock != null) lock.release()
      }
      finally if (channel != null) channel.close()
    }

}
