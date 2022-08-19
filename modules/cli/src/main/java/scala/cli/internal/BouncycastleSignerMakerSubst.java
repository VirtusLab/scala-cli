package scala.cli.internal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import coursier.publish.signing.Signer;

import java.nio.file.Path;
import java.util.function.Supplier;

import scala.build.Logger;
import scala.cli.publish.BouncycastleExternalSigner$;
import scala.cli.signing.shared.PasswordOption;

@TargetClass(className = "scala.cli.publish.BouncycastleSignerMaker")
public final class BouncycastleSignerMakerSubst {

  @Substitute
  public Signer get(PasswordOption passwordOrNull, PasswordOption secretKey, Supplier<String[]> command, Logger logger) {
    return BouncycastleExternalSigner$.MODULE$.apply(secretKey, passwordOrNull, command.get(), logger);
  }

  @Substitute
  void maybeInit() {
    // do nothing
  }

}
