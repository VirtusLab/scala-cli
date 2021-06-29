package scala.cli.integration

import java.io.IOException
import java.nio.charset.StandardCharsets

final case class TestInputs(
  files: Seq[(os.RelPath, String)]
) {
  private def writeIn(dir: os.Path): Unit =
    for ((relPath, content) <- files) {
      val path = dir / relPath
      os.write(path, content.getBytes(StandardCharsets.UTF_8), createFolders = true)
    }
  def root(): os.Path = {
    val tmpDir = TestInputs.tmpDir("scala-cli-tests-")
    writeIn(tmpDir)
    tmpDir
  }
  def fromRoot[T](f: os.Path => T): T =
    TestInputs.withTmpDir("scala-cli-tests-") { tmpDir =>
      writeIn(tmpDir)
      f(tmpDir)
    }
}

object TestInputs {

  private def withTmpDir[T](prefix: String)(f: os.Path => T): T = {
    val tmpDir = os.temp.dir(prefix = prefix)
    try f(tmpDir)
    finally {
      try os.remove.all(tmpDir)
      catch {
        case ex: IOException =>
          System.err.println(s"Ignoring $ex while removing $tmpDir")
      }
    }
  }
  private def tmpDir(prefix: String): os.Path =
    os.temp.dir(prefix = prefix)
}
