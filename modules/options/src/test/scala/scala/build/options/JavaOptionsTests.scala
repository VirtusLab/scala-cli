package scala.build.options

import com.eed3si9n.expecty.Expecty.expect
import coursier.jvm.JvmChannel

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
}
