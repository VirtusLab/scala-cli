package coursierapi.shaded.scala.meta.internal.svm_subs;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "coursierapi.shaded.scala.runtime.Statics", onlyWith = HasReleaseFenceMethod.class)
final class Target_scala_runtime_Statics {

    @Substitute
    public static void releaseFence() {
        UnsafeUtils.UNSAFE.storeFence();
    }
}
