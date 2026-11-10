package scala.cli.commands.github

import munit.FunSuite

final class LibSodiumJniTests extends FunSuite {

  private val version    = "1.0.22"
  private val msvcZipUrl =
    s"https://download.libsodium.org/libsodium/releases/libsodium-$version-stable-msvc.zip"

  for
    platform <- Seq(
      "x86_64-pc-linux",
      "aarch64-pc-linux",
      "x86_64-apple-darwin",
      "aarch64-apple-darwin"
    )
  do
    test(s"no libsodium download needed on $platform (statically linked in libsodiumjni)") {
      assertEquals(LibSodiumJni.libsodiumArchive(platform, version), Right(None))
    }

  test("Windows x64 gets libsodium.dll from the libsodium MSVC archive") {
    assertEquals(
      LibSodiumJni.libsodiumArchive("x86_64-pc-win32", version),
      Right(Some(LibSodiumJni.LibSodiumArchive(
        msvcZipUrl,
        os.rel / "libsodium" / "x64" / "Release" / "v143" / "dynamic" / "libsodium.dll"
      )))
    )
  }

  test("Windows ARM64 gets libsodium.dll from the libsodium MSVC archive") {
    assertEquals(
      LibSodiumJni.libsodiumArchive("aarch64-pc-win32", version),
      Right(Some(LibSodiumJni.LibSodiumArchive(
        msvcZipUrl,
        os.rel / "libsodium" / "ARM64" / "Release" / "v143" / "dynamic" / "libsodium.dll"
      )))
    )
  }

  for platform <- Seq("ppc64le-pc-linux", "x86-pc-win32", "riscv64-apple-darwin", "x86_64-freebsd")
  do
    test(s"unsupported platform $platform is rejected") {
      assert(LibSodiumJni.libsodiumArchive(platform, version).isLeft)
    }

  for
    (platform, ext) <- Seq(
      "x86_64-pc-linux"      -> "so",
      "aarch64-pc-linux"     -> "so",
      "x86_64-apple-darwin"  -> "dylib",
      "aarch64-apple-darwin" -> "dylib",
      "x86_64-pc-win32"      -> "dll",
      "aarch64-pc-win32"     -> "dll"
    )
  do
    test(s"libsodiumjni shared library extension on $platform is $ext") {
      assertEquals(LibSodiumJni.jniLibExtension(platform), Right(ext))
    }
}
