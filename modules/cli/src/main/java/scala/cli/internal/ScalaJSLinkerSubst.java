package scala.cli.internal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import scala.build.internal.ScalaJsConfig;

import java.nio.file.Path;

@TargetClass(className = "scala.cli.internal.ScalaJsLinker")
@Platforms({Platform.WINDOWS.class})
final class ScalaJsLinkerSubst {

    @Substitute
    void link(
        Path[] classPath,
        String mainClassOrNull,
        boolean addTestInitializer,
        ScalaJsConfig config,
        Path dest
    ) {
        throw new RuntimeException("Scala.JS linking unsupported on Windows");
    }

}
