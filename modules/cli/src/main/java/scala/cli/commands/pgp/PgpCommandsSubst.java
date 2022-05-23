package scala.cli.commands.pgp;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import scala.cli.commands.ScalaCommand;
import scala.cli.commands.pgp.ExternalCommand;

@TargetClass(className = "scala.cli.commands.pgp.PgpCommands")
public final class PgpCommandsSubst {
  @Substitute
  public ScalaCommand<?>[] allScalaCommands() {
    return new ScalaCommand<?>[0];
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
