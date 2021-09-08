package scala.build.blooprifle.internal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import org.scalasbt.ipcsocket.Win32NamedPipeSocket;

import java.io.IOException;
import java.net.Socket;

@TargetClass(className = "scala.build.blooprifle.internal.NamedSocketBuilder")
@Platforms({Platform.WINDOWS.class})
final class WindowsNamedSocketBuilder {

    @Substitute
    Socket create(String path) {
        try {
            return new Win32NamedPipeSocket(path, true);
        } catch (IOException ex) {
            throw new RuntimeException("NamedSocketBuilder", ex);
        }
    }

}
