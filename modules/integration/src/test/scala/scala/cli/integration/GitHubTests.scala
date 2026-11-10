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
        "--power",
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
      val output = readFromArray(res.out.bytes)(using GitHubTests.encryptedSecretCodec)

      expect(output.key_id == keyId)

      val decrypted      = Sodium.sealOpen(output.encrypted, keyPair.getPubKey, keyPair.getSecKey)
      val decryptedValue = new String(decrypted, StandardCharsets.UTF_8)
      expect(decryptedValue == value)
    }
  }

  override def munitFlakyOK: Boolean = TestUtil.isCI

  def createSecret(): Unit = {
    try
      createSecretTest()
    catch {
      case e: UnsatisfiedLinkError if e.getMessage.contains("libsodium") =>
        fail("libsodium, couldn't be loaded")
    }

  }

  // tagged as sanity so that it runs on the macOS CI jobs too
  test("create secret".tag(ScalaCliSuite.sanity))(TestUtil.retryOnCi()(createSecret()))

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

  // Keep in sync with LibSodiumJni.libsodiumArchive in the cli module (not a dependency of
  // the integration tests): since libsodiumjni 1.0.0, libsodium is statically linked in its
  // Linux and macOS native libraries, while on Windows, libsodiumjni.dll needs libsodium.dll,
  // which we take from the official libsodium MSVC archive.
  private def libsodiumDllPathOpt: Option[os.Path] =
    Option.when(Properties.isWin) {
      val msvcArch = sys.props("os.arch").toLowerCase(Locale.ROOT) match {
        case "aarch64" | "arm64" => "ARM64"
        case _                   => "x64"
      }
      val url =
        s"https://download.libsodium.org/libsodium/releases/libsodium-${Constants.libsodiumVersion}-stable-msvc.zip"
      val archiveCache = ArchiveCache()
      val dir          = archiveCache.get(Artifact(url)).unsafeRun()(using archiveCache.cache.ec)
        .fold(e => throw new Exception(e), os.Path(_, os.pwd))
      dir / "libsodium" / msvcArch / "Release" / "v143" / "dynamic" / "libsodium.dll"
    }

  private def initSodium(): Unit = {
    libsodiumDllPathOpt.foreach(lib => System.load(lib.toString))
    LoadLibrary.initializeFromResources()
    Sodium.init()
  }

}
