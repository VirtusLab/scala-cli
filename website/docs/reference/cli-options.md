---
title: Command-line options
sidebar_position: 1
---



This is a summary of options that are available for each subcommand of the `scala-cli` command.

## Scalac options forwarding

 All options that start with:


- `-g`
- `-language`
- `-opt`
- `-P`
- `-target`
- `-V`
- `-W`
- `-X`
- `-Y`

are assumed to be Scala compiler options and will be propagated to Scala Compiler. This applies to all commands that uses compiler directly or indirectly.


 ## Scalac options that are directly supported in scala CLI (so can be provided as is, without any prefixes etc.):

 - `-encoding`
 - `-release`
 - `-color`
 - `-nowarn`
 - `-feature`
 - `-deprecation`



## Benchmarking options

Available in commands:

[`run`](./commands.md#run), [`shebang`](./commands.md#shebang)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--jmh`

Run JMH benchmarks

### `--jmh-version`

Set JMH version

## Compilation server options

Available in commands:

[`bloop`](./commands.md#bloop), [`bloop exit`](./commands.md#bloop-exit), [`bloop output`](./commands.md#bloop-output), [`bloop start`](./commands.md#bloop-start), [`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test), [`uninstall`](./commands.md#uninstall)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--bloop-bsp-protocol`

[Internal]
Protocol to use to open a BSP connection with Bloop

### `--bloop-bsp-socket`

[Internal]
Socket file to use to open a BSP connection with Bloop

### `--bloop-host`

[Internal]
Host the compilation server should bind to

### `--bloop-port`

[Internal]
Port the compilation server should bind to (pass `-1` to pick a random port)

### `--bloop-daemon-dir`

[Internal]
Daemon directory of the Bloop daemon (directory with lock, pid, and socket files)

### `--bloop-version`

[Internal]
If Bloop isn't already running, the version we should start

### `--bloop-bsp-timeout`

[Internal]
Maximum duration to wait for the BSP connection to be opened

### `--bloop-bsp-check-period`

[Internal]
Duration between checks of the BSP connection state

### `--bloop-startup-timeout`

[Internal]
Maximum duration to wait for the compilation server to start up

### `--bloop-default-java-opts`

[Internal]
Include default JVM options for Bloop

### `--bloop-java-opt`

[Internal]
Pass java options to use by Bloop server

### `--bloop-global-options-file`

[Internal]
Bloop global options file

### `--bloop-jvm`

[Internal]
JVM to use to start Bloop (e.g. 'system|11', 'temurin:17', …)

### `--bloop-working-dir`

[Internal]
Working directory for Bloop, if it needs to be started

### `--server`

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

## Compile options

Available in commands:

[`compile`](./commands.md#compile)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--print-class-path`

Aliases: `-p`, `--print-classpath`

Print the resulting class path

### `--test`

Compile test scope

## Cross options

Available in commands:

[`compile`](./commands.md#compile), [`package`](./commands.md#package), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--cross`

Run given command against all provided Scala versions and/or platforms

## Debug options

Available in commands:

[`bloop`](./commands.md#bloop), [`bloop start`](./commands.md#bloop-start), [`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`config`](./commands.md#config), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`pgp push`](./commands.md#pgp-push), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`publish setup`](./commands.md#publish-setup), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--debug`

Turn debugging on

### `--debug-port`

Debug port (5005 by default)

### `--debug-mode`

Debug mode (attach by default)

## Dependency options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--dependency`

Aliases: `--dep`

Add dependencies

### `--repository`

Aliases: `-r`, `--repo`

Add repositories

### `--compiler-plugin`

Aliases: `-P`, `--plugin`

Add compiler plugin dependencies

## Dependency update options

Available in commands:

[`dependency-update`](./commands.md#dependency-update)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--all`

Update all dependencies if newer version was released

## Directories options

Available in commands:

[`bloop`](./commands.md#bloop), [`bloop exit`](./commands.md#bloop-exit), [`bloop output`](./commands.md#bloop-output), [`bloop start`](./commands.md#bloop-start), [`bsp`](./commands.md#bsp), [`clean`](./commands.md#clean), [`compile`](./commands.md#compile), [`config`](./commands.md#config), [`dependency-update`](./commands.md#dependency-update), [`directories`](./commands.md#directories), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`install completions` , `install-completions`](./commands.md#install-completions), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`publish setup`](./commands.md#publish-setup), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test), [`uninstall`](./commands.md#uninstall)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--home-directory`

Aliases: `--home`

Override the path to user's home directory

## Doc options

Available in commands:

[`doc`](./commands.md#doc)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--output`

Aliases: `-o`

Set the destination path

### `--force`

Aliases: `-f`

Overwrite the destination directory, if it exists

### `--default-scaladoc-options`

Aliases: `--default-scaladoc-opts`

Control if Scala CLI should use default options for scaladoc, true by default. Use `--default-scaladoc-opts:false` to not include default options.

## Export options

Available in commands:

[`export`](./commands.md#export)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--sbt`

### `--mill`

### `--sbt-setting`

Aliases: `--setting`

### `--project`

Aliases: `-p`

Project name to be used on Mill build file

### `--sbt-version`

### `--output`

Aliases: `-o`

## Fmt options

Available in commands:

[`fmt` , `format` , `scalafmt`](./commands.md#fmt)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--check`

Check if sources are well formatted

### `--respect-project-filters`

Use project filters defined in the configuration. Turned on by default, use `--respect-project-filters:false` to disable it.

### `--save-scalafmt-conf`

Saves .scalafmt.conf file if it was created or overwritten

### `--os-arch-suffix`

[Internal]
### `--scalafmt-tag`

[Internal]
### `--scalafmt-github-org-name`

[Internal]
### `--scalafmt-extension`

[Internal]
### `--scalafmt-launcher`

[Internal]
### `--scalafmt-arg`

Aliases: `-F`

Pass argument to scalafmt.

### `--scalafmt-conf`

Aliases: `--scalafmt-config`

Custom path to the scalafmt configuration file.

### `--scalafmt-conf-str`

Aliases: `--scalafmt-conf-snippet`, `--scalafmt-config-str`

Pass configuration as a string.

### `--scalafmt-dialect`

Aliases: `--dialect`

Pass a global dialect for scalafmt. This overrides whatever value is configured in the .scalafmt.conf file or inferred based on Scala version used.

### `--scalafmt-version`

Aliases: `--fmt-version`

Pass scalafmt version before running it (3.6.1 by default). If passed, this overrides whatever value is configured in the .scalafmt.conf file.

## Help options

Available in commands:

[`add-path`](./commands.md#add-path), [`bloop`](./commands.md#bloop), [`bloop exit`](./commands.md#bloop-exit), [`bloop output`](./commands.md#bloop-output), [`bloop start`](./commands.md#bloop-start), [`bsp`](./commands.md#bsp), [`clean`](./commands.md#clean), [`compile`](./commands.md#compile), [`config`](./commands.md#config), [`default-file`](./commands.md#default-file), [`dependency-update`](./commands.md#dependency-update), [`directories`](./commands.md#directories), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`help`](./commands.md#help), [`install completions` , `install-completions`](./commands.md#install-completions), [`install-home`](./commands.md#install-home), [`github secret list` , `gh secret list`](./commands.md#github-secret-list), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`pgp create`](./commands.md#pgp-create), [`pgp key-id`](./commands.md#pgp-key-id), [`pgp pull`](./commands.md#pgp-pull), [`pgp push`](./commands.md#pgp-push), [`pgp sign`](./commands.md#pgp-sign), [`pgp verify`](./commands.md#pgp-verify), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`publish setup`](./commands.md#publish-setup), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`github secret create` , `gh secret create`](./commands.md#github-secret-create), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test), [`uninstall`](./commands.md#uninstall), [`uninstall completions` , `uninstall-completions`](./commands.md#uninstall-completions), [`update`](./commands.md#update), [`version`](./commands.md#version)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--usage`

Print usage and exit

### `--help`

Aliases: `-h`, `-help`

Print help message and exit

### `--help-full`

Aliases: `--full-help`, `-full-help`, `-help-full`

Print help message, including hidden options, and exit

## Help group options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--help-js`

Show options for ScalaJS

### `--help-native`

Show options for ScalaNative

### `--help-scaladoc`

Aliases: `--doc-help`, `--help-doc`, `--scaladoc-help`

Show options for Scaladoc

### `--help-repl`

Aliases: `--repl-help`

Show options for Scala REPL

### `--help-scalafmt`

Aliases: `--fmt-help`, `--help-fmt`, `--scalafmt-help`

Show options for Scalafmt

## Install completions options

Available in commands:

[`install completions` , `install-completions`](./commands.md#install-completions)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--format`

Aliases: `--shell`

Name of the shell, either zsh or bash

### `--rc-file`

Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell

### `--output`

Aliases: `-o`

Completions output directory

### `--banner`

[Internal]
Custom banner in comment placed in rc file

### `--name`

[Internal]
Custom completions name

### `--env`

Print completions to stdout

## Java options

Available in commands:

[`package`](./commands.md#package), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--java-opt`

Aliases: `-J`

Set Java options, such as `-Xmx1g`

## Java prop options

Available in commands:

[`package`](./commands.md#package), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--java-prop-option`

Aliases: `--java-prop`

Add java properties. Note that options equal `-Dproperty=value` are assumed to be java properties and don't require to be passed after `--java-prop`.

## Jvm options

Available in commands:

[`bloop`](./commands.md#bloop), [`bloop start`](./commands.md#bloop-start), [`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`config`](./commands.md#config), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`pgp push`](./commands.md#pgp-push), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`publish setup`](./commands.md#publish-setup), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--java-home`

Set the Java home directory

### `--jvm`

Aliases: `-j`

Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`

### `--jvm-index`

[Internal]
JVM index URL

### `--jvm-index-os`

[Internal]
Operating system to use when looking up in the JVM index

### `--jvm-index-arch`

[Internal]
CPU architecture to use when looking up in the JVM index

### `--javac-plugin`

[Internal]
Javac plugin dependencies or files

### `--javac-option`

Aliases: `--javac-opt`

[Internal]
Javac options

### `--bsp-debug-port`

[Internal]
Port for BSP debugging

## Logging options

Available in commands:

[`add-path`](./commands.md#add-path), [`bloop`](./commands.md#bloop), [`bloop exit`](./commands.md#bloop-exit), [`bloop output`](./commands.md#bloop-output), [`bloop start`](./commands.md#bloop-start), [`bsp`](./commands.md#bsp), [`clean`](./commands.md#clean), [`compile`](./commands.md#compile), [`config`](./commands.md#config), [`default-file`](./commands.md#default-file), [`dependency-update`](./commands.md#dependency-update), [`directories`](./commands.md#directories), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`help`](./commands.md#help), [`install completions` , `install-completions`](./commands.md#install-completions), [`install-home`](./commands.md#install-home), [`github secret list` , `gh secret list`](./commands.md#github-secret-list), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`pgp pull`](./commands.md#pgp-pull), [`pgp push`](./commands.md#pgp-push), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`publish setup`](./commands.md#publish-setup), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`github secret create` , `gh secret create`](./commands.md#github-secret-create), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test), [`uninstall`](./commands.md#uninstall), [`uninstall completions` , `uninstall-completions`](./commands.md#uninstall-completions), [`update`](./commands.md#update), [`version`](./commands.md#version)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--quiet`

Aliases: `-q`

Decrease verbosity

### `--progress`

Use progress bars

## Main class options

Available in commands:

[`export`](./commands.md#export), [`package`](./commands.md#package), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`run`](./commands.md#run), [`shebang`](./commands.md#shebang)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--main-class`

Aliases: `-M`

Specify which main class to run

### `--main-class-ls`

Aliases: `--list-main-class`, `--list-main-classes`, `--main-class-list`

List main classes available in the current context

## Markdown options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--enable-markdown`

Aliases: `--markdown`, `--md`

Enable markdown support.

## Package options

Available in commands:

[`package`](./commands.md#package)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--output`

Aliases: `-o`

Set the destination path

### `--force`

Aliases: `-f`

Overwrite the destination file, if it exists

### `--library`

Generate a library JAR rather than an executable JAR

### `--source`

Generate a source JAR rather than an executable JAR

### `--doc`

Aliases: `--javadoc`, `--scaladoc`

Generate a scaladoc JAR rather than an executable JAR

### `--assembly`

Generate an assembly JAR

### `--preamble`

For assembly JAR, whether to add a bash / bat preamble

### `--main-class-in-manifest`

[Internal]
For assembly JAR, whether to specify a main class in the JAR manifest

### `--spark`

[Internal]
Generate an assembly JAR for Spark (assembly that doesn't contain Spark, nor any of its dependencies)

### `--standalone`

Package standalone JARs

### `--deb`

Build Debian package, available only on Linux

### `--dmg`

Build dmg package, available only on macOS

### `--rpm`

Build rpm package, available only on Linux

### `--msi`

Build msi package, available only on Windows

### `--pkg`

Build pkg package, available only on macOS

### `--docker`

Build Docker image

### `--provided`

[Internal]
Exclude modules *and their transitive dependencies* from the JAR to be packaged

### `--default-scaladoc-options`

Aliases: `--default-scaladoc-opts`

Use default scaladoc options

### `--native-image`

Aliases: `--graal`

Build GraalVM native image

## Packager options

Available in commands:

[`package`](./commands.md#package)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--version`

Set the version of the generated package

### `--logo-path`

Path to application logo in PNG format, it will be used to generate icon and banner/dialog in msi installer

### `--launcher-app`

Set launcher app name, which will be linked to the PATH

### `--description`

### `--maintainer`

Aliases: `-m`

This should contain names and email addresses of co-maintainers of the package

### `--debian-conflicts`

The list of Debian package that this package is not compatible with

### `--debian-dependencies`

The list of Debian packages that this package depends on

### `--deb-architecture`

Architectures that are supported by the repository (default: all)

### `--priority`

This field represents how important it is that the user have the package installed

### `--section`

This field specifies an application area into which the package has been classified

### `--identifier`

CF Bundle Identifier

### `--license`

Licenses that are supported by the repository (list of licenses: https://fedoraproject.org/wiki/Licensing:Main?rd=Licensing)

### `--release`

The number of times this version of the software was released (default: 1)

### `--rpm-architecture`

Architectures that are supported by the repository (default: noarch)

### `--license-path`

Path to the license file

### `--product-name`

Name of product (default: Scala packager)

### `--exit-dialog`

Text that will be displayed on the exit dialog

### `--suppress-validation`

Suppress Wix ICE validation (required for users that are neither interactive, not local administrators)

### `--extra-config`

Path to extra WIX configuration content

### `--is64-bits`

Aliases: `--64`

Whether a 64-bit executable is being packaged

### `--installer-version`

WIX installer version

### `--wix-upgrade-code-guid`

The GUID to identify that the windows package can be upgraded.

### `--docker-from`

Building the container from base image

### `--docker-image-registry`

The image registry; if empty, it will use the default registry

### `--docker-image-repository`

The image repository

### `--docker-image-tag`

The image tag; the default tag is `latest`

### `--graalvm-java-version`

GraalVM Java major version to use to build GraalVM native images (17 by default)

### `--graalvm-version`

GraalVM version to use to build GraalVM native images (22.3.0 by default)

### `--graalvm-jvm-id`

JVM id of GraalVM distribution to build GraalVM native images (like "graalvm-java17:22.0.0")

### `--graalvm-args`

Pass args to GraalVM

## Pgp push pull options

Available in commands:

[`pgp pull`](./commands.md#pgp-pull), [`pgp push`](./commands.md#pgp-push), [`publish setup`](./commands.md#publish-setup)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--key-server`

Key server to push / pull keys from

## Publish options

Available in commands:

[`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--working-dir`

[Internal]
Directory where temporary files for publishing should be written

### `--scala-version-suffix`

[Internal]
Scala version suffix to append to the module name, like "_2.13" or "_3"

### `--scala-platform-suffix`

[Internal]
Scala platform suffix to append to the module name, like "_sjs1" or "_native0.4"

### `--sources`

Whether to build and publish source JARs

### `--doc`

Aliases: `--javadoc`, `--scaladoc`

Whether to build and publish doc JARs

### `--gpg-key`

Aliases: `-K`

ID of the GPG key to use to sign artifacts

### `--signer`

Method to use to sign artifacts

### `--gpg-option`

Aliases: `-G`, `--gpg-opt`

gpg command-line options

### `--ivy2-home`

Set Ivy 2 home directory

### `--force-signing-binary`

[Internal]
### `--checksum`

[Internal]
### `--dummy`

Proceed as if publishing, but do not upload / write artifacts to the remote repository

### `--ivy2-local-like`

[Internal]
### `--parallel-upload`

[Internal]
## Publish params options

Available in commands:

[`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`publish setup`](./commands.md#publish-setup)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--organization`

Organization to publish artifacts under

### `--name`

Name to publish artifacts as

### `--module-name`

Final name to publish artifacts as, including Scala version and platform suffixes if any

### `--version`

Version to publish artifacts as

### `--compute-version`

How to compute the version to publish artifacts as

### `--url`

URL to put in publishing metadata

### `--license`

License to put in publishing metadata

### `--vcs`

VCS information to put in publishing metadata

### `--description`

Description to put in publishing metadata

### `--developer`

Developer(s) to add in publishing metadata, like "alex|Alex|https://alex.info" or "alex|Alex|https://alex.info|alex@alex.me"

### `--secret-key`

Secret key to use to sign artifacts with Bouncy Castle

### `--secret-key-password`

Aliases: `--secret-key-pass`

Password of secret key to use to sign artifacts with Bouncy Castle

### `--ci`

Use or setup publish parameters meant to be used on continuous integration

## Publish repository options

Available in commands:

[`publish`](./commands.md#publish), [`publish setup`](./commands.md#publish-setup)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--publish-repository`

Aliases: `-R`, `--publish-repo`

Repository to publish to

### `--user`

User to use with publishing repository

### `--password`

Password to use with publishing repository

### `--realm`

Realm to use when passing credentials to publishing repository

## Publish setup options

Available in commands:

[`publish setup`](./commands.md#publish-setup)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--public-key`

Public key to use to verify artifacts (to be uploaded to a key server)

### `--check`

Check if some options for publishing are missing, and exit with non-zero return code if that's the case

### `--token`

GitHub token to use to upload secrets to GitHub - password encoded

### `--random-secret-key`

Generate a random key pair for publishing, with a secret key protected by a random password

### `--random-secret-key-mail`

When generating a random key pair, the mail to associate to it

### `--checks`

The option groups to check - can be "all", or a comma-separated list of "core", "signing", "repo", "extra"

### `--check-workflow`

Whether to check if a GitHub workflow already exists (one for publishing is written if none is found)

### `--dummy`

Dummy mode - don't upload any secret to GitHub

## Python options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--python-setup`

Set Java options so that Python can be loaded

### `--python`

Aliases: `--py`

Enable Python support via ScalaPy

### `--scala-py-version`

Aliases: `--scalapy-version`

[experimental] Set ScalaPy version (0.5.2+5-83f1eb68 by default)

## Repl options

Available in commands:

[`repl` , `console`](./commands.md#repl)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--ammonite`

Aliases: `-A`, `--amm`

Use Ammonite (instead of the default Scala REPL)

### `--ammonite-version`

Aliases: `--ammonite-ver`

Set the Ammonite version (2.5.6 by default)

### `--ammonite-arg`

Aliases: `-a`

[Internal]
Provide arguments for ammonite repl

### `--repl-dry-run`

[Internal]
Don't actually run the REPL, just fetch it

## Run options

Available in commands:

[`run`](./commands.md#run), [`shebang`](./commands.md#shebang)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--spark-submit`

Aliases: `--spark`

[Internal]
Run as a Spark job, using the spark-submit command

### `--submit-argument`

Aliases: `--submit-arg`

[Internal]
Spark-submit arguments

### `--standalone-spark`

Aliases: `--spark-standalone`

Run as a Spark job, using a vanilla Spark distribution downloaded by Scala CLI

### `--hadoop-jar`

Aliases: `--hadoop`

Run as a Hadoop job, using the "hadoop jar" command

### `--command`

Print the command that would have been run (one argument per line), rather than running it

### `--scratch-dir`

Temporary / working directory where to write generated launchers

### `--use-manifest`

[Internal]
Run Java commands using a manifest-based class path (shortens command length)

## Scala.js options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--js`

Enable Scala.js. To show more options for Scala.js pass `--help-js`

### `--js-version`

The Scala.js version (1.12.0 by default).

### `--js-mode`

The Scala.js mode, either `dev` or `release`

### `--js-module-kind`

The Scala.js module kind: commonjs/common, esmodule/es, nomodule/none

### `--js-check-ir`

### `--js-emit-source-maps`

Emit source maps

### `--js-source-maps-path`

Set the destination path of source maps

### `--js-dom`

Enable jsdom

### `--js-header`

A header that will be added at the top of generated .js files

### `--js-allow-big-ints-for-longs`

Primitive Longs *may* be compiled as primitive JavaScript bigints

### `--js-avoid-classes`

Avoid class'es when using functions and prototypes has the same observable semantics.

### `--js-avoid-lets-and-consts`

Avoid lets and consts when using vars has the same observable semantics.

### `--js-module-split-style`

The Scala.js module split style: fewestmodules, smallestmodules, smallmodulesfor

### `--js-small-module-for-package`

Create as many small modules as possible for the classes in the passed packages and their subpackages.

### `--js-es-version`

The Scala.js ECMA Script version: es5_1, es2015, es2016, es2017, es2018, es2019, es2020, es2021

### `--js-linker-path`

[Internal]
Path to the Scala.js linker

### `--js-cli-version`

[Internal]
Scala.js CLI version to use for linking (1.1.3-sc1 by default).

### `--js-cli-java-arg`

[Internal]
Scala.js CLI Java options

### `--js-cli-on-jvm`

[Internal]
Whether to run the Scala.js CLI on the JVM or using a native executable

## Scala Native options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--native`

Enable Scala Native. To show more options for Scala Native pass `--help-native`

### `--native-version`

Set the Scala Native version (0.4.9 by default).

### `--native-mode`

Set Scala Native compilation mode

### `--native-gc`

Set the Scala Native garbage collector

### `--native-clang`

Path to the Clang command

### `--native-clangpp`

Path to the Clang++ command

### `--native-linking`

Extra options passed to `clang` verbatim during linking

### `--native-linking-defaults`

[Internal]
Use default linking settings

### `--native-compile`

List of compile options

### `--native-compile-defaults`

[Internal]
Use default compile options

### `--embed-resources`

Embed resources into the Scala Native binary (can be read with the Java resources API)

## Scalac options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--scalac-option`

Aliases: `-O`, `--scala-opt`, `--scala-option`

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

## Scalac extra options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--scalac-help`

Aliases: `--help-scalac`

Show help for scalac. This is an alias for --scalac-option -help

### `--scalac-verbose`

Aliases: `--verbose-scalac`

Turn verbosity on for scalac. This is an alias for --scalac-option -verbose

## Secret options

Available in commands:

[`github secret list` , `gh secret list`](./commands.md#github-secret-list), [`github secret create` , `gh secret create`](./commands.md#github-secret-create)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--token`

### `--repository`

Aliases: `--repo`

## Secret create options

Available in commands:

[`github secret create` , `gh secret create`](./commands.md#github-secret-create)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--public-key`

Aliases: `--pub-key`

### `--dummy`

Aliases: `-n`

### `--print-request`

[Internal]
## Shared options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--scala-version`

Aliases: `-S`, `--scala`

Set the Scala version (3.2.2 by default)

### `--scala-binary-version`

Aliases: `-B`, `--scala-bin`, `--scala-binary`

[Internal]
Set the Scala binary version

### `--extra-jars`

Aliases: `--class`, `--class-path`, `--classes`, `--classpath`, `-classpath`, `-cp`, `--extra-class`, `--extra-class-path`, `--extra-classes`, `--extra-jar`, `--jar`, `--jars`

Add extra JARs and compiled classes to the class path

### `--extra-compile-only-jars`

Aliases: `--compile-only-jar`, `--compile-only-jars`, `--extra-compile-only-jar`

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

### `--extra-source-jars`

Aliases: `--extra-source-jar`, `--source-jar`, `--source-jars`

Add extra source JARs

### `--resource-dirs`

Aliases: `--resource-dir`

Add a resource directory

### `--platform`

Specify platform

### `--scala-library`

[Internal]
### `--java`

[Internal]
Do not add dependency to Scala Standard library. This is useful, when Scala CLI works with pure Java projects.

### `--runner`

[Internal]
Should include Scala CLI runner on the runtime ClassPath. Runner is added by default for application running on JVM using standard Scala versions. Runner is used to make stack traces more readable in case of application failure.

### `--semantic-db`

[Internal]
Generate SemanticDBs

### `--add-stubs`

[Internal]
Add dependency for stubs needed to make $ivy and $dep imports to work.

### `--strict-bloop-json-check`

[Internal]
### `--compilation-output`

Aliases: `--compile-out`, `--compile-output`, `-d`, `--destination`, `--output-directory`

Copy compilation results to output directory using either relative or absolute path

## Snippet options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--script-snippet`

Allows to execute a passed string as a Scala script

### `--execute-script`

Aliases: `-e`, `--execute-sc`, `--execute-scala-script`

[Internal]
A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

### `--scala-snippet`

Allows to execute a passed string as Scala code

### `--execute-scala`

[Internal]
A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

### `--java-snippet`

Allows to execute a passed string as Java code

### `--execute-java`

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

### `--markdown-snippet`

Aliases: `--md-snippet`

[experimental] Allows to execute a passed string as Markdown code

### `--execute-markdown`

Aliases: `--execute-md`

[Internal]
[experimental] A synonym to --markdown-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

## Test options

Available in commands:

[`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--test-framework`

Name of the test framework's runner class to use while running tests

### `--require-tests`

Fail if no test suites were run

### `--test-only`

Specify a glob pattern to filter the tests suite to be run.

## Uninstall options

Available in commands:

[`uninstall`](./commands.md#uninstall)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--force`

Aliases: `-f`

Force scala-cli uninstall

### `--skip-cache`

[Internal]
Don't clear scala-cli cache

### `--binary-name`

[Internal]
Binary name

### `--bin-dir`

[Internal]
Binary directory

## Uninstall completions options

Available in commands:

[`uninstall`](./commands.md#uninstall), [`uninstall completions` , `uninstall-completions`](./commands.md#uninstall-completions)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--rc-file`

Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell

### `--banner`

[Internal]
Custom banner in comment placed in rc file

### `--name`

[Internal]
Custom completions name

## Update options

Available in commands:

[`update`](./commands.md#update)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--binary-name`

[Internal]
Binary name

### `--bin-dir`

[Internal]
Binary directory

### `--force`

Aliases: `-f`

Force update scala-cli if is outdated

### `--is-internal-run`

[Internal]
### `--gh-token`

[Internal]
A github token used to access GitHub. Not needed in most cases.

## Verbosity options

Available in commands:

[`add-path`](./commands.md#add-path), [`bloop`](./commands.md#bloop), [`bloop exit`](./commands.md#bloop-exit), [`bloop output`](./commands.md#bloop-output), [`bloop start`](./commands.md#bloop-start), [`bsp`](./commands.md#bsp), [`clean`](./commands.md#clean), [`compile`](./commands.md#compile), [`config`](./commands.md#config), [`default-file`](./commands.md#default-file), [`dependency-update`](./commands.md#dependency-update), [`directories`](./commands.md#directories), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`help`](./commands.md#help), [`install completions` , `install-completions`](./commands.md#install-completions), [`install-home`](./commands.md#install-home), [`github secret list` , `gh secret list`](./commands.md#github-secret-list), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`pgp pull`](./commands.md#pgp-pull), [`pgp push`](./commands.md#pgp-push), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`publish setup`](./commands.md#publish-setup), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`github secret create` , `gh secret create`](./commands.md#github-secret-create), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test), [`uninstall`](./commands.md#uninstall), [`uninstall completions` , `uninstall-completions`](./commands.md#uninstall-completions), [`update`](./commands.md#update), [`version`](./commands.md#version)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--verbose`

Aliases: `-v`

Increase verbosity (can be specified multiple times)

### `--interactive`

Aliases: `-i`

Interactive mode

### `--actions`

Enable actionable diagnostics

## Version options

Available in commands:

[`version`](./commands.md#version)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--cli-version`

Aliases: `--cli`

Show plain Scala CLI version only

### `--scala-version`

Aliases: `--scala`

Show plain Scala version only

### `--gh-token`

[Internal]
A github token used to access GitHub. Not needed in most cases.

### `--offline`

Don't check for the newest available Scala CLI version upstream

## Watch options

Available in commands:

[`compile`](./commands.md#compile), [`package`](./commands.md#package), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--watch`

Aliases: `-w`

Watch source files for changes

### `--restart`

Aliases: `--revolver`

Run your application in background and automatically restart if sources have been changed

## Internal options 
### Add path options

Available in commands:

[`add-path`](./commands.md#add-path)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--title`

[Internal]
### Bloop options

Available in commands:

[`bloop`](./commands.md#bloop)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--working-directory`

Aliases: `--dir`, `--working-dir`

[Internal]
### Bloop start options

Available in commands:

[`bloop start`](./commands.md#bloop-start)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--force`

Aliases: `-f`

[Internal]
### Bsp options

Available in commands:

[`bsp`](./commands.md#bsp)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--json-options`

[Internal]
Command-line options JSON file

### Bsp file options

Available in commands:

[`clean`](./commands.md#clean), [`setup-ide`](./commands.md#setup-ide)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--bsp-directory`

Aliases: `--bsp-dir`

[Internal]
Custom BSP configuration location

### `--bsp-name`

Aliases: `--name`

[Internal]
Name of BSP

### Config options

Available in commands:

[`config`](./commands.md#config)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--dump`

[Internal]
Dump config DB as JSON

### `--create-pgp-key`

[Internal]
Create PGP key in config

### `--email`

[Internal]
Email to use to create PGP key in config

### `--password`

[Internal]
If the entry is a password, print the password value rather than how to get the password

### `--password-value`

[Internal]
If the entry is a password, save the password value rather than how to get the password

### `--unset`

Aliases: `--remove`

[Internal]
Remove an entry from config

### `--https-only`

[Internal]
For repository.credentials and publish.credentials, whether these credentials should be HTTPS only (default: true)

### `--match-host`

[Internal]
For repository.credentials, whether to use these credentials automatically based on the host

### `--optional`

[Internal]
For repository.credentials, whether to use these credentials are optional

### `--pass-on-redirect`

[Internal]
For repository.credentials, whether to use these credentials should be passed upon redirection

### Coursier options

Available in commands:

[`bloop`](./commands.md#bloop), [`bloop exit`](./commands.md#bloop-exit), [`bloop start`](./commands.md#bloop-start), [`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`config`](./commands.md#config), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`pgp push`](./commands.md#pgp-push), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`publish setup`](./commands.md#publish-setup), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`github secret create` , `gh secret create`](./commands.md#github-secret-create), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test), [`uninstall`](./commands.md#uninstall)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--ttl`

[Internal]
Specify a TTL for changing dependencies, such as snapshots

### `--cache`

[Internal]
Set the coursier cache location

### `--coursier-validate-checksums`

[Internal]
Enable checksum validation of artifacts downloaded by coursier

### Default file options

Available in commands:

[`default-file`](./commands.md#default-file)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--write`

[Internal]
Write result to files rather than to stdout

### `--list`

[Internal]
List available default files

### `--list-ids`

[Internal]
List available default file ids

### `--force`

Aliases: `-f`

[Internal]
Force overwriting destination files

### Input options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`publish setup`](./commands.md#publish-setup), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--default-forbidden-directories`

[Internal]
### `--forbid`

[Internal]
### Install home options

Available in commands:

[`install-home`](./commands.md#install-home)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--scala-cli-binary-path`

[Internal]
### `--force`

Aliases: `-f`

[Internal]
Overwrite if it exists

### `--binary-name`

[Internal]
Binary name

### `--env`

[Internal]
Print the update to `env` variable

### `--bin-dir`

[Internal]
Binary directory

### Metabrowse options

Available in commands:

[`browse` , `metabrowse`](./commands.md#browse)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--add-rt-jar`

[Internal]
Download and add `rt.jar` in the class path

### `--host`

Aliases: `-H`

[Internal]
Bind to host

### `--port`

Aliases: `-p`

[Internal]
Bind to port

### `--os-arch-suffix`

[Internal]
### `--metabrowse-tag`

[Internal]
### `--metabrowse-github-org-name`

[Internal]
### `--metabrowse-extension`

[Internal]
### `--metabrowse-launcher`

[Internal]
### `--metabrowse-dialect`

[Internal]
### Pgp create options

Available in commands:

[`pgp create`](./commands.md#pgp-create)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--email`

[Internal]
### `--password`

[Internal]
### `--dest`

[Internal]
### `--pub-dest`

[Internal]
### `--secret-dest`

[Internal]
### `--verbose`

[Internal]
### `--quiet`

[Internal]
### Pgp key id options

Available in commands:

[`pgp key-id`](./commands.md#pgp-key-id)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--fingerprint`

[Internal]
### `--verbose`

Aliases: `-v`

[Internal]
### Pgp pull options

Available in commands:

[`pgp pull`](./commands.md#pgp-pull)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--allow-empty`

[Internal]
Whether to exit with code 0 if no key is passed

### Pgp push options

Available in commands:

[`pgp push`](./commands.md#pgp-push)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--force`

Aliases: `-f`

[Internal]
Try to push the key even if Scala CLI thinks it's not a public key

### `--allow-empty`

[Internal]
Whether to exit with code 0 if no key is passed

### `--force-signing-binary`

[Internal]
### Pgp scala signing options

Available in commands:

[`config`](./commands.md#config), [`pgp push`](./commands.md#pgp-push), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`publish setup`](./commands.md#publish-setup)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--signing-cli-version`

[Internal]
### `--signing-cli-java-arg`

[Internal]
### `--force-jvm-signing-cli`

[Internal]
Whether to run the Scala Signing CLI on the JVM or using a native executable

### Pgp sign options

Available in commands:

[`pgp sign`](./commands.md#pgp-sign)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--password`

[Internal]
### `--secret-key`

[Internal]
### `--force`

Aliases: `-f`

[Internal]
### `--stdout`

[Internal]
### Pgp verify options

Available in commands:

[`pgp verify`](./commands.md#pgp-verify)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--key`

[Internal]
### Setup IDE options

Available in commands:

[`setup-ide`](./commands.md#setup-ide)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--charset`

[Internal]
### Workspace options

Available in commands:

[`bsp`](./commands.md#bsp), [`clean`](./commands.md#clean), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`export`](./commands.md#export), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`browse` , `metabrowse`](./commands.md#browse), [`package`](./commands.md#package), [`publish`](./commands.md#publish), [`publish local`](./commands.md#publish-local), [`publish setup`](./commands.md#publish-setup), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--workspace`

[Internal]
Directory where .scala-build is written

