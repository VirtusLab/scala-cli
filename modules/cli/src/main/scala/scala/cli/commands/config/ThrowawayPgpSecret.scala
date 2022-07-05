package scala.cli.commands.config

import coursier.cache.Cache
import coursier.util.Task

import java.security.SecureRandom

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.cli.commands.pgp.PgpProxyMaker
import scala.cli.errors.PgpError
import scala.cli.signing.shared.Secret
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
    password: Secret[String],
    logger: Logger,
    cache: Cache[Task]
  ): Either[BuildException, (Secret[String], Secret[Array[Byte]])] = either {

    val dir    = os.temp.dir(perms = if (Properties.isWin) null else "rwx------")
    val pubKey = dir / "pub"
    val secKey = dir / "sec"
    val retCode = value {
      (new PgpProxyMaker).get().createKey(
        pubKey.toString,
        secKey.toString,
        mail,
        logger.verbosity <= 0,
        password.value,
        cache,
        logger
      )
    }

    def cleanUp(): Unit =
      os.remove.all(dir)

    if (retCode == 0)
      try (Secret(os.read(pubKey)), Secret(os.read.bytes(secKey)))
      finally cleanUp()
    else {
      cleanUp()
      value {
        Left {
          new PgpError(
            s"Failed to create PGP key pair (see messages above, scala-cli-signing return code: $retCode)"
          )
        }
      }
    }
  }

}
