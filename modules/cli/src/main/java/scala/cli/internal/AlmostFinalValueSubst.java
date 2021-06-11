package scala.cli.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;

@TargetClass(className = "scala.reflect.internal.util.AlmostFinalValue")
@Substitute
@Platforms({Platform.LINUX.class, Platform.DARWIN.class, Platform.WINDOWS.class})
final class AlmostFinalValueSubst {

  @Substitute
  final MethodHandle invoker;

  @Substitute
  AlmostFinalValueSubst() {
    MethodType mt = MethodType.methodType(boolean.class);
    try {
      invoker = MethodHandles.publicLookup().findStatic(AlmostFinalValueHelper.class, "isFalse", mt);
    } catch (NoSuchMethodException | IllegalAccessException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Substitute
  void toggleOnAndDeoptimize() {
  }
}