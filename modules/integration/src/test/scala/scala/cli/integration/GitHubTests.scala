package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import coursier.cache.ArchiveCache
import coursier.util.Artifact
import libsodiumjni.Sodium
import libsodiumjni.internal.LoadLibrary

import java.nio.charset.StandardCharsets
import java.util.{Base64, Locale}

import scala.util.Properties

class GitHubTests extends ScalaCliSuite {

  override def group: ScalaCliSuite.TestGroup = ScalaCliSuite.TestGroup.First

  def createSecretTest(): Unit = {
    GitHubTests.initSodium()

    val keyId   = "the-key-id"
    val keyPair = Sodium.keyPair()
    val value   = "1234"
    TestInputs.empty.fromRoot { root =>
      val pubKey = GitHubTests.PublicKey(keyId, Base64.getEncoder.encodeToString(keyPair.getPubKey))
      os.write(root / "pub-key.json", writeToArray(pubKey))

      val res = os.proc(
        TestUtil.cli,
        "github",
        "secret",
        "create",
        "--repo",
        "foo/foo",
        s"FOO=value:$value",
        "--dummy",
        "--print-request",
        "--pub-key",
        root / "pub-key.json"
      )
        .call(cwd = root)
      val output = readFromArray(res.out.bytes)(GitHubTests.encryptedSecretCodec)

      expect(output.key_id == keyId)

      val decrypted      = Sodium.sealOpen(output.encrypted, keyPair.getPubKey, keyPair.getSecKey)
      val decryptedValue = new String(decrypted, StandardCharsets.UTF_8)
      expect(decryptedValue == value)
    }
  }

  // currently having issues loading libsodium from the static launcher
  // that launcher is mainly meant to be used on CIs or from docker, missing
  // that feature shouldn't be a big deal there
  if (TestUtil.cliKind != "native-static")
    test("create secret") {
      createSecretTest()
    }

}

object GitHubTests {

  final case class PublicKey(
    key_id: String,
    key: String
  )

  implicit val publicKeyCodec: JsonValueCodec[PublicKey] =
    JsonCodecMaker.make

  final case class EncryptedSecret(
    encrypted_value: String,
    key_id: String
  ) {
    def encrypted: Array[Byte] =
      Base64.getDecoder.decode(encrypted_value)
  }

  implicit val encryptedSecretCodec: JsonValueCodec[EncryptedSecret] =
    JsonCodecMaker.make

  private def libsodiumVersion = Constants.libsodiumVersion

  // Warning: somehow also in settings.sc in the build, and in FetchExternalBinary
  lazy val condaPlatform: String = {
    val mambaOs =
      if (Properties.isWin) "win"
      else if (Properties.isMac) "osx"
      else if (Properties.isLinux) "linux"
      else sys.error(s"Unsupported mamba OS: ${sys.props("os.name")}")
    val arch = sys.props("os.arch").toLowerCase(Locale.ROOT)
    val mambaArch = arch match {
      case "x86_64" | "amd64"  => "64"
      case "arm64" | "aarch64" => "arm64"
      case "ppc64le"           => "ppc64le"
      case _ =>
        sys.error(s"Unsupported mamba architecture: $arch")
    }
    s"$mambaOs-$mambaArch"
  }

  private def archiveUrlAndPath() = {
    val suffix = condaPlatform match {
      case "linux-64"      => "-h36c2ea0_1"
      case "linux-aarch64" => "-hb9de7d4_1"
      case "osx-64"        => "-hbcb3906_1"
      case "osx-arm64"     => "-h27ca646_1"
      case "win-64"        => "-h62dcd97_1"
      case other           => sys.error(s"Unrecognized conda platform $other")
    }
    val relPath = condaPlatform match {
      case "linux-64"      => os.rel / "lib" / "libsodium.so"
      case "linux-aarch64" => os.rel / "lib" / "libsodium.so"
      case "osx-64"        => os.rel / "lib" / "libsodium.dylib"
      case "osx-arm64"     => os.rel / "lib" / "libsodium.dylib"
      case "win-64"        => os.rel / "Library" / "bin" / "libsodium.dll"
      case other           => sys.error(s"Unrecognized conda platform $other")
    }
    (
      s"https://anaconda.org/conda-forge/libsodium/$libsodiumVersion/download/$condaPlatform/libsodium-$libsodiumVersion$suffix.tar.bz2",
      relPath
    )
  }

  private def initSodium(): Unit = {
    val (url, relPath) = archiveUrlAndPath()
    val archiveCache   = ArchiveCache()
    val dir = archiveCache.get(Artifact(url)).unsafeRun()(archiveCache.cache.ec)
      .fold(e => throw new Exception(e), os.Path(_, os.pwd))
    val lib = dir / relPath
    System.load(lib.toString)

    LoadLibrary.initializeFromResources()
    Sodium.init()
  }

}
