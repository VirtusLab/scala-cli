package scala.cli.commands.pgp

import caseapp.core.RemainingArgs
import org.bouncycastle.bcpg.ArmoredOutputStream

import java.io.{ByteArrayOutputStream, File}

import scala.build.Logger
import scala.cli.commands.ScalaCommand
import scala.cli.signing.shared.Secret
import scala.cli.signing.util.{BouncycastleSetup, PgpHelper}

object PgpCreate extends ScalaCommand[PgpCreateOptions] {

  override def hidden                  = true
  override def scalaSpecificationLevel = SpecificationLevel.EXPERIMENTAL
  override def names                   = List(
    List("pgp", "create")
  )

  private def printable(p: os.Path): String =
    if (p.startsWith(os.pwd)) p.relativeTo(os.pwd).segments.mkString(File.separator)
    else p.toString

  override def runCommand(options: PgpCreateOptions, args: RemainingArgs, logger: Logger): Unit =
    createKey(
      email = options.email,
      password = options.password.map(_.get()),
      publicKeyPath = options.publicKeyPath,
      secretKeyPath = options.secretKeyPath,
      logger = logger
    )

  def createKey(
    email: String,
    password: Option[Secret[String]],
    publicKeyPath: os.Path,
    secretKeyPath: os.Path,
    logger: Logger
  ): Unit = {
    BouncycastleSetup.ensureProviderRegistered()

    val maybePassword = password.map(_.value.toCharArray)
    val keyRingGen    = PgpHelper.generateKeyRingGenerator(email, maybePassword)
    val pubKeyRing    = keyRingGen.generatePublicKeyRing()

    val pubKeyContent = {
      val baos = new ByteArrayOutputStream
      val out  = new ArmoredOutputStream(baos)
      pubKeyRing.encode(out)
      out.close()
      baos.toByteArray
    }
    val secretKeyContent = {
      val baos   = new ByteArrayOutputStream
      val skr    = keyRingGen.generateSecretKeyRing()
      val secout = new ArmoredOutputStream(baos)
      skr.encode(secout)
      secout.close()
      baos.toByteArray
    }

    os.write(publicKeyPath, pubKeyContent)
    val keyId    = pubKeyRing.getPublicKey.getKeyID
    val keyIdStr = java.util.HexFormat.of().withPrefix("0x").toHexDigits(keyId)
    logger.message(s"Wrote public key $keyIdStr to ${printable(publicKeyPath)}")
    os.write(secretKeyPath, secretKeyContent)
    logger.message(s"Wrote secret key to ${printable(secretKeyPath)}")
  }
}
