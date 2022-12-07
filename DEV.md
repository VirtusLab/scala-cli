## Developer docs

### Requirements

Building Scala CLI requires JVM 17 to work properly.

In theory, our build is able to download and install for its own needs JVM 17 on some OSes however it may not work in
Intellij / Metals out of the box.

The Scala CLI sources ship with Mill launchers, so that Mill itself doesn't need to be installed on your system.

### Common commands

#### Running the CLI from sources

```bash
./mill -i scala …arguments…
```

#### Run unit tests

```bash
./mill 'build-module.test'
```

To can filter unit test suites:

```bash
./mill 'build-module.test' 'scala.build.tests.BuildTestsScalac.*'
./mill 'build-module.test' 'scala.build.tests.BuildTestsScalac.simple'
```

#### Run integration tests with the JVM launcher

```bash
./mill integration.test.jvm
```

Filter test suites with

```bash
./mill integration.test.jvm 'scala.cli.integration.RunTestsDefault.*'
./mill integration.test.jvm 'scala.cli.integration.RunTestsDefault.Multiple scripts'
```

You can pass the `--debug` option to debug Scala CLI when running integration tests. Note that this allows to debug the
Scala CLI launcher (the app) and not the integration test code itself. The debugger is being run in the `attach` mode.

```bash
./mill integration.test.jvm 'scala.cli.integration.RunTestsDefault.*' --debug
```

The debug option uses 5005 port by default. It is possible to change it as follows:

```bash
./mill integration.test.jvm 'scala.cli.integration.RunTestsDefault.*' --debug:5006
```

#### Run integration tests with the native launcher

(generating the launcher can take several minutes)

```bash
./mill integration.test.native
./mill integration.test.native 'scala.cli.integration.RunTestsDefault.*'
```

#### Generate native packages

Build native packagers:

* `deb` for linux
* `msi` for windows
* `dmg` and `pkg` for macOS

(generating native packager for specified format)

```bash
./mill -i scala package ..arguments... --deb --output 'path.deb'
./mill -i scala package ..arguments... --dmg --output 'path.dmg'
./mill -i scala package ..arguments... --pkg --output 'path.pkg'
```

#### IDE Import

The Scala CLI repository should work when imported automatically from Mill to IDEA IntelliJ and Metals.
Please raise an issue if you run into any problems.

#### Generate a native launcher

```bash
./mill -i show cli.nativeImage
```

This prints the path to the generated native image.
The file named `scala` at the root of the project should also
be a link to it. (Note that the link is committed and is always there,
whether the files it points at exists or not.)

#### Generate a JVM launcher

```bash
./mill -i show cli.launcher
```

This prints the path to the generated launcher. This launcher is a JAR,
that directly re-uses the class directories of the modules of the project
(so that cleaning up those classes will break the launcher). If this is a
problem (if you wish to run the launcher on another machine or from a
Docker image for example), use a native launcher (see above) or a standalone
JVM one (see below).

#### Generate a standalone JVM launcher

```bash
./mill -i show cli.standaloneLauncher
```

This prints the path to the generated launcher. This launcher is a JAR,
that embeds JARs of the scala-cli modules, and downloads their dependencies
from Maven Central upon first launch (using the coursier cache, just like
a coursier bootstrap).

### Helper projects

A number of features of Scala CLI are managed from external projects, living under
the [`scala-cli` organization](https://github.com/scala-cli) on GitHub. These
projects can be used by Scala CLI as libraries pulled before it's compiled, but also
as binaries. In the latter case, Scala CLI downloads on-the-fly binaries from these
repositories' GitHub release assets, and runs them as external processes.

For example, here are a few external projects used by Scala CLI:

- [scala-js-cli-native-image](https://github.com/scala-cli/scala-js-cli-native-image): provides a binary running the
  Scala.js linker
- [scala-cli-signing](https://github.com/scala-cli/scala-cli-signing): provides both libraries and binaries to handle
  PGP concerns in Scala CLI
- [libsodiumjni](https://github.com/scala-cli/libsodiumjni): provides minimal JNI bindings for
  [libsodium](https://github.com/jedisct1/libsodium), that is used by Scala CLI to encrypt secrets
  uploaded as GitHub repository secrets in the `publish setup` sub-command

For the full list of those projects and their description, see the
[scala-cli repository list](https://github.com/orgs/scala-cli/repositories) and the READMEs
of each of these projects.

The use of external binaries allows to make the Scala CLI binary slimmer and faster
to generate, but also allow to lower memory requirements to generate it (allowing to
generate these binaries on the GitHub-provided GitHub actions hosts).

### Website

The Scala CLI website is built with [Docusaurus](https://v1.docusaurus.io/en/) and
uses [Infima](https://infima.dev/docs/layout/spacing) for styling.

Ensure you are using Node >= 16.14.2.

#### Generate the website once

```bash
cd website
yarn
yarn build
npm run serve
```

#### Generate the website continuously

```bash
cd website
yarn
yarn run start
```

### Verifying the documentation

We have a built-in tool to validate `.md` files called [Sclicheck](/sclicheck/Readme.md).
All `Sclicheck` tests can be run with `Mill` + `munit`: (and this is what we run on the CI, too)

```bash
./mill -i docs-tests.test
```

The former also includes testing gifs and `Sclicheck` itself.
To just check the documents, run:

```bash
./mill -i docs-tests.test 'sclicheck.DocTests.*'
```

You can also check all root docs, commands, reference docs, guides or cookbooks:

```bash
./mill -i docs-tests.test 'sclicheck.DocTests.root*'
./mill -i docs-tests.test 'sclicheck.DocTests.guide*'
./mill -i docs-tests.test 'sclicheck.DocTests.command*'
./mill -i docs-tests.test 'sclicheck.DocTests.cookbook*'
./mill -i docs-tests.test 'sclicheck.DocTests.reference*'
```

Similarly, you can check single files:

```bash
./mill -i docs-tests.test 'sclicheck.DocTests.<category> <doc-name>'
```

For example, to run the check on `compile.md`

```bash
./mill -i docs-tests.test 'sclicheck.DocTests.command compile'
```

## Scala CLI logos

Package with various logos for scala-cli can be found
on [google drive](https://drive.google.com/drive/u/1/folders/1M6JeQXmO4DTBeRBKAFJ5HH2p_hbfQnqS)

## Launcher script

There is a script `scala-cli-src` in the repository root that is intended to work exactly like released scala-cli, but
using a binary compiled the worktree.
Just add it to your PATH to get the already-released-scala-cli experience.

## Releases

Instructions on how to
release - [Release Procedure](https://github.com/VirtusLab/scala-cli/blob/main/.github/release/release-procedure.md)

## Debugging BSP server

The easiest way to debug BSP sever is using `scala-cli-src` script with `--bsp-debug-port 5050` flag (the port should be
unique to the workspace where BSP will be debugged). In such case BSP will be launched using local source and will run
on JVM. It will also expects a debugger running in the listen mode using provided port (so the initialization of the
connection can be debugged). In such case we recommend to have option to auto rerun debugging session off (so there is
always a debugger instance ready to be used).