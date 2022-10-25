package scala.cli.commands.pgp;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import scala.cli.commands.pgp.ExternalCommand;
import scala.cli.commands.pgp.PgpCommand;

@TargetClass(className = "scala.cli.commands.pgp.PgpCommands")
public final class PgpCommandsSubst {
  @Substitute
  public PgpCommand<?>[] allScalaCommands() {
    return new PgpCommand<?>[0];
  }
  @Substitute
  public ExternalCommand[] allExternalCommands() {
    return new ExternalCommand[] {
      new PgpCreateExternal(),
      new PgpKeyIdExternal(),
      new PgpSignExternal(),
      new PgpVerifyExternal()
    };
  }
}
