package scala.cli.commands.config

import java.security.SecureRandom

import scala.build.Logger
import scala.cli.commands.pgp.PgpCreate
import scala.cli.signing.Secret
import scala.util.Properties

object ThrowawayPgpSecret {

  private val secretChars =
    (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ Seq('$', '/', '*', '&', '\'', '"', '!', '(',
      ')', '-', '_', '\\', ';', '.', ':', '=', '+', '?', ',', '%')).toVector
  private def secretChars(rng: SecureRandom): Iterator[Char] =
    Iterator.continually {
      val idx = rng.nextInt(secretChars.length)
      secretChars(idx)
    }

  def pgpPassPhrase(): Secret[String] = {
    val random = new SecureRandom
    Secret(secretChars(random).take(32).mkString)
  }
  def pgpSecret(
    mail: String,
    password: Option[Secret[String]],
    logger: Logger
  ): (Secret[String], Secret[String]) = {
    val dir    = os.temp.dir(perms = if (Properties.isWin) null else "rwx------")
    val pubKey = dir / "pub"
    val secKey = dir / "sec"
    try {
      PgpCreate.createKey(
        email = mail,
        password = password,
        publicKeyPath = pubKey,
        secretKeyPath = secKey,
        logger = if logger.verbosity <= 0 then Logger.nop else logger
      )
      (Secret(os.read(pubKey)), Secret(os.read(secKey)))
    }
    finally os.remove.all(dir)
  }

}
