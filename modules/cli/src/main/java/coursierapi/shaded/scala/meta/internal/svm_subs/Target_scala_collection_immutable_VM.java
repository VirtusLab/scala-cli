package coursierapi.shaded.scala.meta.internal.svm_subs;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "coursierapi.shaded.scala.collection.immutable.VM", onlyWith = HasReleaseFenceMethod.class)
final class Target_scala_collection_immutable_VM {

    @Substitute
    public static void releaseFence() {
        UnsafeUtils.UNSAFE.storeFence();
    }
}
