package scala.cli.publish

import coursier.publish.signing.Signer
import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.security.Security
import java.util.function.Supplier

import scala.build.Logger
import scala.cli.signing.shared.{PasswordOption, Secret}
import scala.cli.signing.util.BouncycastleSigner

class BouncycastleSignerMaker {
  def get(
    passwordOrNull: PasswordOption,
    secretKey: PasswordOption,
    command: Supplier[Array[String]], // unused here, but used in the GraalVM substitution
    logger: Logger                    // unused here, but used in the GraalVM substitution
  ): Signer =
    BouncycastleSigner(
      secretKey.getBytes(),
      Option(passwordOrNull).map(_.get())
    )
  def maybeInit(): Unit =
    Security.addProvider(new BouncyCastleProvider)
}
