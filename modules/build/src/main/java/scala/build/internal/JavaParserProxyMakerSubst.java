package scala.build.internal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import java.util.function.Supplier;

import scala.build.internal.JavaParserProxyBinaryJava$;

/**
 * This makes [[JavaParserProxyMaker.get]] provide a [[JavaParserProxyBinary]]
 * rather than a [[JavaParserProxyJvm]], from native launchers.
 *
 * See [[JavaParserProxyMaker]] for more details.
 */
@TargetClass(className = "scala.build.internal.JavaParserProxyMaker")
public final class JavaParserProxyMakerSubst {
  @Substitute
  public JavaParserProxy get(
    Object archiveCache,
    scala.Option<String> javaClassNameVersionOpt,
    scala.build.Logger logger,
    Supplier<String> javaCommand
  ) {
    return JavaParserProxyBinaryJava$.MODULE$.apply(
      archiveCache, logger, javaClassNameVersionOpt, javaCommand);
  }
}
