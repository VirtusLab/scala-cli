package scala.cli.integration

import java.io.{FileOutputStream, IOException}
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.{ZipEntry, ZipOutputStream}

import scala.cli.integration.TestInputs.compress
import scala.util.control.NonFatal

final case class TestInputs(
  files: Seq[(os.RelPath, String)]
) {
  def add(extraFiles: (os.RelPath, String)*): TestInputs =
    copy(files = files ++ extraFiles)
  private def writeIn(dir: os.Path): Unit =
    for ((relPath, content) <- files) {
      val path = dir / relPath
      os.write(path, content.getBytes(StandardCharsets.UTF_8), createFolders = true)
    }
  def root(): os.Path = {
    val tmpDir = TestInputs.tmpDir
    writeIn(tmpDir)
    tmpDir
  }
  def asZip[T](f: (os.Path, os.Path) => T): T =
    TestInputs.withTmpDir { tmpDir =>
      val zipArchivePath = tmpDir / s"${tmpDir.last}.zip"
      compress(zipArchivePath, files)
      f(tmpDir, zipArchivePath)
    }
  def fromRoot[T](f: os.Path => T): T =
    TestInputs.withTmpDir { tmpDir =>
      writeIn(tmpDir)
      f(tmpDir)
    }
}

object TestInputs {

  def compress(zipFilepath: os.Path, files: Seq[(os.RelPath, String)]) = {
    val zip = new ZipOutputStream(new FileOutputStream(zipFilepath.toString()))
    try for ((relPath, content) <- files) {
      zip.putNextEntry(new ZipEntry(relPath.toString()))
      val in: Array[Byte] = content.getBytes
      zip.write(in)
      zip.closeEntry()
    }
    finally zip.close()
  }

  private lazy val baseTmpDir = {
    Option(System.getenv("SCALA_CLI_TMP")).getOrElse {
      sys.error("SCALA_CLI_TMP not set")
    }
    val base = os.Path(System.getenv("SCALA_CLI_TMP"), os.pwd)
    val rng  = new SecureRandom
    val d    = base / s"run-${math.abs(rng.nextInt().toLong)}"
    os.makeDir.all(d)
    Runtime.getRuntime.addShutdownHook(
      new Thread("scala-cli-its-clean-up-tmp-dir") {
        setDaemon(true)
        override def run(): Unit =
          try os.remove.all(d)
          catch {
            case NonFatal(_) =>
              System.err.println(s"Could not remove $d, ignoring it.")
          }
      }
    )
    d
  }

  private val tmpCount = new AtomicInteger

  private def withTmpDir[T](f: os.Path => T): T = {
    val tmpDir = baseTmpDir / s"test-${tmpCount.incrementAndGet()}"
    os.makeDir.all(tmpDir)
    val tmpDir0 = os.Path(tmpDir.toIO.getCanonicalFile)
    def removeAll(): Unit =
      try os.remove.all(tmpDir0)
      catch {
        case ex: IOException =>
          System.err.println(s"Ignoring $ex while removing $tmpDir0")
      }
    try f(tmpDir0)
    finally removeAll()
  }

  private def tmpDir: os.Path = {
    val tmpDir = baseTmpDir / s"test-${tmpCount.incrementAndGet()}"
    os.makeDir.all(tmpDir)
    os.Path(tmpDir.toIO.getCanonicalFile)
  }
}
