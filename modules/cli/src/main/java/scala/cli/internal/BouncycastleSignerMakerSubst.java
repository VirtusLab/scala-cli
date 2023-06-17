package scala.cli.internal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import coursier.publish.signing.Signer;

import java.nio.file.Path;
import java.util.function.Supplier;

import scala.build.Logger;
import scala.cli.publish.BouncycastleExternalSigner$;
import scala.cli.signing.shared.PasswordOption;

/** Used for choosing the right BouncyCastleSigner when Scala CLI is run as a native image.
 *  This class is used to substitute scala.cli.commands.pgp.PgpProxyMaker.
 *  This decouples Scala CLI native image from BouncyCastle used by scala-cli-signing.
 */
@TargetClass(className = "scala.cli.publish.BouncycastleSignerMaker")
public final class BouncycastleSignerMakerSubst {

  @Substitute
  public Signer get(
    Boolean forceSigningExternally,
    PasswordOption passwordOrNull,
    PasswordOption secretKey,
    Supplier<String[]> command,
    Logger logger
  ) {
    return BouncycastleExternalSigner$.MODULE$.apply(secretKey, passwordOrNull, command.get(), logger);
  }

  @Substitute
  void maybeInit() {
    // do nothing
  }

}
