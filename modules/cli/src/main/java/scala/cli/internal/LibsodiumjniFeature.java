package scala.cli.internal;

import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.jdk.NativeLibrarySupport;
import com.oracle.svm.core.jdk.PlatformNativeLibrarySupport;
import com.oracle.svm.hosted.FeatureImpl;
import com.oracle.svm.hosted.c.NativeLibraries;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import java.util.Locale;

@AutomaticFeature
@Platforms({Platform.LINUX.class, Platform.WINDOWS.class})
public class LibsodiumjniFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows");
        boolean isStaticLauncher = Boolean.getBoolean("scala-cli.static-launcher");
        if (!isWindows && !isStaticLauncher) {
            System.err.println("Actually disabling LibsodiumjniFeature (not Windows nor static launcher)");
            return;
        }
        NativeLibrarySupport.singleton().preregisterUninitializedBuiltinLibrary("sodiumjni");
        PlatformNativeLibrarySupport.singleton().addBuiltinPkgNativePrefix("libsodiumjni_");
        NativeLibraries nativeLibraries = ((FeatureImpl.BeforeAnalysisAccessImpl) access).getNativeLibraries();
        nativeLibraries.addStaticNonJniLibrary("sodium");
        nativeLibraries.addStaticJniLibrary("sodiumjni");
    }
}
