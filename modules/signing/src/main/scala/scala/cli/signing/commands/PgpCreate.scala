package scala.cli.signing.commands

import caseapp.core.RemainingArgs
import caseapp.core.app.Command
import org.bouncycastle.bcpg.ArmoredOutputStream

import java.io.{BufferedOutputStream, ByteArrayOutputStream, File}
import java.util.{Base64, HexFormat}

import scala.cli.signing.util.PgpHelper

object PgpCreate extends Command[PgpCreateOptions] {

  override def names = List(
    List("pgp", "create")
  )

  private def printable(p: os.Path): String =
    if (p.startsWith(os.pwd)) p.relativeTo(os.pwd).segments.mkString(File.separator)
    else p.toString

  def run(options: PgpCreateOptions, args: RemainingArgs): Unit =
    tryRun(options, args)

  def tryRun(options: PgpCreateOptions, args: RemainingArgs): Unit = {

    val maybePassword = options.password.map(_.get().value.toCharArray)
    val keyRingGen    = PgpHelper.generateKeyRingGenerator(options.email, maybePassword)
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
      val secout = new BufferedOutputStream(baos)
      skr.encode(secout)
      secout.close()
      baos.toByteArray
    }

    val publicKeyPath = options.publicKeyPath
    val secretKeyPath = options.secretKeyPath

    os.write(publicKeyPath, pubKeyContent)
    if (options.verbosity >= 0) {
      val keyId    = pubKeyRing.getPublicKey.getKeyID
      val keyIdStr = java.util.HexFormat.of().withPrefix("0x").toHexDigits(keyId)
      System.err.println(s"Wrote public key $keyIdStr to ${printable(publicKeyPath)}")
    }
    os.write(secretKeyPath, secretKeyContent)
    if (options.verbosity >= 0)
      System.err.println(s"Wrote secret key to ${printable(secretKeyPath)}")
  }
}
