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

This command runs the unit tests from the `build-module` module.

```bash
./mill 'build-module.test'
```

If you want to run unit tests for another module, set `module_name` to the name of the module from which you want to run
the unit tests:

```bash
./mill 'module_name.test'
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

#### Generate JUnit test reports

As running tests with mill generates output in a non-standard JSON format, we have a script for converting it to the 
more well known JUnit XML test report format which we can then process and view on the CI.
In case you want to generate a test report locally, you can run the following command:

```bash
.github/scripts/generate-junit-reports.sc <test suite title> <test report title> <output-path out/
```

The test should fail when no test reports were found or if no tests were actually run.

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

When working with IntelliJ make sure that the project's Java is set correctly.
To confirm, check under `File -> Project Structure` that:

- in `Project Settings/Project` `SDK` and `Language level` is set to **17**
- in `Project Settings/Modules` all the modules have `Language level` set to **17**
- in `Platform Settings/SDKs` only **Java 17** is visible

Otherwise, some IDE features may not work correctly, i.e. the debugger might crash upon connection.

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
the [`scala-cli`](https://github.com/scala-cli) and [`VirtusLab`](https://github.com/VirtusLab) organizations on GitHub. These
projects can be used by Scala CLI as libraries pulled before it's compiled, but also
as binaries. In the latter case, Scala CLI downloads on-the-fly binaries from these
repositories' GitHub release assets, and runs them as external processes.

Here's some of the more important external projects used by Scala CLI:

- [scala-js-cli-native-image](https://github.com/VirtusLab/scala-js-cli): provides a binary running the
  Scala.js linker
- [scala-cli-signing](https://github.com/VirtusLab/scala-cli-signing): provides both libraries and binaries to handle
  PGP concerns in Scala CLI
- [libsodiumjni](https://github.com/VirtusLab/libsodiumjni): provides minimal JNI bindings for
  [libsodium](https://github.com/jedisct1/libsodium), that is used by Scala CLI to encrypt secrets
  uploaded as GitHub repository secrets in the `publish setup` sub-command
- [scala-cli-setup](https://github.com/VirtusLab/scala-cli-setup): a GitHub Action to install Scala CLI.
- [scalafmt-native-image](https://github.com/VirtusLab/scalafmt-native-image): GraalVM native-image launchers
  for `scalafmt`.
- [bloop-core](https://github.com/scala-cli/bloop-core): a fork of [bloop](https://github.com/scalacenter/bloop)
  stripped up of its benchmark infrastructure and build integrations.
- [no-crc32-zip-input-stream](https://github.com/VirtusLab/no-crc32-zip-input-stream): A copy of `ZipInputStream` 
  from OpenJDK, with CRC32 calculations disabled.
- [lightweight-spark-distrib](https://github.com/VirtusLab/lightweight-spark-distrib): a small application allowing
  to make Spark distributions more lightweight.
- [java-class-name](https://github.com/VirtusLab/java-class-name): a small library to extract class names
  from Java sources.

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

## GraalVM reflection configuration

As Scala CLI is using GraalVM native image, it requires a configuration file for reflection.
The configuration for the `cli` module is located
in [the reflect-config.json](modules/cli/src/main/resources/META-INF/native-image/org.virtuslab/scala-cli-core/reflect-config.json)
file.

When adding new functionalities or updating dependencies, it might turn out the reflection configuration for some class
may be missing. The relevant error message when running `integration.test.native` may be misleading,
usually with a `ClassNotFoundException` or even with a functionality seemingly being skipped.
This is because logic referring to classes with missing reflection configuration may be skipped for the used native
image.

To generate the relevant configuration automatically, you can run:

```bash
./mill -i cli.runWithAssistedConfig <scala-cli-sub-command> <args> <options>
```

Just make sure to run it exactly the same as the native image would have been run, as the configuration is generated for
a particular invocation path. The run has to succeed as well, as the configuration will only be fully generated after an
exit code 0.

```text
Config generated in out/cli/runWithAssistedConfig.dest/config
```

As a result, you should get the path to the generated configuration file. It might contain some unnecessary entries, so
make sure to only copy what you truly need.
As the formatting of the `reflect-config.json` is verified on the CI, make sure to run the following command to adjust
it accordingly before committing:

```bash
./mill -i __.formatNativeImageConf
```

For more info about reflection configuration in GraalVM,
check [the relevant GraalVM Reflection docs](https://www.graalvm.org/latest/reference-manual/native-image/dynamic-features/Reflection/).

## Overriding Scala versions in Scala CLI builds

It's possible to override the internal Scala version used to build Scala CLI,
as well as the default version used by the CLI itself with Java props.
- `scala.version.internal` - overrides the internal Scala version used to build Scala CLI
- `scala.version.user` - overrides the default Scala version used by the CLI itself

NOTE: remember to run `./mill clean` to make sure the Scala versions aren't being cached anywhere.

```bash
./mill -i clean
./mill -i --define scala.version.internal=3.4.0-RC1-bin-20231012-242ba21-NIGHTLY --define scala.version.user=3.4.0-RC1-bin-20231012-242ba21-NIGHTLY scala version --offline
# Scala CLI version: 1.x.x-SNAPSHOT
# Scala version (default): 3.4.0-RC1-bin-20231012-242ba21-NIGHTLY
```
