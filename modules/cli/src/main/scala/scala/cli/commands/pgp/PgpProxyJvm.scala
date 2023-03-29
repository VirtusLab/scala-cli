package scala.cli.commands.pgp

import caseapp.core.RemainingArgs
import coursier.cache.{Cache, FileCache}
import coursier.util.Task

import java.nio.charset.StandardCharsets

import scala.build.errors.BuildException
import scala.build.{Logger, options => bo}
import scala.cli.errors.PgpError
import scala.cli.signing.commands.{PgpCreate, PgpCreateOptions, PgpKeyId}
import scala.cli.signing.shared.{PasswordOption, Secret}

class PgpProxyJvm extends PgpProxy {
  override def createKey(
    pubKey: String,
    secKey: String,
    mail: String,
    quiet: Boolean,
    passwordOpt: Option[String],
    cache: FileCache[Task],
    logger: Logger,
    javaCommand: () => String,
    signingCliOptions: bo.ScalaSigningCliOptions
  ): Either[BuildException, Int] = {

    val password = passwordOpt.map(password => PasswordOption.Value(Secret(password)))

    PgpCreate.tryRun(
      PgpCreateOptions(
        email = mail,
        password = password,
        pubDest = Some(pubKey),
        secretDest = Some(secKey),
        quiet = quiet
      ),
      RemainingArgs(Seq(), Nil)
    )
    Right(0)
  }

  override def keyId(
    key: String,
    keyPrintablePath: String,
    cache: FileCache[Task],
    logger: Logger,
    javaCommand: () => String,
    signingCliOptions: bo.ScalaSigningCliOptions
  ): Either[BuildException, String] =
    PgpKeyId.get(key.getBytes(StandardCharsets.UTF_8), fingerprint = false)
      .headOption
      .toRight {
        new PgpError(s"No public key found in $keyPrintablePath")
      }
}
