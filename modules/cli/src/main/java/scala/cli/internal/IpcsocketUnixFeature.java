package scala.cli.internal;

import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.jdk.NativeLibrarySupport;
import com.oracle.svm.core.jdk.PlatformNativeLibrarySupport;
import com.oracle.svm.hosted.FeatureImpl;
import com.oracle.svm.hosted.c.NativeLibraries;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;

@AutomaticFeature
@Platforms({Platform.LINUX.class, Platform.DARWIN.class})
public class IpcsocketUnixFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        NativeLibrarySupport.singleton().preregisterUninitializedBuiltinLibrary("ipcsocket");
        PlatformNativeLibrarySupport.singleton().addBuiltinPkgNativePrefix("org_scalasbt_ipcsocket_JNIUnixDomainSocketLibraryProvider");
        NativeLibraries nativeLibraries = ((FeatureImpl.BeforeAnalysisAccessImpl) access).getNativeLibraries();
        nativeLibraries.addStaticJniLibrary("ipcsocket");
    }
}
