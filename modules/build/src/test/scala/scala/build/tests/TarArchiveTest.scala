package scala.build.tests

import os.Path

import java.nio.file.Files
import scala.build.TarArchive

class TarArchiveTest extends munit.FunSuite {

  test("archive can be decompressed") {
    val cl      = getClass.getClassLoader
    val testTar = os.resource(cl) / "test_archive.tar.bz2"

    val tempDir = Files.createTempDirectory("").toFile.toPath
    try {
      val result = TarArchive.decompress(os.read.inputStream(testTar), Path(tempDir))
      assert(result.isRight)
      assert(os.exists(Path(tempDir.toString) / 'test_archive / 'test_dir / "testfile_in_folder.txt"))
    }
    finally {
      // Files.delete(tempDir) // TODO: FixMe
    }
  }
}
