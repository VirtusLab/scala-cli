package scala.cli.internal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import scala.cli.commands.pgp.PgpProxy;

@TargetClass(className = "scala.cli.commands.pgp.PgpProxyMaker")
public final class PgpProxyMakerSubst {
  @Substitute
  public PgpProxy get() {
    return new PgpProxy();
  }
}
