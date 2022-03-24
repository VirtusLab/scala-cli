package scala.cli.signing.internal;

// from https://github.com/micronaut-projects/micronaut-oracle-cloud/pull/17
// see also https://github.com/oracle/graal/issues/2800#issuecomment-702480444

import com.oracle.svm.core.annotate.AutomaticFeature;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;

import java.security.Security;

@AutomaticFeature
public class BouncyCastleFeature implements Feature {

    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        RuntimeClassInitialization.initializeAtBuildTime("org.bouncycastle");
        Security.addProvider(new BouncyCastleProvider());
    }

}
