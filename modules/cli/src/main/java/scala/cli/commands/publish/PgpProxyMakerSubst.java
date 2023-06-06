package scala.cli.internal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import scala.cli.commands.pgp.PgpProxy;

/** Used for choosing the right PGP proxy implementation when Scala CLI is run as a native image.
 *  This class is used to substitute scala.cli.commands.pgp.PgpProxyMaker.
 *  This decouples Scala CLI native image from BouncyCastle used by scala-cli-signing.
 */
@TargetClass(className = "scala.cli.commands.pgp.PgpProxyMaker")
public final class PgpProxyMakerSubst {
  @Substitute
  public PgpProxy get() {
    return new PgpProxy();
  }
}
