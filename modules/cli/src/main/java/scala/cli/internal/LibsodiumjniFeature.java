package scala.cli.internal;

import com.oracle.svm.core.jdk.NativeLibrarySupport;
import com.oracle.svm.core.jdk.PlatformNativeLibrarySupport;
import com.oracle.svm.hosted.FeatureImpl;
import com.oracle.svm.hosted.c.NativeLibraries;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.Platform;

// registered via --features in native-image.properties
public class LibsodiumjniFeature implements Feature {

    @Override
    public boolean isInConfiguration(IsInConfigurationAccess access) {
        boolean isStaticLauncher = Boolean.getBoolean("scala-cli.static-launcher");
        return Platform.includedIn(Platform.WINDOWS.class) ||
            (Platform.includedIn(Platform.LINUX.class) && isStaticLauncher);
    }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        NativeLibrarySupport.singleton().preregisterUninitializedBuiltinLibrary("sodiumjni");
        PlatformNativeLibrarySupport.singleton().addBuiltinPkgNativePrefix("libsodiumjni_");
        NativeLibraries nativeLibraries = ((FeatureImpl.BeforeAnalysisAccessImpl) access).getNativeLibraries();
        nativeLibraries.addStaticNonJniLibrary("sodium");
        nativeLibraries.addStaticJniLibrary("sodiumjni");
    }
}
