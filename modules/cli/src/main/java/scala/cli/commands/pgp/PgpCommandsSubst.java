package scala.cli.commands.pgp;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import scala.cli.commands.ScalaCommand;
import scala.cli.commands.pgp.ExternalCommand;

@TargetClass(className = "scala.cli.commands.pgp.PgpCommands")
final class PgpCommandsSubst {
  @Substitute
  ScalaCommand<?>[] allScalaCommands() {
    return new ScalaCommand<?>[0];
  }
  @Substitute
  ExternalCommand[] allExternalCommands() {
    return new ExternalCommand[] {
      new PgpCreateExternal(),
      new PgpSignExternal(),
      new PgpVerifyExternal()
    };
  }
}
