package scala.cli

import coursier.cache.shaded.dirs.dev.dirs.ProjectDirectories
import coursier.paths.Util
import scala.cli.internal.Constants

import java.io.{ByteArrayOutputStream, BufferedInputStream, InputStream}
import java.nio.channels.{FileChannel, FileLock}
import java.nio.file.{Path, StandardOpenOption}
import java.util.zip.{ZipEntry, ZipInputStream}

object LocalRepo {

  private def resourcePath = Constants.localRepoResourcePath
  private def version = Constants.localRepoVersion

  def localRepo(loader: ClassLoader = Thread.currentThread().getContextClassLoader): Option[coursierapi.Repository] = {
    val archiveUrl = loader.getResource(resourcePath)

    if (archiveUrl == null) None
    else {
      val projDirs = ProjectDirectories.from(null, null, "ScalaCli")
      val cacheDir = os.Path(projDirs.cacheDir, os.pwd)
      val repoDir = cacheDir / "local-repo" / version
      val tmpRepoDir = repoDir / os.up / s".$version.tmp"
      val repo = coursierapi.IvyRepository.of(repoDir.toNIO.toUri + "/[defaultPattern]")
      if (!os.exists(repoDir))
        withLock((repoDir / os.up).toNIO, version) {
          os.remove.all(tmpRepoDir)
          var is: InputStream = null
          var zis: ZipInputStream = null
          try {
            is = archiveUrl.openStream()
            zis = new ZipInputStream(new BufferedInputStream(is))
            var ent: ZipEntry = null
            val buf = Array.ofDim[Byte](16*1024)
            while ({
              ent = zis.getNextEntry()
              ent != null
            }) {
              if (!ent.isDirectory) {
                val baos = new ByteArrayOutputStream
                var read = -1
                while ({
                  read = zis.read(buf)
                  read >= 0
                })
                  baos.write(buf, 0, read)
                zis.closeEntry()
                os.write(tmpRepoDir / ent.getName.split('/').toSeq, baos.toByteArray, createFolders = true)
              }
            }
          } finally {
            if (zis != null) zis.close()
            if (is != null) is.close()
          }
          os.move(tmpRepoDir, repoDir)
        }

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
        finally {
          if (lock != null) lock.release()
        }
      } finally {
        if (channel != null) channel.close()
      }
    }

}
