package scala.cli.signing.util

import coursier.publish.Content
import coursier.publish.signing.Signer
import org.bouncycastle.bcpg.sig.KeyFlags
import org.bouncycastle.bcpg.{
  ArmoredOutputStream,
  BCPGOutputStream,
  CompressionAlgorithmTags,
  HashAlgorithmTags
}
import org.bouncycastle.openpgp.{Util => _, _}
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory
import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator
import org.bouncycastle.openpgp.operator.jcajce.{
  JcaKeyFingerprintCalculator,
  JcaPGPContentSignerBuilder,
  JcaPGPContentVerifierBuilderProvider,
  JcePBESecretKeyDecryptorBuilder
}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import scala.cli.signing.shared.Secret
import scala.jdk.CollectionConverters._

final case class BouncycastleSigner(
  pgpSecretKey: PGPSecretKey,
  passwordOpt: Option[Secret[String]]
) extends Signer {
  def sign(content: Content): Either[String, String] =
    sign { f =>
      val b = content.content()
      f(b, 0, b.length)
    }
  def sign(withContent: ((Array[Byte], Int, Int) => Unit) => Unit): Either[String, String] = {

    // originally adapted from https://github.com/jordanbaucke/PGP-Sign-and-Encrypt/blob/472d8932df303d6861ec494a3e942ea268eaf25f/src/SignAndEncrypt.java#L144-L199

    val encOut = new ByteArrayOutputStream
    val out    = new ArmoredOutputStream(encOut)

    val pgpPrivKey =
      pgpSecretKey.extractPrivateKey(
        new JcePBESecretKeyDecryptorBuilder()
          .setProvider("BC")
          .build(passwordOpt.map(_.value.toCharArray).getOrElse(Array.empty))
      )

    val sGen = new PGPSignatureGenerator(
      new JcaPGPContentSignerBuilder(
        pgpSecretKey.getPublicKey
          .getAlgorithm,
        HashAlgorithmTags.SHA1
      ).setProvider("BC")
    )

    sGen.init(PGPSignature.BINARY_DOCUMENT, pgpPrivKey)

    val it = pgpSecretKey.getPublicKey.getUserIDs
    if (it.hasNext()) {
      val spGen = new PGPSignatureSubpacketGenerator
      spGen.setSignerUserID(false, it.next().asInstanceOf[String])
      sGen.setHashedSubpackets(spGen.generate())
    }

    val comData = new PGPCompressedDataGenerator(
      CompressionAlgorithmTags.ZLIB
    )

    val bOut = new BCPGOutputStream(comData.open(out))

    withContent { (b, off, len) =>
      sGen.update(b, off, len)
    }

    sGen.generate().encode(bOut)

    comData.close()

    out.close()

    Right(encOut.toString())
  }
}

object BouncycastleSigner {

  // many things here originally adapted from http://sloanseaman.com/wordpress/2012/05/13/revisited-pgp-encryptiondecryption-in-java/

  private val MASTER_KEY_CERTIFICATION_TYPES = Seq(
    PGPSignature.POSITIVE_CERTIFICATION,
    PGPSignature.CASUAL_CERTIFICATION,
    PGPSignature.NO_CERTIFICATION,
    PGPSignature.DEFAULT_CERTIFICATION
  )
  private def KEY_FLAGS = 27

  private def hasKeyFlags(encKey: PGPPublicKey, keyUsage: Int): Boolean =
    if (encKey.isMasterKey)
      MASTER_KEY_CERTIFICATION_TYPES
        .iterator
        .flatMap(encKey.getSignaturesOfType(_).asScala)
        .forall { sig =>
          isMatchingUsage(sig.asInstanceOf[PGPSignature], keyUsage)
        }
    else
      encKey.getSignaturesOfType(PGPSignature.SUBKEY_BINDING).asScala
        .forall { sig =>
          isMatchingUsage(sig.asInstanceOf[PGPSignature], keyUsage)
        }

  private def isMatchingUsage(sig: PGPSignature, keyUsage: Int): Boolean =
    !sig.hasSubpackets || {
      val sv = sig.getHashedSubPackets
      !sv.hasSubpacket(KEY_FLAGS) || (sv.getKeyFlags & keyUsage) != 0
    }

  private def fingerprintCalculator: KeyFingerPrintCalculator =
    new JcaKeyFingerprintCalculator

  def readSecretKey(in: InputStream): PGPSecretKey = {
    val keyRingCollection = new PGPSecretKeyRingCollection(
      PGPUtil.getDecoderStream(in),
      fingerprintCalculator
    )
    //
    // We just loop through the collection till we find a key suitable for signing.
    // In the real world you would probably want to be a bit smarter about this.
    //
    var secretKey: PGPSecretKey = null
    val rIt                     = keyRingCollection.getKeyRings
    while (secretKey == null && rIt.hasNext) {
      val keyRing = rIt.next().asInstanceOf[PGPSecretKeyRing]
      val kIt     = keyRing.getSecretKeys
      while (secretKey == null && kIt.hasNext) {
        val key = kIt.next().asInstanceOf[PGPSecretKey]
        if (key.isSigningKey)
          secretKey = key
      }
    }
    // Validate secret key
    if (secretKey == null)
      throw new IllegalArgumentException("Can't find private key in the key ring.")
    if (!secretKey.isSigningKey)
      throw new IllegalArgumentException("Private key does not allow signing.")
    if (secretKey.getPublicKey.isRevoked)
      throw new IllegalArgumentException("Private key has been revoked.")
    if (!hasKeyFlags(secretKey.getPublicKey, KeyFlags.SIGN_DATA))
      throw new IllegalArgumentException("Key cannot be used for signing.")
    secretKey
  }

  def readSignature(in: InputStream): Either[String, PGPSignature] = {

    val factory = new JcaPGPObjectFactory(PGPUtil.getDecoderStream(in))

    factory.nextObject() match {
      case data: PGPCompressedData =>
        val factory0 = new JcaPGPObjectFactory(data.getDataStream())
        factory0.nextObject() match {
          case l: PGPSignatureList => Right(l.get(0))
          case other =>
            Left(s"Unrecognized PGP object type: $other")
        }
      case sigList0: PGPSignatureList =>
        Right(sigList0.get(0))
      case obj =>
        Left(s"Unrecognized PGP object type: $obj")
    }
  }

  def verifySignature(sig: PGPSignature, key: PGPPublicKey, content: InputStream): Boolean = {

    sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), key)

    val buf  = Array.ofDim[Byte](16 * 1024)
    var read = -1
    while ({
      read = content.read(buf)
      read >= 0
    })
      if (read > 0)
        sig.update(buf, 0, read)

    sig.verify()
  }

  def apply(
    secretKey: Secret[Array[Byte]],
    passwordOpt: Option[Secret[String]]
  ): BouncycastleSigner = {
    val is         = new ByteArrayInputStream(Util.maybeDecodeBase64(secretKey.value))
    val secretKey0 = readSecretKey(is)
    BouncycastleSigner(secretKey0, passwordOpt)
  }
}
