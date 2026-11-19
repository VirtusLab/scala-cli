package scala.cli.commands.github

import munit.FunSuite

final class LibSodiumJniTests extends FunSuite {

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
