package scala.cli.commands.pgp

import caseapp.core.RemainingArgs
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.bouncycastle.openpgp.{PGPPublicKeyRingCollection, PGPUtil}

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import scala.build.Logger
import scala.build.errors.BuildException
import scala.cli.commands.ScalaCommand
import scala.cli.errors.PgpError
import scala.cli.signing.util.BouncycastleSetup
import scala.jdk.CollectionConverters.*

object PgpKeyId extends ScalaCommand[PgpKeyIdOptions] {

  override def hidden                  = true
  override def scalaSpecificationLevel = SpecificationLevel.EXPERIMENTAL
  override def names                   = List(
    List("pgp", "key-id")
  )

  // from https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java/9855338#9855338
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
    BouncycastleSetup.ensureProviderRegistered()

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

  def keyId(key: String, keyPrintablePath: String): Either[BuildException, String] =
    get(key.getBytes(StandardCharsets.UTF_8), fingerprint = false)
      .headOption
      .toRight(new PgpError(s"No public key found in $keyPrintablePath"))

  override def runCommand(options: PgpKeyIdOptions, args: RemainingArgs, logger: Logger): Unit =
    for (arg <- args.all) {
      val path = os.Path(arg, os.pwd)
      logger.debug(s"Reading $path")
      val keyContent = os.read.bytes(path)
      val values     = get(keyContent, options.fingerprint)
      logger.debug(s"Values: $values")
      for (value <- values)
        println(value)
    }
}
