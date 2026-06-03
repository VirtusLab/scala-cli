package scala.build.tests

import coursier.cache.FileCache

import scala.build.LocalRepo

class LocalRepoTests extends TestUtil.ScalaCliBuildSuite {
  test("localRepoBaseDir is anchored to the coursier cache location") {
    val cacheDir = os.temp.dir(prefix = "scala-cli-cache-")
    val cache    = FileCache().withLocation(cacheDir.toIO)
    assertEquals(LocalRepo.localRepoBaseDir(cache), cacheDir / "scalacli-local-repo")
  }
}
