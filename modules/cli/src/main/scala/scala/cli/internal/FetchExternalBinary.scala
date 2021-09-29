package scala.cli.internal

import coursier.cache.FileCache
import coursier.jvm.ArchiveType
import coursier.util.{Artifact, Task}

import java.io.{FileInputStream, FileOutputStream, IOException}
import java.util.zip.GZIPInputStream
import java.util.{Locale, UUID}

import scala.build.Logger
import scala.build.internal.OsLibc
import scala.util.Properties

object FetchExternalBinary {

  def fetch(
    url: String,
    changing: Boolean,
    cache: FileCache[Task],
    logger: Logger,
    launcherPrefix: String
  ) = {

    val f = cache.logger.use {
      logger.log(s"Getting $url")
      cache.file(Artifact(url).withChanging(changing)).run.flatMap {
        case Left(e)  => Task.fail(e)
        case Right(f) => Task.point(os.Path(f, os.pwd))
      }.unsafeRun()(cache.ec)
    }
    logger.debug(s"$url is available locally at $f")

    val launcher =
      // FIXME Once coursier has proper support for extracted archives in cache, use it instead of those hacks
      if (f.last.endsWith(".zip")) {
        val baseDir = f / os.up
        val dir     = baseDir / s".${f.last.stripSuffix(".zip")}-content"
        if (os.exists(dir))
          logger.debug(s"Found $dir")
        else {
          logger.debug(s"Unzipping $f under $dir")
          val tmpDir = baseDir / s".${f.last.stripSuffix(".zip")}-content-${UUID.randomUUID()}"
          try {
            coursier.jvm.UnArchiver.default().extract(
              ArchiveType.Zip,
              f.toIO,
              tmpDir.toIO,
              overwrite = false
            )
            if (!os.exists(dir)) {
              try os.move(tmpDir, dir, atomicMove = true)
              catch {
                case ex: IOException =>
                  if (!os.exists(dir))
                    throw new Exception(ex)
              }
            }
          }
          finally {
            try os.remove.all(tmpDir)
            catch {
              case _: IOException if Properties.isWin =>
            }
          }
        }

        val dirContent = os.list(dir)
        if (dirContent.length == 1) dirContent.head
        else dirContent.filter(_.last.startsWith(launcherPrefix)).head
      }
      else if (f.last.endsWith(".gz")) {
        val dest = f / os.up / s".${f.last.stripSuffix(".gz")}"
        if (os.exists(dest))
          logger.debug(s"Found $dest")
        else {
          logger.debug(s"Uncompression $f at $dest")
          var fis: FileInputStream  = null
          var fos: FileOutputStream = null
          var gzis: GZIPInputStream = null
          try {
            fis = new FileInputStream(f.toIO)
            gzis = new GZIPInputStream(fis)
            fos = new FileOutputStream(dest.toIO)

            val buf  = Array.ofDim[Byte](16 * 1024)
            var read = -1
            while ({
              read = gzis.read(buf)
              read >= 0
            }) {
              if (read > 0)
                fos.write(buf, 0, read)
            }
            fos.flush()
          }
          finally {
            if (gzis != null) gzis.close()
            if (fos != null) fos.close()
            if (fis != null) fis.close()
          }
        }
        dest
      }
      else
        f

    if (!Properties.isWin)
      os.perms.set(launcher, "rwxr-xr-x")

    launcher
  }

  def platformSuffix(supportsMusl: Boolean = true): String = {
    val arch = sys.props("os.arch").toLowerCase(Locale.ROOT) match {
      case "amd64" => "x86_64"
      case other   => other
    }
    val os =
      if (Properties.isWin) "pc-win32"
      else if (Properties.isLinux) {
        if (supportsMusl && OsLibc.isMusl.getOrElse(false))
          "pc-linux-static"
        else
          "pc-linux"
      }
      else if (Properties.isMac) "apple-darwin"
      else sys.error(s"Unrecognized OS: ${sys.props("os.name")}")
    s"$arch-$os"
  }

}
