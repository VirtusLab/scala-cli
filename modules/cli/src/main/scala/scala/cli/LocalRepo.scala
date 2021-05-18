package scala.cli

import scala.cli.internal.Constants
import coursier.cache.shaded.dirs.dev.dirs.ProjectDirectories
import java.util.zip.ZipInputStream
import java.io.BufferedInputStream
import java.util.zip.ZipEntry
import java.io.InputStream
import java.io.ByteArrayOutputStream

object LocalRepo {

  private def resourcePath = Constants.localRepoResourcePath
  private def version = Constants.localRepoVersion

  def localRepo(loader: ClassLoader = Thread.currentThread().getContextClassLoader): Option[coursierapi.Repository] = {
    val archiveUrl = loader.getResource(resourcePath)

    if (archiveUrl == null) None
    else {
      // FIXME We should acquire a file lock to do that
      val projDirs = ProjectDirectories.from(null, null, "ScalaCli")
      val cacheDir = os.Path(projDirs.cacheDir, os.pwd)
      val repoDir = cacheDir / "local-repo" / version
      val tmpRepoDir = repoDir / os.up / s".$version.tmp"
      val repo = coursierapi.IvyRepository.of(repoDir.toNIO.toUri + "/[defaultPattern]")
      if (!os.exists(repoDir)) {
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

}
