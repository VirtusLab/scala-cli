package scala.cli.commands.pgp

import caseapp.core.RemainingArgs

import java.nio.charset.StandardCharsets

import scala.build.errors.BuildException
import scala.cli.errors.PgpError
import scala.cli.signing.shared.{PasswordOption, Secret}
import scala.cli.signing.util.BouncycastleSetup

/** Runs PGP operations used internally by Scala CLI (rather than evoked directly from the command
  * line via the `pgp` sub-commands).
  */
object PgpProxy {
  def createKey(
    pubKey: String,
    secKey: String,
    mail: String,
    quiet: Boolean,
    passwordOpt: Option[String]
  ): Unit = {
    BouncycastleSetup.ensureProviderRegistered()
    PgpCreate.tryRun(
      PgpCreateOptions(
        email = mail,
        password = passwordOpt.map(password => PasswordOption.Value(Secret(password))),
        pubDest = Some(pubKey),
        secretDest = Some(secKey),
        quiet = quiet
      ),
      RemainingArgs(Seq(), Nil)
    )
  }

  def keyId(key: String, keyPrintablePath: String): Either[BuildException, String] = {
    BouncycastleSetup.ensureProviderRegistered()
    PgpKeyId.get(key.getBytes(StandardCharsets.UTF_8), fingerprint = false)
      .headOption
      .toRight(new PgpError(s"No public key found in $keyPrintablePath"))
  }
}
