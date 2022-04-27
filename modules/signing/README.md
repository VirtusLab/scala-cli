# scala-cli-signing

This project depends on and deals with [bouncycastle](https://www.bouncycastle.org), so that native launchers of Scala CLI
don't have to depend on it, and don't need to have native image process the bouncycastle classes.

Note that Scala CLI depends on some scala-cli-signing modules, but the Scala CLI classes calling scala-cli-signing classes that use bouncycastle are being substituted by others at native image build time (look for `@Substitute` in the Scala CLI sources). Those substitute classes manage to run equivalent stuff by downloading a scala-cli-signing binary, and running it in a separate process.

That way
- Scala CLI JVM launchers use scala-cli-signing directly
- Scala CLI native launchers use bits of scala-cli-signing as a library, and other parts by downloading and running the scala-cli-signing CLI binary in a separate process.

## Building

scala-cli-signing is built with Mill.

Compile everything with
```text
$ ./mill __.compile
```

Generate a native launcher of the CLI with
```text
$ ./mill show native-cli.base-image.nativeImage
```

As of writing this, there are no tests in this repository. New versions of scala-cli-signing are tested when bumping the scala-cli-signing version in Scala CLI.

## Modules

- `shared`: some classes that are used both by Scala CLI (even in native launchers) and scala-cli-signing
- `cli-options`: the case-app options of the commands of the scala-cli-signing CLI. These are used by Scala CLI to generate its reference doc, as the scala-cli-signing CLI commands are also exposed as sub-commands of Scala CLI.
- `cli`: the scala-cli-signing CLI itself, with commands such as `pgp create`, `pgp sign`, `pgp verify`
- `native-cli`: some GraalVM native-image-specific parts of the scala-cli-signing CLI

## Releases

- [Create a release](https://github.com/scala-cli/scala-cli-signing/releases/new) from the GitHub UI (creating or pushing a tag is not enough).
  The corresponding tag name should start with a `v`, like `v0.1.2`.
- Watch the corresponding [GitHub actions](https://github.com/scala-cli/scala-cli-signing/actions) job, restart it if it failed because of a transient error.
- Once the job is done running:
  - artifacts should have been added to the release as assets
  - the sync to Maven Central should be on-going
