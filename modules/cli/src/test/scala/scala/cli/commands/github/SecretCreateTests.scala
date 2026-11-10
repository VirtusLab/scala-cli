package scala.cli.commands.github

import libsodiumjni.internal.LoadLibrary
import libsodiumjni.{Sodium, SodiumException}
import munit.FunSuite

import java.nio.charset.StandardCharsets
import java.util.Base64

import scala.util.Properties

final class SecretCreateTests extends FunSuite {

  private val secret = "1234".getBytes(StandardCharsets.UTF_8)

  private def publicKey(bytes: Array[Byte]) =
    GitHubApi.PublicKey("the-key-id", Base64.getEncoder.encodeToString(bytes))

  // On Windows, the libsodiumjni library bundled in its JAR needs a libsodium.dll
  private def initSodium(): Unit = {
    assume(!Properties.isWin, "libsodium.dll isn't available in unit tests on Windows")
    LoadLibrary.initializeFromResources()
    Sodium.init()
  }

  private def assertInvalidKeyError(res: Either[Throwable, Array[Byte]]): Unit =
    res match {
      case Left(err) =>
        assert(
          err.getMessage.contains("Invalid GitHub public key the-key-id for repository foo/bar"),
          clues(err.getMessage)
        )
      case Right(_) => fail("Expected an error")
    }

  test("a public key that isn't valid base64 is reported as an error") {
    val pubKey = GitHubApi.PublicKey("the-key-id", "not base64!")
    assertInvalidKeyError(SecretCreate.encryptSecret(secret, pubKey, "foo", "bar"))
  }

  test("a public key with a wrong length is reported as an error") {
    initSodium()
    val pubKey = publicKey(Array.fill[Byte](31)(1))
    assertInvalidKeyError(SecretCreate.encryptSecret(secret, pubKey, "foo", "bar"))
  }

  test("libsodium failures are reported as an error") {
    val pubKey = publicKey(Array.fill[Byte](32)(1))
    val res    = SecretCreate.encryptSecret(
      secret,
      pubKey,
      "foo",
      "bar",
      seal = (_, _) => throw new SodiumException("Failed to seal message")
    )
    assertInvalidKeyError(res)
  }

  test("a valid public key encrypts the secret") {
    initSodium()
    val keyPair   = Sodium.keyPair()
    val encrypted = SecretCreate.encryptSecret(secret, publicKey(keyPair.getPubKey), "foo", "bar")
      .fold(e => fail(s"Unexpected error: ${e.getMessage}"), identity)
    val decrypted = Sodium.sealOpen(encrypted, keyPair.getPubKey, keyPair.getSecKey)
    assertEquals(new String(decrypted, StandardCharsets.UTF_8), "1234")
  }
}
