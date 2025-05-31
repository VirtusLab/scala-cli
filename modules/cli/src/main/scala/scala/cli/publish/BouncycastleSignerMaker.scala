package scala.cli.publish

import coursier.publish.signing.Signer
import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.security.Security
import java.util.function.Supplier

import scala.build.Logger
import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.BouncycastleSigner

/** Used for choosing the right BouncyCastleSigner when Scala CLI is run on JVM. <br>
  *
  * See [[scala.cli.internal.BouncycastleSignerMakerSubst BouncycastleSignerMakerSubst]]
  */
class BouncycastleSignerMaker {
  def get(
    forceSigningExternally: java.lang.Boolean,
    passwordOrNull: PasswordOption,
    secretKey: PasswordOption,
    command: Supplier[Array[String]], // unused here, but used in the GraalVM substitution
    logger: Logger                    // unused here, but used in the GraalVM substitution
  ): Signer =
    if (forceSigningExternally)
      BouncycastleExternalSigner(secretKey, passwordOrNull, command.get, logger)
    else
      BouncycastleSigner(secretKey.getBytes(), Option(passwordOrNull).map(_.get()))

  def maybeInit(): Unit =
    Security.addProvider(new BouncyCastleProvider)
}
