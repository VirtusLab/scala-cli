package scala.cli.internal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import scala.cli.commands.pgp.PgpProxy;

@TargetClass(className = "scala.cli.commands.pgp.PgpProxyMaker")
final class PgpProxyMakerSubst {
  @Substitute
  PgpProxy get() {
    return new PgpProxy();
  }
}
