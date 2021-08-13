package scala.cli.internal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import packager.windows.ImageResizer;

@TargetClass(className = "scala.cli.internal.GetImageResizer")
@Platforms({Platform.WINDOWS.class})
final class GetImageResizerSubst {

    @Substitute
    ImageResizer get() {
        return null;
    }

}
