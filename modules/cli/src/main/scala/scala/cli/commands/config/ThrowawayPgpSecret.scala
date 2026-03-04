package scala.cli.commands.config

import coursier.cache.FileCache
import coursier.util.Task

import java.security.SecureRandom

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.{Logger, options as bo}
import scala.cli.commands.pgp.PgpProxyMaker
import scala.cli.commands.shared.{CoursierOptions, SharedJvmOptions}
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
    password: Option[Secret[String]],
    logger: Logger,
    cache: FileCache[Task],
    jvmOptions: SharedJvmOptions,
    coursierOptions: CoursierOptions,
    signingCliOptions: bo.ScalaSigningCliOptions
  ): Either[BuildException, (Secret[String], Secret[String])] = either {

    val dir     = os.temp.dir(perms = if (Properties.isWin) null else "rwx------")
    val pubKey  = dir / "pub"
    val secKey  = dir / "sec"
    val retCode = value {
      (new PgpProxyMaker).get(
        signingCliOptions.forceExternal.getOrElse(false)
      ).createKey(
        pubKey.toString,
        secKey.toString,
        mail,
        logger.verbosity <= 0,
        password.map(_.value),
        cache,
        logger,
        jvmOptions,
        coursierOptions,
        signingCliOptions
      )
    }

    def cleanUp(): Unit =
      os.remove.all(dir)

    if (retCode == 0)
      try (Secret(os.read(pubKey)), Secret(os.read(secKey)))
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
