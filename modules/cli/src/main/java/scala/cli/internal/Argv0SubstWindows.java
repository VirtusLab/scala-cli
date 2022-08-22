package scala.cli.internal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;

import java.nio.file.Path;

@TargetClass(className = "scala.cli.internal.Argv0")
@Platforms({Platform.WINDOWS.class})
final class Argv0SubstWindows {

    @Substitute
    String get(String defaultValue) {
        return coursier.jniutils.ModuleFileName.get();
    }

}
