package scala.cli.signing.util

// https://stackoverflow.com/questions/28245669/using-bouncy-castle-to-create-public-pgp-key-usable-by-thunderbird

import org.bouncycastle.bcpg.sig.{Features, KeyFlags}
import org.bouncycastle.bcpg.{HashAlgorithmTags, PublicKeyAlgorithmTags, SymmetricKeyAlgorithmTags}
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters
import org.bouncycastle.openpgp.operator.bc.{
  BcPBESecretKeyEncryptorBuilder,
  BcPGPContentSignerBuilder,
  BcPGPDigestCalculatorProvider,
  BcPGPKeyPair
}
import org.bouncycastle.openpgp.{PGPKeyRingGenerator, PGPSignature, PGPSignatureSubpacketGenerator}

import java.math.BigInteger
import java.security.SecureRandom
import java.util.Date

object PgpHelper {
  def generateKeyRingGenerator(
    id: String,
    maybePassword: Option[Array[Char]]
  ): PGPKeyRingGenerator =
    generateKeyRingGenerator(id, maybePassword, 0xc0)

  // Note: s2kcount is a number between 0 and 0xff that controls the number of times to iterate the password hash before use. More
  // iterations are useful against offline attacks, as it takes more time to check each password. The actual number of iterations is
  // rather complex, and also depends on the hash function in use. Refer to Section 3.7.1.3 in rfc4880.txt. Bigger numbers give
  // you more iterations.  As a rough rule of thumb, when using SHA256 as the hashing function, 0x10 gives you about 64
  // iterations, 0x20 about 128, 0x30 about 256 and so on till 0xf0, or about 1 million iterations. The maximum you can go to is
  // 0xff, or about 2 million iterations.  I'll use 0xc0 as a default -- about 130,000 iterations.
  def generateKeyRingGenerator(
    id: String,
    maybePassword: Option[Array[Char]],
    s2kcount: Int
  ): PGPKeyRingGenerator = {
    // This object generates individual key-pairs.
    val kpg = new RSAKeyPairGenerator
    // Boilerplate RSA parameters, no need to change anything
    // except for the RSA key-size (2048). You can use whatever key-size makes sense for you -- 4096, etc.
    kpg.init(
      new RSAKeyGenerationParameters(
        BigInteger.valueOf(0x10001),
        new SecureRandom,
        2048,
        12
      )
    )

    // First create the master (signing) key with the generator.
    val rsakpSign = new BcPGPKeyPair(
      PublicKeyAlgorithmTags.RSA_SIGN,
      kpg.generateKeyPair(),
      new Date
    )
    // Then an encryption subkey.
    val rsakpEnc = new BcPGPKeyPair(
      PublicKeyAlgorithmTags.RSA_ENCRYPT,
      kpg.generateKeyPair(),
      new Date
    )
    // Add a self-signature on the id
    val signHashGen = new PGPSignatureSubpacketGenerator
    // Add signed metadata on the signature.
    // 1) Declare its purpose
    signHashGen.setKeyFlags(false, KeyFlags.SIGN_DATA | KeyFlags.CERTIFY_OTHER)
    // 2) Set preferences for secondary crypto algorithms to use when sending messages to this key.
    signHashGen.setPreferredSymmetricAlgorithms(
      false,
      Array(
        SymmetricKeyAlgorithmTags.AES_256,
        SymmetricKeyAlgorithmTags.AES_192,
        SymmetricKeyAlgorithmTags.AES_128
      )
    )
    signHashGen.setPreferredHashAlgorithms(
      false,
      Array(
        HashAlgorithmTags.SHA256,
        HashAlgorithmTags.SHA1,
        HashAlgorithmTags.SHA384,
        HashAlgorithmTags.SHA512,
        HashAlgorithmTags.SHA224
      )
    )
    // 3) Request senders add additional checksums to the message (useful when verifying unsigned messages.)
    signHashGen.setFeature(false, Features.FEATURE_MODIFICATION_DETECTION)
    // Create a signature on the encryption subkey.
    val encHashGen = new PGPSignatureSubpacketGenerator
    // Add metadata to declare its purpose
    encHashGen.setKeyFlags(
      false,
      KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE
    )
    // Objects used to encrypt the secret key.
    val sha1Calc =
      (new BcPGPDigestCalculatorProvider).get(HashAlgorithmTags.SHA1)
    val sha256Calc =
      (new BcPGPDigestCalculatorProvider).get(HashAlgorithmTags.SHA256)
    // bcpg 1.48 exposes this API that includes s2kcount. Earlier versions use a default of 0x60.
    val secretKeyEncryptor =
      new BcPBESecretKeyEncryptorBuilder(
        maybePassword.fold(SymmetricKeyAlgorithmTags.NULL)(_ => SymmetricKeyAlgorithmTags.AES_256),
        sha256Calc,
        s2kcount
      ).build(maybePassword.orNull)

    // Finally, create the keyring itself. The constructor takes parameters that allow it to generate the self signature.
    val keyRingGen =
      new PGPKeyRingGenerator(
        PGPSignature.POSITIVE_CERTIFICATION,
        rsakpSign,
        id,
        sha1Calc,
        signHashGen.generate(),
        null,
        new BcPGPContentSignerBuilder(
          rsakpSign.getPublicKey.getAlgorithm,
          HashAlgorithmTags.SHA1
        ),
        secretKeyEncryptor
      )
    keyRingGen.addSubKey(rsakpEnc, encHashGen.generate(), null)
    keyRingGen
  }
}
