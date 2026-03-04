package scala.cli.commands.pgp

import caseapp.core.RemainingArgs
import coursier.cache.FileCache
import coursier.util.Task

import java.nio.charset.StandardCharsets

import scala.build.errors.BuildException
import scala.build.{Logger, options as bo}
import scala.cli.commands.shared.{CoursierOptions, SharedJvmOptions}
import scala.cli.errors.PgpError
import scala.cli.signing.commands.{PgpCreate, PgpCreateOptions, PgpKeyId}
import scala.cli.signing.shared.{PasswordOption, Secret}

/** A proxy running the PGP operations using scala-cli-singing as a dependency. This construct is
  * not used when PGP commands are evoked from CLI (see [[PgpCommandsSubst]] and [[PgpCommands]]),
  * but rather when PGP operations are used internally. <br>
  *
  * This is the 'JVM' counterpart of [[PgpProxy]]
  */
class PgpProxyJvm extends PgpProxy {
  override def createKey(
    pubKey: String,
    secKey: String,
    mail: String,
    quiet: Boolean,
    passwordOpt: Option[String],
    cache: FileCache[Task],
    logger: Logger,
    jvmOptions: SharedJvmOptions,
    coursierOptions: CoursierOptions,
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
    jvmOptions: SharedJvmOptions,
    coursierOptions: CoursierOptions,
    signingCliOptions: bo.ScalaSigningCliOptions
  ): Either[BuildException, String] =
    PgpKeyId.get(key.getBytes(StandardCharsets.UTF_8), fingerprint = false)
      .headOption
      .toRight {
        new PgpError(s"No public key found in $keyPrintablePath")
      }
}
