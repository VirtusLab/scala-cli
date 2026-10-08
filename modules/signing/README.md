# signing

PGP signing support in Scala CLI: creating PGP keys, signing files and verifying signatures,
using [bouncycastle](https://www.bouncycastle.org).

This code used to live in a separate project, [scala-cli-signing](https://github.com/VirtusLab/scala-cli-signing),
whose source history was imported into this repository. Scala CLI native launchers used to run a separate
scala-cli-signing binary, so that they didn't have to include bouncycastle. Signing now always runs within Scala CLI,
on both JVM and native launchers.

## Where things are

- `modules/signing` (this module, published as `org.virtuslab.scala-cli::signing`): `PasswordOption` and `Secret`
  (package `scala.cli.signing`), used by option & directive handling across Scala CLI, without depending on
  bouncycastle.
- `modules/cli`:
  - `scala.cli.commands.pgp`: the `pgp create`, `pgp key-id`, `pgp sign` and `pgp verify` sub-commands (as well as
    `pgp pull` and `pgp push`)
  - `scala.cli.signing.util`: the bouncycastle-based signer (`BouncycastleSigner`, used by `publish`), key generation
    helpers, and `BouncycastleSetup`, which registers the bouncycastle security provider before PGP operations
  - `scala.cli.signing.internal.BCInitializer` and
    `META-INF/native-image/org.virtuslab/scala-cli-signing/`: the GraalVM native-image configuration for
    bouncycastle

## Building & testing

```text
$ ./mill -i 'signing[].compile'
$ ./mill -i 'cli[].test' 'scala.cli.signing.*'
$ ./mill -i integration.test.jvm 'scala.cli.integration.Pgp*'
```

The module is versioned and released together with the rest of Scala CLI.
