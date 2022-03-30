package scala.cli.publish

import coursier.publish.signing.Signer
import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.nio.file.Path
import java.security.Security
import java.util.function.Supplier

import scala.build.Logger
import scala.cli.signing.shared.{PasswordOption, Secret}
import scala.cli.signing.util.BouncycastleSigner

class BouncycastleSignerMaker {
  def get(
    passwordOrNull: PasswordOption,
    secretKey: Path,
    launcher: Supplier[Path], // unused here, but used in the GraalVM substitution
    logger: Logger            // unused here, but used in the GraalVM substitution
  ): Signer =
    BouncycastleSigner(
      os.Path(secretKey, os.pwd),
      Option(passwordOrNull).fold(Secret(""))(_.get())
    )
  def maybeInit(): Unit =
    Security.addProvider(new BouncyCastleProvider)
}
