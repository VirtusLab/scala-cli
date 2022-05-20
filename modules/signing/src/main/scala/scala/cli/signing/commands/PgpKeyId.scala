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

  def get(keyContent: Array[Byte], fingerprint: Boolean): Seq[String] = {

    val pgpPubRingCollection = new PGPPublicKeyRingCollection(
      PGPUtil.getDecoderStream(new ByteArrayInputStream(keyContent)),
      new JcaKeyFingerprintCalculator
    )

    pgpPubRingCollection.getKeyRings.asScala.toVector.map { key =>
      if (fingerprint) {
        val fp = key.getPublicKey.getFingerprint
        bytesToHex(fp)
      }
      else {
        val keyId = key.getPublicKey.getKeyID
        java.util.HexFormat.of().withPrefix("0x").toHexDigits(keyId)
      }
    }
  }

  def run(options: PgpKeyIdOptions, args: RemainingArgs): Unit =
    for (arg <- args.all) {
      val path = os.Path(arg, os.pwd)
      if (options.verbosity >= 2)
        System.err.println(s"Reading $path")
      val keyContent = os.read.bytes(path)
      val values     = get(keyContent, options.fingerprint)
      if (options.verbosity >= 2)
        System.err.println(s"Values: $values")
      for (value <- values)
        println(value)
    }
}
