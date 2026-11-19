package scala.cli.commands.github

import libsodiumjni.internal.LoadLibrary
import munit.FunSuite

final class LibSodiumJniTests extends FunSuite {

  // libsodiumjni artifact classifier (as computed by FetchExternalBinary.platformSuffix), with
  // matching os.name / os.arch values, and the libsodiumjni shared library extension
  private val platforms = Seq(
    ("x86_64-pc-linux", "Linux", "amd64", "so"),
    ("aarch64-pc-linux", "Linux", "aarch64", "so"),
    ("x86_64-apple-darwin", "Mac OS X", "x86_64", "dylib"),
    ("aarch64-apple-darwin", "Mac OS X", "aarch64", "dylib"),
    ("x86_64-pc-win32", "Windows 11", "amd64", "dll"),
    ("aarch64-pc-win32", "Windows 11", "aarch64", "dll")
  )

  for (platform, osName, osArch, ext) <- platforms do
    test(s"libsodiumjni shared library extension on $platform is $ext") {
      assertEquals(LibSodiumJni.jniLibExtension(platform), Right(ext))
    }

    test(s"libsodiumjni ships a .$ext shared library for $platform") {
      val resourcePath = LoadLibrary.resourcePath(osName, osArch)
      assert(resourcePath.endsWith(s".$ext"), clues(resourcePath))
      assert(
        Option(classOf[LoadLibrary].getClassLoader.getResource(resourcePath)).nonEmpty,
        clues(resourcePath)
      )
    }

  for platform <- Seq("ppc64le-pc-linux", "x86-pc-win32", "riscv64-apple-darwin", "x86_64-freebsd")
  do
    test(s"unsupported platform $platform is rejected") {
      assert(LibSodiumJni.jniLibExtension(platform).isLeft)
    }
}
