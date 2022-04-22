package scala.cli.signing.commands

import caseapp.core.RemainingArgs
import caseapp.core.app.Command
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.bouncycastle.openpgp.{PGPPublicKeyRingCollection, PGPUtil}

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import scala.jdk.CollectionConverters._

object PgpKeyId extends Command[PgpKeyIdOptions] {

  override def names = List(
    List("pgp", "key-id")
  )

  // from https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java/9855338#9855338
  private val hexChars = "0123456789abcdef".toCharArray
  private def bytesToHex(bytes: Array[Byte]): String = {
    val hexChars = Array.ofDim[Char](bytes.length * 2)
    for (j <- bytes.indices) {
      val v = bytes(j) & 0xff
      hexChars(j * 2) = hexChars(v >>> 4)
      hexChars(j * 2 + 1) = hexChars(v & 0x0f)
    }
    new String(hexChars)
  }

  def run(options: PgpKeyIdOptions, args: RemainingArgs): Unit =
    for (arg <- args.all) {
      val path       = os.Path(arg, os.pwd)
      val keyContent = os.read.bytes(path)

      val pgpPubRingCollection = new PGPPublicKeyRingCollection(
        PGPUtil.getDecoderStream(new ByteArrayInputStream(keyContent)),
        new JcaKeyFingerprintCalculator
      )

      for (key <- pgpPubRingCollection.getKeyRings.asScala)
        if (options.fingerprint) {
          System.err.println(s"key.getPublicKey.getAlgorithm=${key.getPublicKey.getAlgorithm}")
          System.err.println(s"key.getPublicKey.getBitStrength=${key.getPublicKey.getBitStrength}")
          System.err.println(s"PublicKeyAlgorithmTags.RSA_SIGN=${PublicKeyAlgorithmTags.RSA_SIGN}")
          val fp = key.getPublicKey.getFingerprint
          println(bytesToHex(fp))
        }
        else {
          val keyId = key.getPublicKey.getKeyID
          println(java.util.HexFormat.of().withPrefix("0x").toHexDigits(keyId))
        }
    }
}
