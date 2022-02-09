
# scala-cli

[![Build status](https://github.com/VirtusLab/scala-cli/workflows/CI/badge.svg)](https://github.com/VirtusLab/scala-ci/actions?query=workflow%3ACI)
[![Maven Central](https://img.shields.io/maven-central/v/org.virtuslab.scala-cli/cli_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/org.virtuslab.scala-cli/cli_2.12)

Scala CLI is an experimental tool to run/compile/test Scala that aims at being a better `scala` command. It shares some similarities with build tools, but doesn't aim at supporting multi-module projects, nor to be extended via a task system.

User-facing documentation can be found on our website: [scala-cli.virtuslab.org](https://scala-cli.virtuslab.org/).

## Developer docs

### Requirements

Building Scala CLI requires a JVM 17 to work properly. 

In theory, our build is able to download and install for its own needs JVM 17 on some OSes however it may not work in Intellij / Metals out of the box. 

The Scala CLI sources ship with Mill launchers, so that Mill itself doesn't need to be installed on your system.

### Common commands

#### Running the CLI from sources

```bash
./mill -i scala …arguments…
```

#### Run unit tests

```bash
./mill 'build[_].test'
```

#### Run integration tests with the JVM launcher

```bash
./mill integration.jvm.test
```

Filter test suites with
```bash
./mill integration.jvm.test 'scala.cli.integration.RunTests.*'
./mill integration.jvm.test 'scala.cli.integration.RunTests.Multiple scripts'
```

#### Run integration tests with the native launcher

(generating the launcher can take several minutes)

```bash
./mill integration.native.test
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

#### Generate Metals configuration files

```bash
./mill mill.contrib.Bloop/install
```

Then run the command "Metals: Connect to build server".

(Recommended over the Metals import project functionality.)

Whenever the build is updated, just do these two steps again.

#### Generate IntelliJ configuration files

```bash
./mill mill.scalalib.GenIdea/idea
```

Then open the scala-cli directory in IntelliJ.

(Recommended over the IntelliJ import project functionality.)

Whenever the build is updated, run the command above again. IntelliJ
should then pick up the new changes.

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

### Website 

The Scala CLI website is built with [Docusaurus](https://v1.docusaurus.io/en/) and uses [Infima](https://infima.dev/docs/layout/spacing) for styling. 

#### Generate the website once

```bash
cd website
yarn build
npm run serve
```

#### Generate the website continuously

```bash
cd website
yarn run start
```

### Verifying the documentation

We have a built-in tool to validate `.md` files called [Sclicheck](/sclicheck/Readme.md). To check all douments (and this is what we run on CI) run:

```.github/scripts/check_docs.sh```

You can also check single documents or directories using 


```
.github/scripts/check_docs.sh <file> <dir>
```

To debug failing document, Sclicheck has build-in following options: `--step` (stop after each command) or `--stopAtFailure` (to stop after a failure). To debug  getting started guide run following command:

```
.github/scripts/check_docs.sh --stopAtFailure docs/getting_started.md
```

## Scala CLI logos

Package with various logos for scala-cli can be found on [google drive](https://drive.google.com/drive/u/1/folders/1M6JeQXmO4DTBeRBKAFJ5HH2p_hbfQnqS)

## Launcher script

There is a script `scala-cli-src` in the repository root that is intended to work exactly like released scala-cli, but using a binary compiled the worktree.
Just add it to your PATH to get the already-released-scala-cli experience.

## Releases

Instructions on how to release - [Release Procedure](https://github.com/VirtusLab/scala-cli/blob/main/.github/release/release-procedure.md)
