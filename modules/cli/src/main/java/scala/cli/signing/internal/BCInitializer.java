package scala.cli.signing.internal;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

public final class BCInitializer {
  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  private BCInitializer() {}
}
