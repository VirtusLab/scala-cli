package scala.cli.signing.util

import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.security.Security

object BouncycastleSetup {
  private lazy val providerRegistration: Unit =
    Security.addProvider(new BouncyCastleProvider): Unit

  /** Registers the BouncyCastle security provider, if it wasn't registered already. Needs to be
    * called before any PGP operation relying on the "BC" provider.
    */
  def ensureProviderRegistered(): Unit = providerRegistration
}
