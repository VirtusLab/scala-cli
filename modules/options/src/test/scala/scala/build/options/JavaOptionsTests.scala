package scala.build.options

import com.eed3si9n.expecty.Expecty.expect
import coursier.cache.{ArchiveCache, CachePolicy, FileCache}
import coursier.jvm.{JvmChannel, JvmIndexEntry}
import coursier.paths.CachePath
import coursier.util.Task

import scala.concurrent.ExecutionContext
import scala.util.Try

class JavaOptionsTests extends munit.FunSuite {
  test("JVM index defaults to the per-OS / CPU index with checksums when available") {
    for (os, arch) <- Seq("darwin" -> "arm64", "linux" -> "amd64", "linux-musl" -> "amd64") do
      expect(
        JavaOptions.jvmIndexChannel(None, os, arch) ==
          JvmChannel.url(JvmChannel.gitHubV2IndexUrl(os, arch))
      )
  }

  test("JVM index falls back to the legacy index for platforms without a per-OS / CPU index") {
    expect(
      JavaOptions.jvmIndexChannel(None, "freebsd", "amd64") ==
        JvmChannel.url(JvmChannel.gitHubIndexUrl)
    )
  }

  test("JVM index passed by users is used as is") {
    val customIndex = "https://example.com/custom-index.json"
    expect(
      JavaOptions.jvmIndexChannel(Some(customIndex), "darwin", "arm64") ==
        JvmChannel.url(customIndex)
    )
  }

  private val v2Channel = JvmChannel.url(JvmChannel.gitHubV2IndexUrl("linux", "amd64"))

  test("offline mode falls back to the legacy JVM index") {
    expect(
      JavaOptions.offlineJvmIndexFallback(None, v2Channel, isOffline = true)
        .contains(JvmChannel.gitHub())
    )
  }

  test("no JVM index fallback when online, for custom indices, or when already on the legacy one") {
    expect(JavaOptions.offlineJvmIndexFallback(None, v2Channel, isOffline = false).isEmpty)
    val customIndex = "https://example.com/custom-index.json"
    expect(
      JavaOptions.offlineJvmIndexFallback(
        Some(customIndex),
        JvmChannel.url(customIndex),
        isOffline = true
      ).isEmpty
    )
    expect(
      JavaOptions.offlineJvmIndexFallback(None, JvmChannel.gitHub(), isOffline = true).isEmpty
    )
  }

  test("offline mode is detected from the cache policies") {
    expect(JavaOptions.isOffline(FileCache().copy(cachePolicies = Seq(CachePolicy.LocalOnly))))
    expect(!JavaOptions.isOffline(FileCache()))
  }

  private val fakeJdkUrl = "tgz+https://example.com/fake-jdk-17.0.1-linux_x64.tar.gz"

  /** Looks up `jvmId` in offline mode, with a cache only containing what `populate` writes */
  private def offlineLookup(jvmId: String)(populate: os.Path => Unit)
    : Try[Either[String, Seq[JvmIndexEntry]]] = {
    val cacheDir = os.temp.dir(prefix = "scala-cli-jvm-index-test-")
    try {
      populate(cacheDir)
      val cache = FileCache()
        .withLocation(cacheDir.toString)
        .copy(cachePolicies = Seq(CachePolicy.LocalOnly))
      val archiveCache = ArchiveCache((cacheDir / "arc").toIO).copy(cache = cache)
      val javaHome     = JavaOptions(jvmIndexOs = Some("linux"), jvmIndexArch = Some("amd64"))
        .javaHomeManager(archiveCache, cache, verbosity = 0)
      given ExecutionContext = cache.ec
      Try(javaHome.cache.get.entries(jvmId).unsafeRun())
    }
    finally os.remove.all(cacheDir)
  }

  private def writeLegacyIndex(cacheDir: os.Path): Unit = {
    val legacyIndexFile = os.Path(
      CachePath.localFile(JvmChannel.gitHubIndexUrl, cacheDir.toIO, null, false)
    )
    os.write(
      legacyIndexFile,
      s"""{"linux": {"amd64": {"jdk@fake": {"17.0.1": "$fakeJdkUrl"}}}}""",
      createFolders = true
    )
  }

  test("JVMs can be looked up offline with only the legacy JVM index in cache") {
    val res = offlineLookup("fake:17")(writeLegacyIndex)
    expect(res.isSuccess)
    val entries = res.get.getOrElse(Nil)
    expect(entries.map(_.url) == Seq(fakeJdkUrl.stripPrefix("tgz+")))
  }

  test("offline JVM lookup reports the per-OS / CPU index error when no index is in cache") {
    val res = offlineLookup("fake:17")(_ => ())
    expect(res.isFailure)
    val messages = Iterator.iterate(res.failed.get)(_.getCause).takeWhile(_ != null)
      .map(e => String.valueOf(e.getMessage)).toSeq
    expect(messages.exists(_.contains(JvmChannel.gitHubV2IndexUrl("linux", "amd64"))))
  }
}
