package scala.cli.commands.pgp

import caseapp.core.RemainingArgs
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.bouncycastle.openpgp.{PGPPublicKeyRingCollection, PGPUtil}

import java.io.{ByteArrayInputStream, InputStream}

import scala.build.Logger
import scala.cli.commands.ScalaCommand
import scala.cli.signing.util.{BouncycastleSetup, BouncycastleSigner}

object PgpVerify extends ScalaCommand[PgpVerifyOptions] {

  override def hidden                  = true
  override def scalaSpecificationLevel = SpecificationLevel.EXPERIMENTAL
  override def names                   = List(
    List("pgp", "verify")
  )

  override def runCommand(options: PgpVerifyOptions, args: RemainingArgs, logger: Logger): Unit = {
    BouncycastleSetup.ensureProviderRegistered()

    val keyContent = os.read.bytes(options.keyPath)

    // originally based on https://github.com/bcgit/bc-java/blob/58fe01df5bd8f839a6f474c16c6a3b7448b0f472/pg/src/main/java/org/bouncycastle/openpgp/examples/DetachedSignatureProcessor.java

    val pgpPubRingCollection = new PGPPublicKeyRingCollection(
      PGPUtil.getDecoderStream(new ByteArrayInputStream(keyContent)),
      new JcaKeyFingerprintCalculator
    )

    val invalidPaths = args.all.filter(!_.endsWith(".asc"))
    if (invalidPaths.nonEmpty) {
      logger.error(s"Invalid signature paths: ${invalidPaths.mkString(", ")}")
      sys.exit(1)
    }

    val results =
      for (path0 <- args.all) yield {
        val filePath         = os.Path(path0.stripSuffix(".asc"), os.pwd)
        val signatureContent = os.read.bytes(os.Path(path0, os.pwd))

        val sig =
          BouncycastleSigner.readSignature(new ByteArrayInputStream(signatureContent)) match {
            case Left(err) =>
              logger.error(err)
              sys.exit(1)
            case Right(sig0) => sig0
          }
        val key = pgpPubRingCollection.getPublicKey(sig.getKeyID)

        var is: InputStream = null
        val verified        =
          try {
            is = os.read.inputStream(filePath)
            BouncycastleSigner.verifySignature(sig, key, is)
          }
          finally
            if (is != null)
              is.close()

        path0 -> verified
      }

    for ((path, verified) <- results) {
      val msg =
        if (verified) "valid signature"
        else "invalid signature"
      logger.message(s"$path: $msg")
    }

    if (results.exists(!_._2))
      sys.exit(1)
  }
}
