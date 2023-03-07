---
title: Commands
sidebar_position: 3
---




## clean

Clean the workspace.

Passed inputs will establish the Scala CLI project, for which the workspace will be cleaned.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/clean

Accepts option groups: [bsp file](./cli-options.md#bsp-file-options), [logging](./cli-options.md#logging-options), [verbosity](./cli-options.md#verbosity-options), [workspace](./cli-options.md#workspace-options)

## compile

Compile Scala code.

Specific compile configurations can be specified with both command line options and using directives defined in sources.
Command line options always take priority over using directives when a clash occurs, allowing to override configurations defined in sources.
Using directives can be defined in all supported input source file types.

Multiple inputs can be passed at once.
Paths to directories, URLs and supported file types are accepted as inputs.
Accepted file extensions: .scala, .sc, .java, .jar, .md, .jar, .c, .h, .zip
For piped inputs use the corresponding alias: _.scala, _.java, _.sc, _.md
All supported types of inputs can be mixed with each other.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/compile

Accepts option groups: [compilation server](./cli-options.md#compilation-server-options), [compile](./cli-options.md#compile-options), [coursier](./cli-options.md#coursier-options), [cross](./cli-options.md#cross-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [markdown](./cli-options.md#markdown-options), [python](./cli-options.md#python-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [suppress warning](./cli-options.md#suppress-warning-options), [verbosity](./cli-options.md#verbosity-options), [watch](./cli-options.md#watch-options), [workspace](./cli-options.md#workspace-options)

## config

Configure global settings for Scala CLI.

Syntax:
```sh
  scala-cli config key value
```
For example, to globally set the interactive mode:
```sh
  scala-cli config interactive true
```
  
Available keys:
  - actions                                        Globally enables actionable diagnostics. Enabled by default.
  - github.token                                   GitHub token.
  - httpProxy.address                              HTTP proxy address.
  - httpProxy.password                             HTTP proxy password (used for authentication).
  - httpProxy.user                                 HTTP proxy user (used for authentication).
  - interactive                                    Globally enables interactive mode (the '--interactive' flag).
  - interactive-was-suggested                      Setting indicating if the global interactive mode was already suggested.
  - pgp.public-key                                 The PGP public key, used for signing.
  - pgp.secret-key                                 The PGP secret key, used for signing.
  - pgp.secret-key-password                        The PGP secret key password, used for signing.
  - power                                          Globally enables power mode (the '--power' launcher flag).
  - publish.credentials                            Publishing credentials, syntax: s1.oss.sonatype.org value:user value:password
  - publish.user.email                             The 'email' user detail, used for publishing.
  - publish.user.name                              The 'name' user detail, used for publishing.
  - publish.user.url                               The 'url' user detail, used for publishing.
  - repositories.credentials                       Repository credentials, syntax: value:user value:password
  - repositories.default                           Default repository, syntax: https://first-repo.company.com https://second-repo.company.com
  - repositories.mirrors                           Repository mirrors, syntax: repositories.mirrors maven:*=https://repository.company.com/maven
  - suppress-warning.directives-in-multiple-files  Globally suppresses warnings about directives declared in multiple source files.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/misc/config

Accepts option groups: [config](./cli-options.md#config-options), [coursier](./cli-options.md#coursier-options), [debug](./cli-options.md#debug-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [pgp scala signing](./cli-options.md#pgp-scala-signing-options), [verbosity](./cli-options.md#verbosity-options)

## dependency-update

Update dependency directives in the project

Accepts option groups: [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [dependency update](./cli-options.md#dependency-update-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [markdown](./cli-options.md#markdown-options), [python](./cli-options.md#python-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [suppress warning](./cli-options.md#suppress-warning-options), [verbosity](./cli-options.md#verbosity-options), [workspace](./cli-options.md#workspace-options)

## doc

Generate Scaladoc documentation.

Multiple inputs can be passed at once.
Paths to directories, URLs and supported file types are accepted as inputs.
Accepted file extensions: .scala, .sc, .java, .jar, .md, .jar, .c, .h, .zip
For piped inputs use the corresponding alias: _.scala, _.java, _.sc, _.md
All supported types of inputs can be mixed with each other.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/doc

Accepts option groups: [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [doc](./cli-options.md#doc-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [markdown](./cli-options.md#markdown-options), [python](./cli-options.md#python-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [suppress warning](./cli-options.md#suppress-warning-options), [verbosity](./cli-options.md#verbosity-options), [workspace](./cli-options.md#workspace-options)

## export

Export current project to an external build tool (like SBT or Mill) or to JSON.

The whole Scala CLI project should get exported along with its dependencies configuration.

Unless otherwise configured, the default export format is SBT.

Multiple inputs can be passed at once.
Paths to directories, URLs and supported file types are accepted as inputs.
Accepted file extensions: .scala, .sc, .java, .jar, .md, .jar, .c, .h, .zip
For piped inputs use the corresponding alias: _.scala, _.java, _.sc, _.md
All supported types of inputs can be mixed with each other.

Detailed documentation can be found on our website: https://scala-cli.virtuslab.org

Accepts option groups: [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [export](./cli-options.md#export-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [main class](./cli-options.md#main-class-options), [markdown](./cli-options.md#markdown-options), [python](./cli-options.md#python-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [suppress warning](./cli-options.md#suppress-warning-options), [verbosity](./cli-options.md#verbosity-options), [workspace](./cli-options.md#workspace-options)

## fmt

Aliases: `format`, `scalafmt`

Formats Scala code.

`scalafmt` is used to perform the formatting under the hood.

The `.scalafmt.conf` configuration file is optional.
Default configuration values will be assumed by Scala CLI.

All standard Scala CLI inputs are accepted, but only Scala sources will be formatted (.scala and .sc files).

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/fmt

Accepts option groups: [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [fmt](./cli-options.md#fmt-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [markdown](./cli-options.md#markdown-options), [python](./cli-options.md#python-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [suppress warning](./cli-options.md#suppress-warning-options), [verbosity](./cli-options.md#verbosity-options), [workspace](./cli-options.md#workspace-options)

## help

Print help message

Accepts option groups: [logging](./cli-options.md#logging-options), [verbosity](./cli-options.md#verbosity-options)

## install completions

Aliases: `install-completions`

Installs Scala CLI completions into your shell

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/completions

Accepts option groups: [install completions](./cli-options.md#install-completions-options), [logging](./cli-options.md#logging-options), [verbosity](./cli-options.md#verbosity-options)

## repl

Aliases: `console`

Fire-up a Scala REPL.

The entire Scala CLI project's classpath is loaded to the repl.

Specific repl configurations can be specified with both command line options and using directives defined in sources.
Command line options always take priority over using directives when a clash occurs, allowing to override configurations defined in sources.
Using directives can be defined in all supported input source file types.

Multiple inputs can be passed at once.
Paths to directories, URLs and supported file types are accepted as inputs.
Accepted file extensions: .scala, .sc, .java, .jar, .md, .jar, .c, .h, .zip
For piped inputs use the corresponding alias: _.scala, _.java, _.sc, _.md
All supported types of inputs can be mixed with each other.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/repl

Accepts option groups: [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [cross](./cli-options.md#cross-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [java](./cli-options.md#java-options), [java prop](./cli-options.md#java-prop-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [markdown](./cli-options.md#markdown-options), [python](./cli-options.md#python-options), [repl](./cli-options.md#repl-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [suppress warning](./cli-options.md#suppress-warning-options), [verbosity](./cli-options.md#verbosity-options), [watch](./cli-options.md#watch-options), [workspace](./cli-options.md#workspace-options)

## package

Compile and package Scala code.

Specific package configurations can be specified with both command line options and using directives defined in sources.
Command line options always take priority over using directives when a clash occurs, allowing to override configurations defined in sources.
Using directives can be defined in all supported input source file types.

Multiple inputs can be passed at once.
Paths to directories, URLs and supported file types are accepted as inputs.
Accepted file extensions: .scala, .sc, .java, .jar, .md, .jar, .c, .h, .zip
For piped inputs use the corresponding alias: _.scala, _.java, _.sc, _.md
All supported types of inputs can be mixed with each other.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/package

Accepts option groups: [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [cross](./cli-options.md#cross-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [java](./cli-options.md#java-options), [java prop](./cli-options.md#java-prop-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [main class](./cli-options.md#main-class-options), [markdown](./cli-options.md#markdown-options), [package](./cli-options.md#package-options), [packager](./cli-options.md#packager-options), [python](./cli-options.md#python-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [suppress warning](./cli-options.md#suppress-warning-options), [verbosity](./cli-options.md#verbosity-options), [watch](./cli-options.md#watch-options), [workspace](./cli-options.md#workspace-options)

## publish

Publishes build artifacts to Maven repositories.

We recommend running the `publish setup` sub-command once prior to
running `publish` in order to set missing `using` directives for publishing.
(but this is not mandatory)
    scala-cli --power publish setup .

Specific publish configurations can be specified with both command line options and using directives defined in sources.
Command line options always take priority over using directives when a clash occurs, allowing to override configurations defined in sources.
Using directives can be defined in all supported input source file types.

Multiple inputs can be passed at once.
Paths to directories, URLs and supported file types are accepted as inputs.
Accepted file extensions: .scala, .sc, .java, .jar, .md, .jar, .c, .h, .zip
For piped inputs use the corresponding alias: _.scala, _.java, _.sc, _.md
All supported types of inputs can be mixed with each other.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/publishing/publish

Accepts option groups: [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [cross](./cli-options.md#cross-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [main class](./cli-options.md#main-class-options), [markdown](./cli-options.md#markdown-options), [pgp scala signing](./cli-options.md#pgp-scala-signing-options), [publish](./cli-options.md#publish-options), [publish params](./cli-options.md#publish-params-options), [publish repository](./cli-options.md#publish-repository-options), [python](./cli-options.md#python-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [suppress warning](./cli-options.md#suppress-warning-options), [verbosity](./cli-options.md#verbosity-options), [watch](./cli-options.md#watch-options), [workspace](./cli-options.md#workspace-options)

## publish local

Publishes build artifacts to the local Ivy2 repository.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/publishing/publish-local

Accepts option groups: [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [cross](./cli-options.md#cross-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [main class](./cli-options.md#main-class-options), [markdown](./cli-options.md#markdown-options), [pgp scala signing](./cli-options.md#pgp-scala-signing-options), [publish](./cli-options.md#publish-options), [publish params](./cli-options.md#publish-params-options), [python](./cli-options.md#python-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [suppress warning](./cli-options.md#suppress-warning-options), [verbosity](./cli-options.md#verbosity-options), [watch](./cli-options.md#watch-options), [workspace](./cli-options.md#workspace-options)

## publish setup

Configures the project for publishing.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/publishing/publish-setup

Accepts option groups: [coursier](./cli-options.md#coursier-options), [debug](./cli-options.md#debug-options), [input](./cli-options.md#input-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [pgp push pull](./cli-options.md#pgp-push-pull-options), [pgp scala signing](./cli-options.md#pgp-scala-signing-options), [publish params](./cli-options.md#publish-params-options), [publish repository](./cli-options.md#publish-repository-options), [publish setup](./cli-options.md#publish-setup-options), [verbosity](./cli-options.md#verbosity-options), [workspace](./cli-options.md#workspace-options)

## run

Compile and run Scala code.

Specific run configurations can be specified with both command line options and using directives defined in sources.
Command line options always take priority over using directives when a clash occurs, allowing to override configurations defined in sources.
Using directives can be defined in all supported input source file types.

For a run to be successful, a main method must be present on the classpath.
.sc scripts are an exception, as a main class is provided in their wrapper.

Multiple inputs can be passed at once.
Paths to directories, URLs and supported file types are accepted as inputs.
Accepted file extensions: .scala, .sc, .java, .jar, .md, .jar, .c, .h, .zip
For piped inputs use the corresponding alias: _.scala, _.java, _.sc, _.md
All supported types of inputs can be mixed with each other.

To pass arguments to the actual application, just add them after `--`, like:
```sh
  scala-cli run Main.scala AnotherSource.scala -- first-arg second-arg
```

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/run

Accepts option groups: [benchmarking](./cli-options.md#benchmarking-options), [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [cross](./cli-options.md#cross-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [java](./cli-options.md#java-options), [java prop](./cli-options.md#java-prop-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [main class](./cli-options.md#main-class-options), [markdown](./cli-options.md#markdown-options), [python](./cli-options.md#python-options), [run](./cli-options.md#run-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [suppress warning](./cli-options.md#suppress-warning-options), [verbosity](./cli-options.md#verbosity-options), [watch](./cli-options.md#watch-options), [workspace](./cli-options.md#workspace-options)

## github secret create

Aliases: `gh secret create`

Creates or updates a GitHub repository secret.
```sh
  scala-cli --power github secret create --repo repo-org/repo-name SECRET_VALUE=value:secret
```

Accepts option groups: [coursier](./cli-options.md#coursier-options), [logging](./cli-options.md#logging-options), [secret](./cli-options.md#secret-options), [secret create](./cli-options.md#secret-create-options), [verbosity](./cli-options.md#verbosity-options)

## github secret list

Aliases: `gh secret list`

Lists secrets for a given GitHub repository.

Accepts option groups: [logging](./cli-options.md#logging-options), [secret](./cli-options.md#secret-options), [verbosity](./cli-options.md#verbosity-options)

## setup-ide

Generates a BSP file that you can import into your IDE.

The setup-ide sub-command allows to pre-configure a Scala CLI project to import to an IDE with BSP support.
It is also ran implicitly when `compile`, `run`, `shebang` or `test` sub-commands are called.

The pre-configuration should be saved in a BSP json connection file under the path:
```sh
    {project-root}/.bsp/scala-cli.json
```

Specific setup-ide configurations can be specified with both command line options and using directives defined in sources.
Command line options always take priority over using directives when a clash occurs, allowing to override configurations defined in sources.
Using directives can be defined in all supported input source file types.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/setup-ide

Accepts option groups: [bsp file](./cli-options.md#bsp-file-options), [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [markdown](./cli-options.md#markdown-options), [python](./cli-options.md#python-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [setup IDE](./cli-options.md#setup-ide-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [suppress warning](./cli-options.md#suppress-warning-options), [verbosity](./cli-options.md#verbosity-options), [workspace](./cli-options.md#workspace-options)

## shebang

Like `run`, but handier for shebang scripts.

This command is equivalent to the `run` sub-command, but it changes the way
Scala CLI parses its command-line arguments in order to be compatible
with shebang scripts.

When relying on the `run` sub-command, inputs and scala-cli options can be mixed,
while program args have to be specified after `--`
```sh
  scala-cli [command] [scala-cli_options | input]... -- [program_arguments]...
```

However, for the `shebang` sub-command, only a single input file can be set, while all scala-cli options
have to be set before the input file.
All inputs after the first are treated as program arguments, without the need for `--`
```sh
  scala-cli shebang [scala-cli_options]... input [program_arguments]...
```

Using this, it is possible to conveniently set up Unix shebang scripts. For example:
```scala
  #!/usr/bin/env -S scala-cli shebang --scala-version 2.13
  println("Hello, world")
```

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/shebang

Accepts option groups: [benchmarking](./cli-options.md#benchmarking-options), [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [cross](./cli-options.md#cross-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [java](./cli-options.md#java-options), [java prop](./cli-options.md#java-prop-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [main class](./cli-options.md#main-class-options), [markdown](./cli-options.md#markdown-options), [python](./cli-options.md#python-options), [run](./cli-options.md#run-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [suppress warning](./cli-options.md#suppress-warning-options), [verbosity](./cli-options.md#verbosity-options), [watch](./cli-options.md#watch-options), [workspace](./cli-options.md#workspace-options)

## test

Compile and test Scala code.

Test sources are compiled separately (after the 'main' sources), and may use different dependencies, compiler options, and other configurations.
A source file is treated as a test source if:
  - it contains the `//> using target.scope "test"` directive
  - the file name ends with `.test.scala`
  - the file comes from a directory that is provided as input, and the relative path from that file to its original directory contains a `test` directory

Specific test configurations can be specified with both command line options and using directives defined in sources.
Command line options always take priority over using directives when a clash occurs, allowing to override configurations defined in sources.
Using directives can be defined in all supported input source file types.

Multiple inputs can be passed at once.
Paths to directories, URLs and supported file types are accepted as inputs.
Accepted file extensions: .scala, .sc, .java, .jar, .md, .jar, .c, .h, .zip
For piped inputs use the corresponding alias: _.scala, _.java, _.sc, _.md
All supported types of inputs can be mixed with each other.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/test

Accepts option groups: [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [cross](./cli-options.md#cross-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [java](./cli-options.md#java-options), [java prop](./cli-options.md#java-prop-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [markdown](./cli-options.md#markdown-options), [python](./cli-options.md#python-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [suppress warning](./cli-options.md#suppress-warning-options), [test](./cli-options.md#test-options), [verbosity](./cli-options.md#verbosity-options), [watch](./cli-options.md#watch-options), [workspace](./cli-options.md#workspace-options)

## uninstall

Uninstalls Scala CLI.
Works only when installed with the installation script.
For detailed installation instructions refer to our website: https://scala-cli.virtuslab.org/install

Accepts option groups: [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [logging](./cli-options.md#logging-options), [uninstall](./cli-options.md#uninstall-options), [uninstall completions](./cli-options.md#uninstall-completions-options), [verbosity](./cli-options.md#verbosity-options)

## uninstall completions

Aliases: `uninstall-completions`

Uninstalls Scala CLI completions from your shell.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/completions

Accepts option groups: [logging](./cli-options.md#logging-options), [uninstall completions](./cli-options.md#uninstall-completions-options), [verbosity](./cli-options.md#verbosity-options)

## update

Updates Scala CLI.
Works only when installed with the installation script.
If Scala CLI was installed with an external tool, refer to its update methods.

For detailed installation instructions refer to our website: https://scala-cli.virtuslab.org/install

Accepts option groups: [logging](./cli-options.md#logging-options), [update](./cli-options.md#update-options), [verbosity](./cli-options.md#verbosity-options)

## version

Prints the version of the Scala CLI and the default version of Scala. (which can be overridden in the project)
If network connection is available, this sub-command also checks if the installed Scala CLI is up-to-date.

The version of the Scala CLI is the version of the command-line tool that runs Scala programs, which
is distinct from the Scala version of the compiler. We recommend to specify the version of the Scala compiler
for a project in its sources (via a using directive). Otherwise, Scala CLI falls back to the default
Scala version defined by the runner.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/version

Accepts option groups: [logging](./cli-options.md#logging-options), [verbosity](./cli-options.md#verbosity-options), [version](./cli-options.md#version-options)

## Hidden commands

### add-path

Add entries to the PATH environment variable.

Accepts option groups: [add path](./cli-options.md#add-path-options), [logging](./cli-options.md#logging-options), [verbosity](./cli-options.md#verbosity-options)

### bloop

Interact with Bloop (the build server) or check its status.

This sub-command allows to check the current status of Bloop.
If Bloop isn't currently running, it will be started.

Bloop is the build server used by Scala CLI.
For more information about Bloop, refer to https://scalacenter.github.io/bloop/

Accepts option groups: [bloop](./cli-options.md#bloop-options), [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [debug](./cli-options.md#debug-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [verbosity](./cli-options.md#verbosity-options)

### bloop exit

Stop Bloop if an instance is running.

Bloop is the build server used by Scala CLI.
For more information about Bloop, refer to https://scalacenter.github.io/bloop/

Accepts option groups: [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [logging](./cli-options.md#logging-options), [verbosity](./cli-options.md#verbosity-options)

### bloop output

Print Bloop output.

Bloop is the build server used by Scala CLI.
For more information about Bloop, refer to https://scalacenter.github.io/bloop/

Accepts option groups: [compilation server](./cli-options.md#compilation-server-options), [logging](./cli-options.md#logging-options), [verbosity](./cli-options.md#verbosity-options)

### bloop start

Starts a Bloop instance, if none is running.

Bloop is the build server used by Scala CLI.
For more information about Bloop, refer to https://scalacenter.github.io/bloop/

Accepts option groups: [bloop start](./cli-options.md#bloop-start-options), [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [debug](./cli-options.md#debug-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [verbosity](./cli-options.md#verbosity-options)

### bsp

Start BSP server.

BSP stands for Build Server Protocol.
For more information refer to https://build-server-protocol.github.io/

This sub-command is not designed to be used by a human.
It is normally supposed to be invoked by your IDE when a Scala CLI project is imported.

Detailed documentation can be found on our website: https://scala-cli.virtuslab.org

Accepts option groups: [bsp](./cli-options.md#bsp-options), [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [markdown](./cli-options.md#markdown-options), [python](./cli-options.md#python-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [suppress warning](./cli-options.md#suppress-warning-options), [verbosity](./cli-options.md#verbosity-options), [workspace](./cli-options.md#workspace-options)

### default-file

Generates default files for a Scala CLI project (i.e. .gitignore).

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/misc/default-file

Accepts option groups: [default file](./cli-options.md#default-file-options), [logging](./cli-options.md#logging-options), [verbosity](./cli-options.md#verbosity-options)

### directories

Prints directories used by Scala CLI.

Accepts option groups: [logging](./cli-options.md#logging-options), [verbosity](./cli-options.md#verbosity-options)

### install-home

Install Scala CLI in a sub-directory of the home directory

Accepts option groups: [install home](./cli-options.md#install-home-options), [logging](./cli-options.md#logging-options), [verbosity](./cli-options.md#verbosity-options)

### pgp pull

Accepts option groups: [logging](./cli-options.md#logging-options), [pgp pull](./cli-options.md#pgp-pull-options), [pgp push pull](./cli-options.md#pgp-push-pull-options), [verbosity](./cli-options.md#verbosity-options)

### pgp push

Accepts option groups: [coursier](./cli-options.md#coursier-options), [debug](./cli-options.md#debug-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [pgp push](./cli-options.md#pgp-push-options), [pgp push pull](./cli-options.md#pgp-push-pull-options), [pgp scala signing](./cli-options.md#pgp-scala-signing-options), [verbosity](./cli-options.md#verbosity-options)

### pgp create

Create PGP key pair

Accepts option groups: [pgp create](./cli-options.md#pgp-create-options)

### pgp key-id

Accepts option groups: [pgp key id](./cli-options.md#pgp-key-id-options)

### pgp sign

Sign files with PGP

Accepts option groups: [pgp sign](./cli-options.md#pgp-sign-options)

### pgp verify

Verify PGP signatures

Accepts option groups: [pgp verify](./cli-options.md#pgp-verify-options)

