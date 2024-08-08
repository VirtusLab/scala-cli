---
title: Environment variables
sidebar_position: 7
---

Scala CLI uses environment variables to configure its behavior.
Below you can find a list of environment variables used and recognized by Scala CLI.

However, it should by no means be treated as an exhaustive list.
Some tools and libraries Scala CLI integrates with may have their own, which may or may not be listed here.


## Scala CLI
  - `SCALA_CLI_CONFIG`: Scala CLI configuration file path
  - `SCALA_CLI_HOME`: Scala CLI home directory
  - `SCALA_CLI_INTERACTIVE`: Interactive mode toggle
  - `SCALA_CLI_INTERACTIVE_INPUTS`: Interactive mode inputs
  - `SCALA_CLI_POWER`: Power mode toggle
  - `SCALA_CLI_PRINT_STACK_TRACES`: Print stack traces toggle
  - `SCALA_CLI_SODIUM_JNI_ALLOW`: Allow to load libsodiumjni
  - `SCALA_CLI_VENDORED_ZIS`: Toggle io.github.scala_cli.zip.ZipInputStream

## Java
  - `JAVA_HOME`: Java installation directory
  - `JAVA_OPTS`: Java options
  - `JDK_JAVA_OPTIONS`: JDK Java options

## Bloop
  - `BLOOP_COMPUTATION_CORES`: ⚡ Number of computation cores to be used
  - `BLOOP_DAEMON_DIR`: ⚡ Bloop daemon directory
  - `BLOOP_JAVA_OPTS`: ⚡ Bloop Java options
  - `BLOOP_MODULE`: ⚡ Bloop default module
  - `BLOOP_PORT`: ⚡ Bloop default port
  - `BLOOP_SCALA_VERSION`: ⚡ Bloop default Scala version
  - `BLOOP_VERSION`: ⚡ Bloop default version
  - `BLOOP_SERVER`: ⚡ Bloop default host
  - `SCALA_CLI_EXTRA_TIMEOUT`: ⚡ Extra timeout

## Coursier
  - `COURSIER_BIN_DIR`: Coursier app binaries directory
  - `COURSIER_CACHE`: Coursier cache location
  - `COURSIER_CONFIG_DIR`: Coursier configuration directory
  - `COURSIER_CREDENTIALS`: Coursier credentials
  - `INSIDE_EMACS`: Emacs toggle
  - `COURSIER_EXPERIMENTAL`: Experimental mode toggle
  - `COURSIER_JNI`: Coursier JNI toggle
  - `COURSIER_MODE`: Coursier mode (can be set to 'offline')
  - `COURSIER_NO_TERM`: Terminal toggle
  - `COURSIER_PROGRESS`: Progress bar toggle
  - `COURSIER_REPOSITORIES`: Coursier repositories
  - `COURSIER_VENDORED_ZIS`: Toggle io.github.scala_cli.zip.ZipInputStream
  - `CS_MAVEN_HOME`: Coursier Maven home directory

## Spark
  - `SPARK_HOME`: ⚡ Spark installation directory

## Miscellaneous
  - `PATH`: The app path variable
  - `DYLD_LIBRARY_PATH`: Runtime library paths on Mac OS X
  - `LD_LIBRARY_PATH`: Runtime library paths on Linux
  - `PATHEXT`: Executable file extensions on Windows
  - `SHELL`: The currently used shell
  - `VCVARSALL`: Visual C++ Redistributable Runtimes
  - `ZDOTDIR`: Zsh configuration directory

## Internal
  - `CI`: ⚡ Marker for running on the CI

