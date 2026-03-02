---
title: Release notes
sidebar_position: 99
---
import {ChainedSnippets} from "../src/components/MarkdownComponents.js";
import ReactPlayer from 'react-player'


# Release notes

## [v1.12.4](https://github.com/VirtusLab/scala-cli/releases/tag/v1.12.4)

This is just a small patch fixing a bug ([#4152](https://github.com/VirtusLab/scala-cli/issues/4152)) breaking Metals support in Scala CLI v1.12.3.

### Fixes
* Fix BSP `buildTarget/wrappedSources` for GraalVM native image by [@Gedochao](https://github.com/Gedochao) in [#4153](https://github.com/VirtusLab/scala-cli/pull/4153)

### Documentation changes
* Fix docs' links with inconsistent routing by [@Gedochao](https://github.com/Gedochao) in [#4154](https://github.com/VirtusLab/scala-cli/pull/4154)

### Updates
* Update scala-cli.sh launcher for 1.12.3 by @github-actions[bot] in [#4151](https://github.com/VirtusLab/scala-cli/pull/4151)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.12.3...v1.12.4

## [v1.12.3](https://github.com/VirtusLab/scala-cli/releases/tag/v1.12.3)

### Change default Scala version to 3.8.2
This Scala CLI version switches the default Scala version to 3.8.2.

```bash
scala-cli version
# Scala CLI version: 1.12.3
# Scala version (default): 3.8.2
```

Added by [@Gedochao](https://github.com/Gedochao) in [#4143](https://github.com/VirtusLab/scala-cli/pull/4143)

### Fixes
* Restore Scala 2 nightlies by [@Gedochao](https://github.com/Gedochao) in [#4140](https://github.com/VirtusLab/scala-cli/pull/4140)
* Fix Scala version validation to accept locally built compiler by [@Gedochao](https://github.com/Gedochao) in [#4141](https://github.com/VirtusLab/scala-cli/pull/4141)
* Ensure Scala CLI correctly refers to the latest Scala 2.12/2.13 nightly and recovers from errors by [@Gedochao](https://github.com/Gedochao) in [#4142](https://github.com/VirtusLab/scala-cli/pull/4142)
* Restore GraalVM image support for older CPUs by [@Gedochao](https://github.com/Gedochao) in [#4150](https://github.com/VirtusLab/scala-cli/pull/4150)

### Build and internal changes
* Cut down on integration test suites by [@Gedochao](https://github.com/Gedochao) in [#4123](https://github.com/VirtusLab/scala-cli/pull/4123)
* Add `coursier/cache-action` on the CI by [@Gedochao](https://github.com/Gedochao) in [#4126](https://github.com/VirtusLab/scala-cli/pull/4126)
* Don't install Bloop separately on the CI by [@Gedochao](https://github.com/Gedochao) in [#4130](https://github.com/VirtusLab/scala-cli/pull/4130)
* Migrate to Mill 1.1.2 (was 1.0.6) by [@Gedochao](https://github.com/Gedochao) in [#4086](https://github.com/VirtusLab/scala-cli/pull/4086)

### Documentation changes
* docs: Add release notes for 1.12.2 by [@tgodzik](https://github.com/tgodzik) in [#4111](https://github.com/VirtusLab/scala-cli/pull/4111)
* Fix shebang in Scala script example by [@sake92](https://github.com/sake92) in [#4117](https://github.com/VirtusLab/scala-cli/pull/4117)
* Cherry pick #4073, #4072 & #4123 to the `stable` branch by [@Gedochao](https://github.com/Gedochao) & [@andrzejressel](https://github.com/andrzejressel) in [#4129](https://github.com/VirtusLab/scala-cli/pull/4129)
* Back port of documentation changes to main  by [@Gedochao](https://github.com/Gedochao) in [#4134](https://github.com/VirtusLab/scala-cli/pull/4134)

### Updates
* Update scala-cli.sh launcher for 1.12.2 by @github-actions[bot] in [#4116](https://github.com/VirtusLab/scala-cli/pull/4116)
* Bump webpack from 5.103.0 to 5.105.0 in /website by @dependabot[bot] in [#4119](https://github.com/VirtusLab/scala-cli/pull/4119)
* Update sbt, scripted-plugin to 1.12.2 by @scala-steward in [#4110](https://github.com/VirtusLab/scala-cli/pull/4110)
* Bump @algolia/client-search from 5.47.0 to 5.48.0 in /website by @dependabot[bot] in [#4121](https://github.com/VirtusLab/scala-cli/pull/4121)
* Bump Ammonite to 3.0.8 (was 3.0.7) by [@Gedochao](https://github.com/Gedochao) in [#4083](https://github.com/VirtusLab/scala-cli/pull/4083)
* Update scalafmt-cli_2.13, scalafmt-core to 3.10.7 by @scala-steward in [#4125](https://github.com/VirtusLab/scala-cli/pull/4125)
* Update Scala 3 Next RC to 3.8.2-RC2 by @scala-steward in [#4124](https://github.com/VirtusLab/scala-cli/pull/4124)
* Update os-lib to 0.11.8 by @scala-steward in [#4101](https://github.com/VirtusLab/scala-cli/pull/4101)
* Update munit to 1.2.2 by @scala-steward in [#4097](https://github.com/VirtusLab/scala-cli/pull/4097)
* Update semanticdb-shared_2.13 to 4.14.7 by @scala-steward in [#4103](https://github.com/VirtusLab/scala-cli/pull/4103)
* Update pprint to 0.9.6 by @scala-steward in [#4092](https://github.com/VirtusLab/scala-cli/pull/4092)
* Update asm to 9.9.1 by @scala-steward in [#4094](https://github.com/VirtusLab/scala-cli/pull/4094)
* Update semanticdb-shared_2.13 to 4.15.2 by @scala-steward in [#4132](https://github.com/VirtusLab/scala-cli/pull/4132)
* Update metaconfig-typesafe-config to 0.18.2 by @scala-steward in [#4096](https://github.com/VirtusLab/scala-cli/pull/4096)
* Update jsoniter-scala-core, ... to 2.38.8 by @scala-steward in [#4089](https://github.com/VirtusLab/scala-cli/pull/4089)
* Update scalafix-interfaces to 0.14.5 by @scala-steward in [#4088](https://github.com/VirtusLab/scala-cli/pull/4088)
* Update jsoup to 1.22.1 by @scala-steward in [#4093](https://github.com/VirtusLab/scala-cli/pull/4093)
* Bump dependencies of the docs website (dependabot sync + cleanup) by [@Gedochao](https://github.com/Gedochao) in [#4135](https://github.com/VirtusLab/scala-cli/pull/4135)
* Bump @svta/cml-utils from 1.0.1 to 1.4.0 in /website by @dependabot[bot] in [#4137](https://github.com/VirtusLab/scala-cli/pull/4137)
* Migrate to Mill 1.1.2 (was 1.0.6) by [@Gedochao](https://github.com/Gedochao) in [#4086](https://github.com/VirtusLab/scala-cli/pull/4086)
* Update Bloop to 2.0.19 (was 2.0.17) by @scala-steward in [#4109](https://github.com/VirtusLab/scala-cli/pull/4109)
* Bump Scala 3 Next RC to 3.8.2-RC3 by [@Gedochao](https://github.com/Gedochao) in [#4136](https://github.com/VirtusLab/scala-cli/pull/4136)
* Bump @algolia/client-search from 5.48.1 to 5.49.0 in /website by @dependabot[bot] in [#4145](https://github.com/VirtusLab/scala-cli/pull/4145)
* Bump @svta/cml-structured-field-values from 1.0.1 to 1.1.2 in /website by @dependabot[bot] in [#4144](https://github.com/VirtusLab/scala-cli/pull/4144)
* Bump Scala 3 Next to 3.8.2 by [@Gedochao](https://github.com/Gedochao) in [#4143](https://github.com/VirtusLab/scala-cli/pull/4143)
* Bump SBT to 1.12.4 by [@Gedochao](https://github.com/Gedochao) in [#4146](https://github.com/VirtusLab/scala-cli/pull/4146)
* Bump Scala 3 Next RC to 3.8.3-RC1 by [@Gedochao](https://github.com/Gedochao) in [#4147](https://github.com/VirtusLab/scala-cli/pull/4147)

## New Contributors
* [@sake92](https://github.com/sake92) made their first contribution in [#4117](https://github.com/VirtusLab/scala-cli/pull/4117)
* [@andrzejressel](https://github.com/andrzejressel) made their first contribution in [#4129](https://github.com/VirtusLab/scala-cli/pull/4129)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.12.2...v1.12.3

## [v1.12.2](https://github.com/VirtusLab/scala-cli/releases/tag/v1.12.2)

### Old sonatype snapshot is causing Scala CLI to timeout

Until the recent migration to new Sonatype Central repository there was an older snapshots repository in use. Unfortuntately, last week it started to make requests to it timeout and hang Scala CLI. We decided remove any references to the old repository, so older (one might say ancient) Scala CLI nightly versions will not be available. This was done by [@tgodzik](https://github.com/tgodzik) in [#4106](https://github.com/VirtusLab/scala-cli/pull/4106).

### Updates

* Bump react and @types/react in /website by @dependabot[bot] in [#4079](https://github.com/VirtusLab/scala-cli/pull/4079)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.12.1...v1.12.2

## [v1.12.1](https://github.com/VirtusLab/scala-cli/releases/tag/v1.12.1)

### Change default Scala version to 3.8.1
This Scala CLI version switches the default Scala version to 3.8.1.

```bash
scala-cli version
# Scala CLI version: 1.12.1
# Scala version (default): 3.8.1
```

Added by [@Gedochao](https://github.com/Gedochao) in [#4065](https://github.com/VirtusLab/scala-cli/pull/4065)

### Change default Scala Native version to 0.5.10
This Scala CLI version switches the default Scala Native version to 0.5.10.

```bash
scala-cli -e 'println("Hello from Scala Native 0.5.10!")' --native
# Compiling project (Scala 3.8.1, Scala Native 0.5.10)
# Compiled project (Scala 3.8.1, Scala Native 0.5.10)
# [info] Linking (multithreadingEnabled=detect) (725 ms)
# [info] Discovered 903 classes and 5554 methods after classloading
# [info] Checking intermediate code (quick) (31 ms)
# [info] Multithreading was not explicitly enabled - initial class loading has not detected any usage of system threads. Multithreading support will be disabled to improve performance.
# [info] Linking (multithreadingEnabled=false) (273 ms)
# [info] Discovered 512 classes and 2629 methods after classloading
# [info] Checking intermediate code (quick) (7 ms)
# [info] Discovered 493 classes and 2002 methods after optimization
# [info] Optimizing (debug mode) (447 ms)
# [info] Produced 12 LLVM IR files
# [info] Generating intermediate code (826 ms)
# [info] Compiling to native code (5461 ms)
# [info] Linking with [pthread, dl, m]
# [info] Linking native code (immix gc, none lto) (257 ms)
# [info] Postprocessing (0 ms)
# [info] Total (7688 ms)
# Hello from Scala Native 0.5.10!
```

Added by [@Gedochao](https://github.com/Gedochao) in [#4078](https://github.com/VirtusLab/scala-cli/pull/4078)

### Fixes
* fix: openjdk:17-slim is not available. using 17.0.2-slim instead by [@kaplan-shaked](https://github.com/kaplan-shaked) in [#4057](https://github.com/VirtusLab/scala-cli/pull/4057)

### Build and internal changes
* Fix Scala 2.13 nightly tests by [@Gedochao](https://github.com/Gedochao) in [#4063](https://github.com/VirtusLab/scala-cli/pull/4063)
* Switch the CI to GitHub Linux arm64 runners by [@Gedochao](https://github.com/Gedochao) in [#4064](https://github.com/VirtusLab/scala-cli/pull/4064)

### Updates
* Bump @algolia/client-search from 5.46.2 to 5.46.3 in /website by @dependabot[bot] in [#4062](https://github.com/VirtusLab/scala-cli/pull/4062)
* Update scala-cli.sh launcher for 1.12.0 by @github-actions[bot] in [#4054](https://github.com/VirtusLab/scala-cli/pull/4054)
* Bump `scalafmt` to 3.10.4 (was 3.10.2) by [@Gedochao](https://github.com/Gedochao) in [#4061](https://github.com/VirtusLab/scala-cli/pull/4061)
* Bump `jgit` to 7.5.0.202512021534-r (was 7.3.0.202506031305-r) by [@Gedochao](https://github.com/Gedochao) in [#4058](https://github.com/VirtusLab/scala-cli/pull/4058)
* Bump Scala Toolkit to 0.8.0 (was 0.7.0) by [@Gedochao](https://github.com/Gedochao) in [#4060](https://github.com/VirtusLab/scala-cli/pull/4060)
* Bump lodash from 4.17.21 to 4.17.23 in /website by @dependabot[bot] in [#4066](https://github.com/VirtusLab/scala-cli/pull/4066)
* Bump Scala 3 Next to 3.8.1 by [@Gedochao](https://github.com/Gedochao) in [#4065](https://github.com/VirtusLab/scala-cli/pull/4065)
* Switch from `graalvm-java17` to `graalvm-community` by [@Gedochao](https://github.com/Gedochao) in [#3459](https://github.com/VirtusLab/scala-cli/pull/3459)
* Bump Ammonite to 3.0.7 (was 3.0.6) by [@Gedochao](https://github.com/Gedochao) in [#4070](https://github.com/VirtusLab/scala-cli/pull/4070)
* Bump Scala 3 Next RC to 3.8.2-RC1 by [@Gedochao](https://github.com/Gedochao) in [#4071](https://github.com/VirtusLab/scala-cli/pull/4071)
* Bump Scala Native to 0.5.10 (was 0.5.9) by [@Gedochao](https://github.com/Gedochao) in [#4078](https://github.com/VirtusLab/scala-cli/pull/4078)

## New Contributors
* [@kaplan-shaked](https://github.com/kaplan-shaked) made their first contribution in [#4057](https://github.com/VirtusLab/scala-cli/pull/4057)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.12.0...v1.12.1

## [v1.12.0](https://github.com/VirtusLab/scala-cli/releases/tag/v1.12.0)

### Change default Scala version to 3.8.0
This Scala CLI version switches the default Scala version to 3.8.0.

```bash
scala-cli version
# Scala CLI version: 1.12.0
# Scala version (default): 3.8.0
```

Added by [@Gedochao](https://github.com/Gedochao) in [#4049](https://github.com/VirtusLab/scala-cli/pull/4049)

### Support for Scala.js 1.20.2
This Scala CLI version adds support for Scala.js 1.20.2.

```bash
scala-cli -e 'println("Hello")' --js
# Compiling project (Scala 3.8.0, Scala.js 1.20.2)
# Compiled project (Scala 3.8.0, Scala.js 1.20.2)
# Hello
```

Added by [@Gedochao](https://github.com/Gedochao) in [#4040](https://github.com/VirtusLab/scala-cli/pull/4040)

### New aliases for RC and nightly Scala versions
This Scala CLI version introduces dedicated aliases for calling the latest Scala Release Candidate versions in a given series (including LTS).

```bash
scala-cli -e 'println(scala.util.Properties.versionNumberString)' -S rc
# 3.8.1-RC1
scala-cli -e 'println(scala.util.Properties.versionNumberString)' -S 3.rc
# 3.8.1-RC1
scala-cli -e 'println(scala.util.Properties.versionNumberString)' -S 3.8.rc
# 3.8.1-RC1
scala-cli -e 'println(dotty.tools.dotc.config.Properties.simpleVersionString)' -S 3.lts.rc --with-compiler
# 3.3.7-RC2
scala-cli -e 'println(dotty.tools.dotc.config.Properties.simpleVersionString)' -S lts.rc --with-compiler
# 3.3.7-RC2
```

Dedicated default and LTS nightly aliases are also provided.

```bash
scala-cli -e 'println(scala.util.Properties.versionNumberString)' -S nightly
# 3.8.2-RC1-bin-20260115-6151803-NIGHTLY
scala-cli -e 'println(dotty.tools.dotc.config.Properties.simpleVersionString)' -S lts.nightly --with-compiler
# 3.3.8-RC1-bin-20260112-d35b2d4-NIGHTLY-git-d35b2d4
scala-cli -e 'println(dotty.tools.dotc.config.Properties.simpleVersionString)' -S 3.lts.nightly --with-compiler
# 3.3.8-RC1-bin-20260112-d35b2d4-NIGHTLY-git-d35b2d4
```

Also note that there is no easy way to identify an RC for Scala 2 / 2.12 / 2.13 (as a particular nightly serves as the RC for those Scala distributions).
A reasonable error is also provided when it is requested.

```bash fail
scala-cli -e 'println(scala.util.Properties.versionNumberString)' -S 2.rc
# Invalid Scala version: 2.rc. In the case of Scala 2, a particular nightly version serves as a release candidate.
scala-cli -e 'println(scala.util.Properties.versionNumberString)' -S 2.12.rc
# Invalid Scala version: 2.12.rc. In the case of Scala 2, a particular nightly version serves as a release candidate.
scala-cli -e 'println(scala.util.Properties.versionNumberString)' -S 2.13.rc
# Invalid Scala version: 2.13.rc. In the case of Scala 2, a particular nightly version serves as a release candidate.
```

Added by [@Gedochao](https://github.com/Gedochao) in [#4042](https://github.com/VirtusLab/scala-cli/pull/4042) and [#4016](https://github.com/VirtusLab/scala-cli/pull/4016)

### (⚡️ experimental) Support for exporting to Mill 1.0.x and overriding the Mill version with `--mill-version`
The `export` sub-command now allows to export to Mill 1.0.x projects.

```scala title=mill-1-0-6-export.scala
@main def main() = println("Let's export to Mill 1.0.6!")
```

```bash
scala-cli export mill-1-0-6-export.scala --mill --mill-version 1.0.6 --power
# Exporting to a mill project...
# Exported to: ~/mill-export/dest
```

Added by [@Gedochao](https://github.com/Gedochao) in [#4028](https://github.com/VirtusLab/scala-cli/pull/4028)

### Features & improvements
* Add `rc` & `*.rc` Scala version aliases by [@Gedochao](https://github.com/Gedochao) in [#4016](https://github.com/VirtusLab/scala-cli/pull/4016)
* Support `export`-ing to Mill 1.x.y & allow to override Mill version with `--mill-version` by [@Gedochao](https://github.com/Gedochao) in [#4028](https://github.com/VirtusLab/scala-cli/pull/4028)
* Improve Scala nightly version handling & add `lts.nightly`, `3.lts.nightly` and `nightly` Scala version tags by [@Gedochao](https://github.com/Gedochao) in [#4042](https://github.com/VirtusLab/scala-cli/pull/4042)

### Fixes
* Fix Scala 3.nightly and 3.*latest-minor*.nightly to consistently point to the same version by [@Gedochao](https://github.com/Gedochao) in [#4014](https://github.com/VirtusLab/scala-cli/pull/4014)
* fix #4005 -  Windows native-image compile failure caused by SUBST collision by [@philwalk](https://github.com/philwalk) in [#4006](https://github.com/VirtusLab/scala-cli/pull/4006)

### Documentation changes
* Solve docs' website warnings by [@Gedochao](https://github.com/Gedochao) in [#4007](https://github.com/VirtusLab/scala-cli/pull/4007)
* Back port of documentation changes to main by @github-actions[bot] in [#4015](https://github.com/VirtusLab/scala-cli/pull/4015)

### Build and internal changes
* Enable Scala Native tests with the `test` command on Scala 3 by [@Gedochao](https://github.com/Gedochao) in [#4018](https://github.com/VirtusLab/scala-cli/pull/4018)
* Self-contained docker build with ARM64 publishing by [@keynmol](https://github.com/keynmol) in [#3962](https://github.com/VirtusLab/scala-cli/pull/3962)

### Updates
* Update scala-cli.sh launcher for 1.11.0 by @github-actions[bot] in [#4004](https://github.com/VirtusLab/scala-cli/pull/4004)
* Bump Ammonite to 3.0.6 (was 3.0.5) by [@Gedochao](https://github.com/Gedochao) in [#4008](https://github.com/VirtusLab/scala-cli/pull/4008)
* Bump actions/download-artifact from 6 to 7 by @dependabot[bot] in [#4009](https://github.com/VirtusLab/scala-cli/pull/4009)
* Bump actions/upload-artifact from 5 to 6 by @dependabot[bot] in [#4010](https://github.com/VirtusLab/scala-cli/pull/4010)
* Bump sass from 1.95.0 to 1.96.0 in /website by @dependabot[bot] in [#4011](https://github.com/VirtusLab/scala-cli/pull/4011)
* Bump `react` & `react-dom` from 19.2.1 to 19.2.3 in /website by @dependabot[bot] in [#4012](https://github.com/VirtusLab/scala-cli/pull/4012)
* Bump Mill to 0.12.17 (was 0.12.16) by [@Gedochao](https://github.com/Gedochao) in [#4020](https://github.com/VirtusLab/scala-cli/pull/4020)
* Bump Scala Next RC to 3.8.0-RC4 by [@Gedochao](https://github.com/Gedochao) in [#4021](https://github.com/VirtusLab/scala-cli/pull/4021)
* Bump actions/checkout from 4 to 6 by @dependabot[bot] in [#4024](https://github.com/VirtusLab/scala-cli/pull/4024)
* Bump qs from 6.14.0 to 6.14.1 in /website by @dependabot[bot] in [#4032](https://github.com/VirtusLab/scala-cli/pull/4032)
* Bump @algolia/client-search from 5.46.0 to 5.46.2 in /website by @dependabot[bot] in [#4030](https://github.com/VirtusLab/scala-cli/pull/4030)
* Bump sass from 1.96.0 to 1.97.1 in /website by @dependabot[bot] in [#4027](https://github.com/VirtusLab/scala-cli/pull/4027)
* Bump actions/upload-artifact from 5 to 6 by @dependabot[bot] in [#4023](https://github.com/VirtusLab/scala-cli/pull/4023)
* Bump actions/download-artifact from 6 to 7 by @dependabot[bot] in [#4022](https://github.com/VirtusLab/scala-cli/pull/4022)
* Bump react-player from 2.16.1 to 3.4.0 in /website by @dependabot[bot] in [#4025](https://github.com/VirtusLab/scala-cli/pull/4025)
* Bump Scala 3 Next RC to 3.8.0-RC5 by [@Gedochao](https://github.com/Gedochao) in [#4033](https://github.com/VirtusLab/scala-cli/pull/4033)
* Migrate to Mill 1.0.6 (was 0.12.17) by [@Gedochao](https://github.com/Gedochao) in [#4019](https://github.com/VirtusLab/scala-cli/pull/4019)
* Bump libsodium to 1.0.21 (partially, except for static launchers locked at 1.0.18) by [@Gedochao](https://github.com/Gedochao) in [#4041](https://github.com/VirtusLab/scala-cli/pull/4041)
* Bump `coursier` to 2.1.25-M23 by [@Gedochao](https://github.com/Gedochao) in [#4037](https://github.com/VirtusLab/scala-cli/pull/4037)
* Bump Scala 3 Next RC to 3.8.0-RC6 by [@Gedochao](https://github.com/Gedochao) in [#4039](https://github.com/VirtusLab/scala-cli/pull/4039)
* Bump Scala.js to 1.20.2 by [@Gedochao](https://github.com/Gedochao) in [#4040](https://github.com/VirtusLab/scala-cli/pull/4040)
* Bump sass from 1.97.1 to 1.97.2 in /website by @dependabot[bot] in [#4038](https://github.com/VirtusLab/scala-cli/pull/4038)
* Bump alpine to 3.23 & relevant libsodium to 1.0.20 by [@Gedochao](https://github.com/Gedochao) in [#4047](https://github.com/VirtusLab/scala-cli/pull/4047)
* Bump @types/react from 19.2.7 to 19.2.8 in /website by @dependabot[bot] in [#4048](https://github.com/VirtusLab/scala-cli/pull/4048)
* Bump Scala 3 Next to 3.8.0 by [@Gedochao](https://github.com/Gedochao) in [#4049](https://github.com/VirtusLab/scala-cli/pull/4049)
* Bump Scala 3 Next RC to 3.8.1-RC1 by [@Gedochao](https://github.com/Gedochao) in [#4051](https://github.com/VirtusLab/scala-cli/pull/4051)
* Bump undici from 7.16.0 to 7.18.2 in /website by @dependabot[bot] in [#4050](https://github.com/VirtusLab/scala-cli/pull/4050)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.11.0...v1.12.0

## [v1.11.0](https://github.com/VirtusLab/scala-cli/releases/tag/v1.11.0)

### Change default Scala versions to 2.13.18 and 2.12.21
This Scala CLI version switches the default Scala versions:
- default Scala 2.13 to 2.13.18
- default Scala 2.12 to 2.12.21

Added by [@Gedochao](https://github.com/Gedochao) in [#3999](https://github.com/VirtusLab/scala-cli/pull/3999) and [#3958](https://github.com/VirtusLab/scala-cli/pull/3958)

### Fall back to a legacy version of the `runner` & `test-runner` modules for JVM < 17

The newest versions of the `runner` and `test-runner` modules will require Java 17 or newer to run.
When trying to use them with older JVMs, Scala CLI will now print a warning and fall back to using legacy versions of those modules instead.

```bash ignore
scala-cli test . --jvm 11
# Compiling project (Scala 3.7.4, JVM (11))
# Compiled project (Scala 3.7.4, JVM (11))
# [warn] Java 11 is no longer supported by the test-runner module.
# [warn] Defaulting to a legacy test-runner module version: 1.7.1.
# [warn] To use the latest test-runner, upgrade Java to at least 17.
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3965](https://github.com/VirtusLab/scala-cli/pull/3965)

### Features & improvements
* Improve Python detection on Mac by [@Gedochao](https://github.com/Gedochao) in [#3961](https://github.com/VirtusLab/scala-cli/pull/3961)
### Fixes
* fix for 3307 (windows launcher rejects filenames with utf8 chars) by [@philwalk](https://github.com/philwalk) in [#3923](https://github.com/VirtusLab/scala-cli/pull/3923)
* Allow for repeatable `-XX:*` Java options by [@Gedochao](https://github.com/Gedochao) in [#3976](https://github.com/VirtusLab/scala-cli/pull/3976)
* fix #3979 by adding save-and-restore to generated run-native-image.bat by [@philwalk](https://github.com/philwalk) in [#3981](https://github.com/VirtusLab/scala-cli/pull/3981)
* Fix handling of multiple exclude directives by [@tom91136](https://github.com/tom91136) in [#3984](https://github.com/VirtusLab/scala-cli/pull/3984)
* Fix doc generation for projects with compile-only dependencies by [@Gedochao](https://github.com/Gedochao) in [#3990](https://github.com/VirtusLab/scala-cli/pull/3990)
* Rewrite Scala.js linking to enable packaging of projects without a main method by [@Gedochao](https://github.com/Gedochao) in [#3992](https://github.com/VirtusLab/scala-cli/pull/3992)
### Deprecations
* Fall back to legacy `runner` / `test-runner` for JVM < 17 by [@Gedochao](https://github.com/Gedochao) in [#3965](https://github.com/VirtusLab/scala-cli/pull/3965)
### Documentation changes
* Add examples for passing REPL options via using directive to the docs by [@Gedochao](https://github.com/Gedochao) in [#3975](https://github.com/VirtusLab/scala-cli/pull/3975)
* Back port of documentation changes to main by @github-actions[bot] in [#3978](https://github.com/VirtusLab/scala-cli/pull/3978)
### Build and internal changes
* Increase CI timeout for integration tests to 150 minutes by [@Gedochao](https://github.com/Gedochao) in [#3955](https://github.com/VirtusLab/scala-cli/pull/3955)
* Run the entire REPL test suite on both Ammonite and Scala 3 REPL by [@Gedochao](https://github.com/Gedochao) in [#3982](https://github.com/VirtusLab/scala-cli/pull/3982)
* Clean cached JDKs on Linux CI runners by [@Gedochao](https://github.com/Gedochao) in [#3995](https://github.com/VirtusLab/scala-cli/pull/3995)
### Updates
* Update scala-cli.sh launcher for 1.10.1 by @github-actions[bot] in [#3954](https://github.com/VirtusLab/scala-cli/pull/3954)
* Bump sass from 1.93.3 to 1.94.0 in /website by @dependabot[bot] in [#3960](https://github.com/VirtusLab/scala-cli/pull/3960)
* Bump Scala 2.13 to 2.13.18 (was 2.13.17) by [@Gedochao](https://github.com/Gedochao) in [#3958](https://github.com/VirtusLab/scala-cli/pull/3958)
* Bump `ammonite` to 3.0.4 (was 3.0.3) by [@Gedochao](https://github.com/Gedochao) in [#3969](https://github.com/VirtusLab/scala-cli/pull/3969)
* Bump actions/checkout from 5 to 6 by @dependabot[bot] in [#3973](https://github.com/VirtusLab/scala-cli/pull/3973)
* Bump sass from 1.94.0 to 1.94.2 in /website by @dependabot[bot] in [#3974](https://github.com/VirtusLab/scala-cli/pull/3974)
* Bump misc dependencies by [@Gedochao](https://github.com/Gedochao) in [#3971](https://github.com/VirtusLab/scala-cli/pull/3971)
* Bump Scala 3 Next RC to 3.8.0-RC1 by [@Gedochao](https://github.com/Gedochao) in [#3957](https://github.com/VirtusLab/scala-cli/pull/3957)
* Update `bloop` to 2.0.17 (was 2.0.15) and `bloop-config` to 2.3.3 (was 2.3.2) by [@Gedochao](https://github.com/Gedochao) in [#3970](https://github.com/VirtusLab/scala-cli/pull/3970)
* Bump node-forge from 1.3.1 to 1.3.2 in /website by @dependabot[bot] in [#3980](https://github.com/VirtusLab/scala-cli/pull/3980)
* Bump Scala 3 Next RC to 3.8.0-RC2 by [@Gedochao](https://github.com/Gedochao) in [#3977](https://github.com/VirtusLab/scala-cli/pull/3977)
* Bump Scala.js deps by [@Gedochao](https://github.com/Gedochao) in [#3983](https://github.com/VirtusLab/scala-cli/pull/3983)
* Bump mdast-util-to-hast from 13.0.2 to 13.2.1 in /website by @dependabot[bot] in [#3986](https://github.com/VirtusLab/scala-cli/pull/3986)
* Bump @easyops-cn/docusaurus-search-local from 0.52.1 to 0.52.2 in /website by @dependabot[bot] in [#3985](https://github.com/VirtusLab/scala-cli/pull/3985)
* Bump misc dependencies by [@Gedochao](https://github.com/Gedochao) in [#3987](https://github.com/VirtusLab/scala-cli/pull/3987)
* Bump `scala-js-cli` to 1.20.1.1 (was 1.20.1) by [@Gedochao](https://github.com/Gedochao) in [#3989](https://github.com/VirtusLab/scala-cli/pull/3989)
* Bump sass from 1.94.2 to 1.95.0 in /website by @dependabot[bot] in [#3998](https://github.com/VirtusLab/scala-cli/pull/3998)
* Bump `react` & `react-dom` from 19.2.0 to 19.2.1 in /website by @dependabot[bot] in [#3997](https://github.com/VirtusLab/scala-cli/pull/3997)
* Bump `coursier` to 2.1.25-M21 by [@Gedochao](https://github.com/Gedochao) in [#4000](https://github.com/VirtusLab/scala-cli/pull/4000)
* Bump `scala-cli-signing` to 0.2.13 & `coursier-publish` to 0.4.4 by [@Gedochao](https://github.com/Gedochao) in [#3988](https://github.com/VirtusLab/scala-cli/pull/3988)
* Bump Ammonite to 3.0.5 by [@Gedochao](https://github.com/Gedochao) in [#4001](https://github.com/VirtusLab/scala-cli/pull/4001)
* Bump Scala 3 Next RC to 3.8.0-RC3 by [@Gedochao](https://github.com/Gedochao) in [#3991](https://github.com/VirtusLab/scala-cli/pull/3991)
* Bump Scala 2.12 to 2.12.21 by [@Gedochao](https://github.com/Gedochao) in [#3999](https://github.com/VirtusLab/scala-cli/pull/3999)

## New Contributors
* [@tom91136](https://github.com/tom91136) made their first contribution in [#3984](https://github.com/VirtusLab/scala-cli/pull/3984)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.10.1...v1.11.0

## [v1.10.1](https://github.com/VirtusLab/scala-cli/releases/tag/v1.10.1)
This is a bugfix release, chiefly aiming to mend [#3949](https://github.com/VirtusLab/scala-cli/issues/3949), which affected several of our users.
### Fixes
* Ensure Coursier logger gets initialized while downloading JVMs by [@Gedochao](https://github.com/Gedochao) in [#3951](https://github.com/VirtusLab/scala-cli/pull/3951)
### Documentation changes
* Back port of documentation changes to main by @github-actions[bot] in [#3948](https://github.com/VirtusLab/scala-cli/pull/3948)
### Build and internal changes
* Revert some of expecty 0.17.1 workarounds by [@Gedochao](https://github.com/Gedochao) in [#3937](https://github.com/VirtusLab/scala-cli/pull/3937)
### Updates
* Bump Node to 24 for the docs website by [@Gedochao](https://github.com/Gedochao) in [#3947](https://github.com/VirtusLab/scala-cli/pull/3947)
* Update scala-cli.sh launcher for 1.10.0 by @github-actions[bot] in [#3946](https://github.com/VirtusLab/scala-cli/pull/3946)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.10.0...v1.10.1

## [v1.10.0](https://github.com/VirtusLab/scala-cli/releases/tag/v1.10.0)

### Change default Scala versions to 3.7.4 and 2.13.17
This Scala CLI version switches the default Scala versions:
- default Scala 3 to 3.7.4
- default Scala 2.13 to 2.13.17

```bash
scala-cli version
# Scala CLI version: 1.10.0
# Scala version (default): 3.7.4
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3942](https://github.com/VirtusLab/scala-cli/pull/3942) and [#3895](https://github.com/VirtusLab/scala-cli/pull/3895)

### Support for the new Scala 3.8 REPL
As per https://github.com/scala/scala3/pull/24243, Scala 3 REPL has been extracted to [a separate artifact](https://repo.scala-lang.org/ui/packages/gav:%2F%2Forg.scala-lang:scala3-repl_3/3.8.0-RC1-bin-20251101-389483e-NIGHTLY) 
in Scala 3.8, as a result of which the use of the REPL command with Scala 3.8.0-RC1-bin-20251101-389483e-NIGHTLY
or newer will require upgrading Scala CLI at least to 1.10 to work.

```bash ignore
scala-cli repl                                          
# Welcome to Scala 3.8.0-RC1-bin-20251101-389483e-NIGHTLY (23.0.1, Java OpenJDK 64-Bit Server VM).
# Type in expressions for evaluation. Or try :help.
#                                                                                                                  
# scala> 
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3936](https://github.com/VirtusLab/scala-cli/pull/3936)

### Support for adding extra directories to a Docker image
This feature adds the ability to include additional directories in Docker images. 
Users can now specify extra directories to be copied into a Docker image during the build process.
The directories can be passed with the `--docker-extra-directories` command line option or `//> using packaging.dockerExtraDirectories` directive.

```scala compile power
//> using packaging.dockerExtraDirectories path/to/directory1 path/to/directory2
```

```bash ignore
scala-cli --power package . --docker --docker-image-repository repo --docker-extra-directories path/to/directory
```
Added by [@btomala](https://github.com/btomala) and [@Gedochao](https://github.com/Gedochao) in [VirtusLab/scala-packager#250]https://github.com/VirtusLab/scala-packager/pull/250 and [#3908](https://github.com/VirtusLab/scala-cli/pull/3908)

### Deprecate support for building GraalVM native images with Scala pre-3.3
When building GraalVM native images with Scala CLI and Scala versions older than 3.3.0, the following warning will now be printed:
```bash ignore
# [warning] building native images with Scala 3 older than 3.3.0 is deprecated.
# [warning] support will be dropped in a future Scala CLI version.
# [warning] it is advised to upgrade to a more recent Scala version
```
While the native images will still be built, the functionality will be removed in a future Scala CLI version.
It is advised to migrate projects to Scala 3.3 or newer.

Additionally, the following modules have been dropped and will no longer be published:
- `scala3-runtime`
- `scala3-graal`
- `scala3-graal-processor`

As they remain necessary for building native images for Scala pre-3.3 projects, 
their usage has been deprecated and frozen at respective version 1.9.1.

Added by [@Gedochao](https://github.com/Gedochao) in [#3929](https://github.com/VirtusLab/scala-cli/pull/3929)

### Stop publishing certain modules for Scala 2
While it is technically an internal change, it is worth noting certain Scala CLI modules will no longer be published for Scala 2.
Those include:
- `runner`
- `test-runner`
- `tasty-lib`
- `config`
- `specification-level`

From this point on, they will only be published for Scala 3.

Added by [@Gedochao](https://github.com/Gedochao) in [#3911](https://github.com/VirtusLab/scala-cli/pull/3911) and [#3912](https://github.com/VirtusLab/scala-cli/pull/3912)

### Features
* Bump `scala-packager` to 0.2.1 & enable adding extra directories to a docker image by [@Gedochao](https://github.com/Gedochao) & [@btomala](https://github.com/btomala) in [#3908](https://github.com/VirtusLab/scala-cli/pull/3908)
* Add support for the new Scala 3.8 REPL by [@Gedochao](https://github.com/Gedochao) in [#3936](https://github.com/VirtusLab/scala-cli/pull/3936)
### Fixes
* Ensure non-self executable JVM launchers' `setup-ide` produces working BSP connection JSON by [@Gedochao](https://github.com/Gedochao) in [#3876](https://github.com/VirtusLab/scala-cli/pull/3876)
* Fix test scope resources to not be added to the main scope by [@Gedochao](https://github.com/Gedochao) in [#3898](https://github.com/VirtusLab/scala-cli/pull/3898)
### Documentation changes
* Suggest using ivy2Local in the documentation by [@przemek-pokrywka](https://github.com/przemek-pokrywka) in [#3902](https://github.com/VirtusLab/scala-cli/pull/3902)
### Build and internal changes
* Run the default (Scala 3 Next) suite with the JVM bootstrapped launcher on the CI by [@Gedochao](https://github.com/Gedochao) in [#3872](https://github.com/VirtusLab/scala-cli/pull/3872)
* Run JDK tests for Java 25 by [@Gedochao](https://github.com/Gedochao) in [#3874](https://github.com/VirtusLab/scala-cli/pull/3874)
* Update MacOS CI by [@Gedochao](https://github.com/Gedochao) in [#3885](https://github.com/VirtusLab/scala-cli/pull/3885)
* Add `.cursor` to `.gitignore` by [@Gedochao](https://github.com/Gedochao) in [#3893](https://github.com/VirtusLab/scala-cli/pull/3893)
* Unify `cli` module unit tests with consistent logging, timeouts and other settings by [@Gedochao](https://github.com/Gedochao) in [#3896](https://github.com/VirtusLab/scala-cli/pull/3896)
* Cross compile the `runner` and `test-runner` modules against Scala 3 Next versions by [@Gedochao](https://github.com/Gedochao) in [#3927](https://github.com/VirtusLab/scala-cli/pull/3927)
* Migrate integration tests to Scala 3 by [@Gedochao](https://github.com/Gedochao) in [#3926](https://github.com/VirtusLab/scala-cli/pull/3926)
* Misc unit test fixes by [@Gedochao](https://github.com/Gedochao) in [#3931](https://github.com/VirtusLab/scala-cli/pull/3931)
* Temporarily tag CLI docker image tests as flaky by [@Gedochao](https://github.com/Gedochao) in [#3939](https://github.com/VirtusLab/scala-cli/pull/3939)
* Temporarily tag CLI docker image documentation tests as flaky by [@Gedochao](https://github.com/Gedochao) in [#3940](https://github.com/VirtusLab/scala-cli/pull/3940)
* Temporarily disable some more flaky CLI docker image documentation tests by [@Gedochao](https://github.com/Gedochao) in [#3941](https://github.com/VirtusLab/scala-cli/pull/3941)
* Drop Scala 2 in `runner`, `test-runner` and `tasty-lib` modules by [@Gedochao](https://github.com/Gedochao) in [#3911](https://github.com/VirtusLab/scala-cli/pull/3911)
* Drop Scala 2 in `config` and `specification-level` modules & bump `jsoniter-scala` to 2.38.2 (was 2.13.5.2) by [@Gedochao](https://github.com/Gedochao) in [#3912](https://github.com/VirtusLab/scala-cli/pull/3912)
* NIT Fix miscellaneous warnings by [@Gedochao](https://github.com/Gedochao) in [#3913](https://github.com/VirtusLab/scala-cli/pull/3913)
* NIT Fix more miscellaneous warnings by [@Gedochao](https://github.com/Gedochao) in [#3920](https://github.com/VirtusLab/scala-cli/pull/3920)
* Drop `scala3-runtime`, `scala3-graal` & `scala3-graal-processor` & deprecate pre-Scala-3.3 native images by [@Gedochao](https://github.com/Gedochao) in [#3929](https://github.com/VirtusLab/scala-cli/pull/3929)
### Updates
* Update scala-cli.sh launcher for 1.9.1 by @github-actions[bot] in [#3871](https://github.com/VirtusLab/scala-cli/pull/3871)
* Bump sass from 1.92.1 to 1.93.0 in /website by @dependabot[bot] in [#3878](https://github.com/VirtusLab/scala-cli/pull/3878)
* Bump Mill to 0.12.16 (was 0.12.15) by [@Gedochao](https://github.com/Gedochao) in [#3881](https://github.com/VirtusLab/scala-cli/pull/3881)
* Bump Munit to 1.2.0 by [@Gedochao](https://github.com/Gedochao) in [#3883](https://github.com/VirtusLab/scala-cli/pull/3883)
* Bump Scala 3 Next RC to 3.7.4-RC1 by [@Gedochao](https://github.com/Gedochao) in [#3887](https://github.com/VirtusLab/scala-cli/pull/3887)
* Bump sass from 1.93.0 to 1.93.2 in /website by @dependabot[bot] in [#3889](https://github.com/VirtusLab/scala-cli/pull/3889)
* Bump Scala 2.13 to 2.13.17 by [@Gedochao](https://github.com/Gedochao) in [#3895](https://github.com/VirtusLab/scala-cli/pull/3895)
* Bump Node to 24 & `@docusaurus/*` to 3.9.1 by [@Gedochao](https://github.com/Gedochao) in [#3899](https://github.com/VirtusLab/scala-cli/pull/3899)
* Bump Scala 2.13 to 2.13.17 on the CI by [@Gedochao](https://github.com/Gedochao) in [#3900](https://github.com/VirtusLab/scala-cli/pull/3900)
* Bump `coursier` to 2.1.25-M19 by [@Gedochao](https://github.com/Gedochao) in [#3884](https://github.com/VirtusLab/scala-cli/pull/3884)
* Bump internal Scala version to 3.3.7 by [@Gedochao](https://github.com/Gedochao) in [#3906](https://github.com/VirtusLab/scala-cli/pull/3906)
* Bump Ammonite to 3.0.3 by [@Gedochao](https://github.com/Gedochao) in [#3909](https://github.com/VirtusLab/scala-cli/pull/3909)
* Migrate from old `coursier` APIs by [@Gedochao](https://github.com/Gedochao) in [#3910](https://github.com/VirtusLab/scala-cli/pull/3910)
* Bump actions/setup-node from 5 to 6 by @dependabot[bot] in [#3915](https://github.com/VirtusLab/scala-cli/pull/3915)
* Bump `scala-packager` to 0.2.1 & enable adding extra directories to a docker image by [@Gedochao](https://github.com/Gedochao) & [@btomala](https://github.com/btomala) in [#3908](https://github.com/VirtusLab/scala-cli/pull/3908)
* Bump Scala Native to 0.5.9 by [@Gedochao](https://github.com/Gedochao) in [#3918](https://github.com/VirtusLab/scala-cli/pull/3918)
* Bump react from 19.1.1 to 19.2.0 in /website by @dependabot[bot] in [#3904](https://github.com/VirtusLab/scala-cli/pull/3904)
* Bump `docusaurus` to 3.9.2 (was 3.9.1) by [@Gedochao](https://github.com/Gedochao) in [#3919](https://github.com/VirtusLab/scala-cli/pull/3919)
* Bump actions/download-artifact from 5 to 6 by @dependabot[bot] in [#3924](https://github.com/VirtusLab/scala-cli/pull/3924)
* Bump actions/upload-artifact from 4 to 5 by @dependabot[bot] in [#3925](https://github.com/VirtusLab/scala-cli/pull/3925)
* Bump Scala 3 Next RC to 3.7.4-RC3 by [@Gedochao](https://github.com/Gedochao) in [#3928](https://github.com/VirtusLab/scala-cli/pull/3928)
* Bump `python-native-libs` to 0.2.5 (was 0.2.4) by [@Gedochao](https://github.com/Gedochao) in [#3932](https://github.com/VirtusLab/scala-cli/pull/3932)
* Bump Scala 3 Next to 3.7.4 by [@Gedochao](https://github.com/Gedochao) in [#3942](https://github.com/VirtusLab/scala-cli/pull/3942)
* Bump expecty to 0.17.1 (was 0.17.0) by [@Gedochao](https://github.com/Gedochao) in [#3938](https://github.com/VirtusLab/scala-cli/pull/3938)
* Bump sass from 1.93.2 to 1.93.3 in /website by @dependabot[bot] in [#3935](https://github.com/VirtusLab/scala-cli/pull/3935)

## New Contributors
* [@przemek-pokrywka](https://github.com/przemek-pokrywka) made their first contribution in [#3902](https://github.com/VirtusLab/scala-cli/pull/3902)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.9.1...v1.10.0

## [v1.9.1](https://github.com/VirtusLab/scala-cli/releases/tag/v1.9.1)

### Support for Scala 3.7.3
This Scala CLI version switches the default Scala version to 3.7.3.

```bash
scala-cli version
# Scala CLI version: 1.9.1
# Scala version (default): 3.7.3
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3866](https://github.com/VirtusLab/scala-cli/pull/3866)

### Support for Scala.js 1.20.1
This Scala CLI version adds support for Scala.js 1.20.1.

```bash
scala-cli -e 'println("Hello")' --js
# Compiling project (Scala 3.7.3, Scala.js 1.20.1)
# Compiled project (Scala 3.7.3, Scala.js 1.20.1)
# Hello
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3861](https://github.com/VirtusLab/scala-cli/pull/3861) and [scala-js-cli#160](https://github.com/VirtusLab/scala-js-cli/pull/160)

### Fixes
* Fix completely broken lock during setting up local repo on Linux by [@unlsycn](https://github.com/unlsycn) in [#3846](https://github.com/VirtusLab/scala-cli/pull/3846)
* Ensure `publish` actually fails on a failed upload by [@Gedochao](https://github.com/Gedochao) in [#3853](https://github.com/VirtusLab/scala-cli/pull/3853)

### Updates
* Update scala-cli.sh launcher for 1.9.0 by @github-actions[bot] in [#3851](https://github.com/VirtusLab/scala-cli/pull/3851)
* Bump sass from 1.90.0 to 1.91.0 in /website by @dependabot[bot] in [#3839](https://github.com/VirtusLab/scala-cli/pull/3839)
* Bump Scala 3 Next RC to 3.7.3-RC3 by [@Gedochao](https://github.com/Gedochao) in [#3854](https://github.com/VirtusLab/scala-cli/pull/3854)
* Bump `react` & `react-dom` from 19.1.0 to 19.1.1 in /website by @dependabot[bot] in [#3806](https://github.com/VirtusLab/scala-cli/pull/3806)
* Bump announced Scala 3 Next RC to 3.7.3-RC3 by [@Gedochao](https://github.com/Gedochao) in [#3858](https://github.com/VirtusLab/scala-cli/pull/3858)
* Bump `jgit` to 7.3.0.202506031305-r by [@Gedochao](https://github.com/Gedochao) in [#3856](https://github.com/VirtusLab/scala-cli/pull/3856)
* Bump Scala.js to 1.20.1 by [@Gedochao](https://github.com/Gedochao) in [#3861](https://github.com/VirtusLab/scala-cli/pull/3861)
* Bump actions/setup-python from 5 to 6 by @dependabot[bot] in [#3863](https://github.com/VirtusLab/scala-cli/pull/3863)
* Bump Scala Next to 3.7.3 by [@Gedochao](https://github.com/Gedochao) in [#3866](https://github.com/VirtusLab/scala-cli/pull/3866)
* Bump sass from 1.91.0 to 1.92.1 in /website by @dependabot[bot] in [#3864](https://github.com/VirtusLab/scala-cli/pull/3864)
* Bump @mdx-js/react from 3.1.0 to 3.1.1 in /website by @dependabot[bot] in [#3865](https://github.com/VirtusLab/scala-cli/pull/3865)
* Bump actions/setup-node from 4 to 5 by @dependabot[bot] in [#3862](https://github.com/VirtusLab/scala-cli/pull/3862)
* Bump `scalafmt` to 3.9.10 by [@Gedochao](https://github.com/Gedochao) in [#3868](https://github.com/VirtusLab/scala-cli/pull/3868)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.9.0...v1.9.1

## [v1.9.0](https://github.com/VirtusLab/scala-cli/releases/tag/v1.9.0)

### Support for the new Scala 3 nightly repository
This Scala CLI version supports the new Scala 3 nightly versions repository: 
https://repo.scala-lang.org/artifactory/maven-nightlies

This means that newest Scala 3 nightly versions will become available to use with Scala CLI, 
as well as the `3.nightly` tag will now refer to the actual, newest Scala version.

As a result, Scala 3.8 features like capture checked Scala 3 library should now be available from Scala CLI.

```scala compile
//> using scala 3.8.0-RC1-bin-20250901-ca400bd-NIGHTLY
import language.experimental.captureChecking

trait File extends caps.SharedCapability:
  def count(): Int

def f(file: File): IterableOnce[Int]^{file} =
  Iterator(1)
    .map(_ + file.count())
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3838](https://github.com/VirtusLab/scala-cli/pull/3838)

### Features
* Add support for the new Scala 3 nightly repository by [@Gedochao](https://github.com/Gedochao) in [#3838](https://github.com/VirtusLab/scala-cli/pull/3838)

### Fixes
* Fix using directive with URL + query parameters by [@jgoday](https://github.com/jgoday) in [#3835](https://github.com/VirtusLab/scala-cli/pull/3835)

### Documentation changes
* Update the Scala CLI docs landing page by [@Gedochao](https://github.com/Gedochao) in [#3825](https://github.com/VirtusLab/scala-cli/pull/3825)
* Back port of documentation changes to main by @github-actions[bot] in [#3826](https://github.com/VirtusLab/scala-cli/pull/3826)

### Build and internal changes
* Prepare for Mill 1.0.x bump by [@Gedochao](https://github.com/Gedochao) in [#3833](https://github.com/VirtusLab/scala-cli/pull/3833)
* Temporarily disable flaky Spark tests on Mac CI by [@Gedochao](https://github.com/Gedochao) in [#3842](https://github.com/VirtusLab/scala-cli/pull/3842)

### Updates
* Update scala-cli.sh launcher for 1.8.5 by @github-actions[bot] in [#3824](https://github.com/VirtusLab/scala-cli/pull/3824)
* Bump actions/checkout from 4 to 5 by @dependabot[bot] in [#3827](https://github.com/VirtusLab/scala-cli/pull/3827)
* Bump sass from 1.89.2 to 1.90.0 in /website by @dependabot[bot] in [#3828](https://github.com/VirtusLab/scala-cli/pull/3828)
* Bump `case-app` to 2.1.0 by [@Gedochao](https://github.com/Gedochao) in [#3830](https://github.com/VirtusLab/scala-cli/pull/3830)
* Bump actions/download-artifact from 4 to 5 by @dependabot[bot] in [#3829](https://github.com/VirtusLab/scala-cli/pull/3829)
* Update sbt, scripted-plugin to 1.11.4 by [@scala-steward](https://github.com/scala-steward) in [#3832](https://github.com/VirtusLab/scala-cli/pull/3832)
* Update Scala 3 Next RC to 3.7.3-RC2 by [@Gedochao](https://github.com/Gedochao) in [#3834](https://github.com/VirtusLab/scala-cli/pull/3834)
* Bump `scala-packager` to 0.2.0 by [@Gedochao](https://github.com/Gedochao) in [#3843](https://github.com/VirtusLab/scala-cli/pull/3843)
* Update announced Scala 3 RC to 3.7.3-RC2 by [@Gedochao](https://github.com/Gedochao) in [#3845](https://github.com/VirtusLab/scala-cli/pull/3845)
* Update jsoup to 1.21.2 by [@scala-steward](https://github.com/scala-steward) in [#3840](https://github.com/VirtusLab/scala-cli/pull/3840)

## New Contributors
* @jgoday made their first contribution in [#3835](https://github.com/VirtusLab/scala-cli/pull/3835)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.8.5...v1.9.0

## [v1.8.5](https://github.com/VirtusLab/scala-cli/releases/tag/v1.8.5)

### Support for Scala 3.7.2
This Scala CLI version switches the default Scala version to 3.7.2.

```bash
scala-cli version
# Scala CLI version: 1.8.5
# Scala version (default): 3.7.2
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3809](https://github.com/VirtusLab/scala-cli/pull/3809)

### Add props with input paths for non-script inputs
2 new properties have been added, to provide a way to access input paths outside of `.sc` scripts:
- `scala.sources` - full paths of all inputs in the project, separated by `java.io.File.pathSeparator`
- `scala.source.names` - names of all inputs in the project, separated by `java.io.File.pathSeparator`

```scala title=PrintSources.scala compile
//PrintSources.scala
@main def main() = {
  println(sys.props("scala.sources"))
  println(sys.props("scala.source.names"))
}
```

```
~/PrintSources.scala
PrintSources.scala
```

These are meant to be an input type agnostic equivalent for the `scriptPath` method available in `.sc` scripts.

Added by [@philwalk](https://github.com/philwalk) in [#3799](https://github.com/VirtusLab/scala-cli/pull/3799)

### `sonatype:snapshots` points to the new Sonatype Central snapshots repository
It is no longer necessary to manually add https://central.sonatype.com/repository/maven-snapshots, it is added under the `sonatype:snapshots` alias (along with the old snapshot repository).
It is also added in all other contexts when snapshots should be used.

```scala compile
//> using repository sonatype:snapshots
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3797](https://github.com/VirtusLab/scala-cli/pull/3797)

### Scala CLI nightlies are available again
We once again publish Scala CLI nightlies. You can use the newest version under the `nightly` tag.
As pre-1.8.5 Scala CLI versions do not look for them on the new Sonatype Central snapshots repository, 
they will not be visible when called from earlier versions.
```bash ignore
scala-cli --cli-version nightly version
# Scala CLI version: 1.8.4-24-g8ca6c960b-SNAPSHOT
# Scala version (default): 3.7.2
```

:::info
The new Sonatype Central Portal retains snapshot versions for 90 days,
so individual nightly versions will be available to test for a limited time period.
:::

Added by [@Gedochao](https://github.com/Gedochao) in [#3818](https://github.com/VirtusLab/scala-cli/pull/3818)

### Features
* Automatically add the new Sonatype Central Portal snapshots repository when snapshots are expected to be used by [@Gedochao](https://github.com/Gedochao) in [#3797](https://github.com/VirtusLab/scala-cli/pull/3797)
* Adjust Scala CLI nightly version resolution for the new Maven Central Snapshots repository by [@Gedochao](https://github.com/Gedochao) in [#3818](https://github.com/VirtusLab/scala-cli/pull/3818)
* provide a way to access paths of source files by [@philwalk](https://github.com/philwalk) in [#3799](https://github.com/VirtusLab/scala-cli/pull/3799)
* Add more logging for publishing by [@Gedochao](https://github.com/Gedochao) in [#3813](https://github.com/VirtusLab/scala-cli/pull/3813)

### Fixes
* fix for 3789 - updated by [@philwalk](https://github.com/philwalk) in [#3794](https://github.com/VirtusLab/scala-cli/pull/3794)
* script extension can be either '.sc' or empty string by [@philwalk](https://github.com/philwalk) in [#3802](https://github.com/VirtusLab/scala-cli/pull/3802)

### Build and internal changes
* Tag tests relying on old snapshots as flaky (or remove them where applicable) by [@Gedochao](https://github.com/Gedochao) in [#3817](https://github.com/VirtusLab/scala-cli/pull/3817)
* Add `--no-fallback` to graalvm native-image configuration by [@lbialy](https://github.com/lbialy) in [#3820](https://github.com/VirtusLab/scala-cli/pull/3820)

### Updates
* Bump react-player from 2.16.0 to 2.16.1 in /website by [@dependabot[bot]](https://github.com/dependabot[bot]) in [#3784](https://github.com/VirtusLab/scala-cli/pull/3784)
* Update scala-cli.sh launcher for 1.8.4 by [@github-actions[bot]](https://github.com/github-actions[bot]) in [#3787](https://github.com/VirtusLab/scala-cli/pull/3787)
* Update zip-input-stream to 0.1.3 by [@scala-steward](https://github.com/scala-steward) in [#3792](https://github.com/VirtusLab/scala-cli/pull/3792)
* Update Scala 3 Next RC to 3.7.2-RC2 by [@scala-steward](https://github.com/scala-steward) in [#3791](https://github.com/VirtusLab/scala-cli/pull/3791)
* Update java-class-name_3 to 0.1.8 by [@scala-steward](https://github.com/scala-steward) in [#3795](https://github.com/VirtusLab/scala-cli/pull/3795)
* Update Scala 3 Next announced RC to 3.7.2-RC2 by [@Gedochao](https://github.com/Gedochao) in [#3796](https://github.com/VirtusLab/scala-cli/pull/3796)
* Update publish to 0.4.3 by [@scala-steward](https://github.com/scala-steward) in [#3801](https://github.com/VirtusLab/scala-cli/pull/3801)
* Update pprint to 0.9.1 by [@scala-steward](https://github.com/scala-steward) in [#3803](https://github.com/VirtusLab/scala-cli/pull/3803)
* Update pprint to 0.9.3 by [@scala-steward](https://github.com/scala-steward) in [#3805](https://github.com/VirtusLab/scala-cli/pull/3805)
* Bump @easyops-cn/docusaurus-search-local from 0.51.1 to 0.52.1 in /website by [@dependabot[bot]](https://github.com/dependabot[bot]) in [#3807](https://github.com/VirtusLab/scala-cli/pull/3807)
* Update fansi to 0.5.1 by [@scala-steward](https://github.com/scala-steward) in [#3804](https://github.com/VirtusLab/scala-cli/pull/3804)
* Update Scala 3 Next to 3.7.2 by [@Gedochao](https://github.com/Gedochao) in [#3809](https://github.com/VirtusLab/scala-cli/pull/3809)
* Update mill-main to 0.12.15 by [@scala-steward](https://github.com/scala-steward) in [#3814](https://github.com/VirtusLab/scala-cli/pull/3814)
* Update `scalameta` to 4.13.9 by [@scala-steward](https://github.com/scala-steward) in [#3816](https://github.com/VirtusLab/scala-cli/pull/3816)
* Update scalafmt-cli_2.13, scalafmt-core to 3.9.9 by [@scala-steward](https://github.com/scala-steward) in [#3815](https://github.com/VirtusLab/scala-cli/pull/3815)
* Update os-lib to 0.11.5 by [@scala-steward](https://github.com/scala-steward) in [#3811](https://github.com/VirtusLab/scala-cli/pull/3811)
* Set Scala 3.7.2 as the latest announced Scala 3 Next by [@Gedochao](https://github.com/Gedochao) in [#3812](https://github.com/VirtusLab/scala-cli/pull/3812)
* Bump Bloop to 2.0.13 by [@Gedochao](https://github.com/Gedochao) in [#3821](https://github.com/VirtusLab/scala-cli/pull/3821)

## New Contributors
* [@lbialy](https://github.com/lbialy) made their first contribution in [#3820](https://github.com/VirtusLab/scala-cli/pull/3820)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.8.4...v1.8.5

## [v1.8.4](https://github.com/VirtusLab/scala-cli/releases/tag/v1.8.4)

### (⚡️ experimental) `publish` support for the Sonatype Central Portal
This Scala CLI version adds support for publishing artifacts to the Sonatype Central Portal 
via its [OSSRH Staging API](https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/).
It is once again publish artifacts to Maven Central with Scala CLI.
Both stable and `*-SNAPSHOT` versions are handled.
The only configuration change necessary is to migrate the Sonatype namespace in the UI and regenerate credentials to the new Sonatype Central Portal, as [per Sonatype instructions](https://central.sonatype.org/pages/ossrh-eol/)

```bash ignore
scala-cli publish . --power
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3774](https://github.com/VirtusLab/scala-cli/pull/3774), [#3776](https://github.com/VirtusLab/scala-cli/pull/3776), [#3778](https://github.com/VirtusLab/scala-cli/pull/3778), [coursier/publish#128](https://github.com/coursier/publish/pull/128) and [coursier/publish#127](https://github.com/coursier/publish/pull/127)

### Better support for the REPL with JDK 24+
When using the REPL with JDK 24 or newer, users should no longer see the warnings about restricted methods of `java.lang.System` being used.

```bash ignore
scala-cli repl --jvm 24
# WARNING: A terminally deprecated method in sun.misc.Unsafe has been called
# WARNING: sun.misc.Unsafe::objectFieldOffset has been called by scala.runtime.LazyVals$ (file:/Users/pchabelski/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala3-library_3/3.7.1/scala3-library_3-3.7.1.jar)
# WARNING: Please consider reporting this to the maintainers of class scala.runtime.LazyVals$
# WARNING: sun.misc.Unsafe::objectFieldOffset will be removed in a future release
# Welcome to Scala 3.7.1 (24.0.1, Java OpenJDK 64-Bit Server VM).
# Type in expressions for evaluation. Or try :help.
#            
#
# scala> 
```

Note that the deprecated method from `sun.misc.Unsafe` warning is still present, and will only be addressed in Scala 3.8.0.

Added by [@Gedochao](https://github.com/Gedochao) in [#3767](https://github.com/VirtusLab/scala-cli/pull/3767)

### Features
* `publish` command with Sonatype Central Portal OSSRH Staging API by [@Gedochao](https://github.com/Gedochao) in [#3774](https://github.com/VirtusLab/scala-cli/pull/3774)
* Add support for publishing with Scala CLI to Sonatype Central Portal by [@Gedochao](https://github.com/Gedochao) in [#3776](https://github.com/VirtusLab/scala-cli/pull/3776)

### Fixes
* Prevent the REPL from warning about restricted `java.lang.System` API on JDK 24 by [@Gedochao](https://github.com/Gedochao) in [#3767](https://github.com/VirtusLab/scala-cli/pull/3767)
* fix for 3725, 3752, 3766 and 3769 by [@philwalk](https://github.com/philwalk) in [#3726](https://github.com/VirtusLab/scala-cli/pull/3726)

### Documentation changes
* Update verbosity.md by [@kubukoz](https://github.com/kubukoz) in [#3772](https://github.com/VirtusLab/scala-cli/pull/3772)

### Updates
* Update scala-cli.sh launcher for 1.8.3 by [@github-actions[bot]](https://github.com/github-actions[bot]) in [#3765](https://github.com/VirtusLab/scala-cli/pull/3765)
* Update sbt, scripted-plugin to 1.11.3 by [@scala-steward](https://github.com/scala-steward) in [#3773](https://github.com/VirtusLab/scala-cli/pull/3773)
* Update `scalafmt` to 3.9.8 by [@scala-steward](https://github.com/scala-steward) in [#3771](https://github.com/VirtusLab/scala-cli/pull/3771)
* Update scalafix-interfaces to 0.14.3 by [@scala-steward](https://github.com/scala-steward) in [#3673](https://github.com/VirtusLab/scala-cli/pull/3673)
* Bump `coursier/publish` to 0.4.2 by [@Gedochao](https://github.com/Gedochao) in [#3778](https://github.com/VirtusLab/scala-cli/pull/3778)
* Update semanticdb-shared_2.13 to 4.13.8 by [@scala-steward](https://github.com/scala-steward) in [#3770](https://github.com/VirtusLab/scala-cli/pull/3770)
* Update jimfs to 1.3.1 by [@scala-steward](https://github.com/scala-steward) in [#3779](https://github.com/VirtusLab/scala-cli/pull/3779)
* Update `scala-cli-signing` to 0.2.11 by [@scala-steward](https://github.com/scala-steward) in [#3781](https://github.com/VirtusLab/scala-cli/pull/3781)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.8.3...v1.8.4

## [v1.8.3](https://github.com/VirtusLab/scala-cli/releases/tag/v1.8.3)

This is a small release which aims to fix issues with publishing Scala CLI on Sonatype Central Portal.

### Build and internal changes
* Add extra logging on Scala CLI artifacts publishing by [@Gedochao](https://github.com/Gedochao) in [#3745](https://github.com/VirtusLab/scala-cli/pull/3745)
* [NIT] Refactor publishing by [@Gedochao](https://github.com/Gedochao) in [#3743](https://github.com/VirtusLab/scala-cli/pull/3743)
* Ensure publishing to Sonatype runs without parallelism by [@Gedochao](https://github.com/Gedochao) in [#3755](https://github.com/VirtusLab/scala-cli/pull/3755)
* Don't use any custom logic when publishing to Sonatype Central by [@Gedochao](https://github.com/Gedochao) in [#3759](https://github.com/VirtusLab/scala-cli/pull/3759)
* Print artifacts' version when publishing Scala CLI by [@Gedochao](https://github.com/Gedochao) in [#3760](https://github.com/VirtusLab/scala-cli/pull/3760)

### Updates
* Update scala-cli.sh launcher for 1.8.2 by [@github-actions](https://github.com/github-actions) in [#3744](https://github.com/VirtusLab/scala-cli/pull/3744)
* Update semanticdb-shared_2.13 to 4.13.7 by [@scala-steward](https://github.com/scala-steward) in [#3746](https://github.com/VirtusLab/scala-cli/pull/3746)
* Bump @easyops-cn/docusaurus-search-local from 0.50.0 to 0.51.0 in /website by [@dependabot](https://github.com/dependabot) in [#3748](https://github.com/VirtusLab/scala-cli/pull/3748)
* Bump brace-expansion from 1.1.11 to 1.1.12 in /website by [@dependabot](https://github.com/dependabot) in [#3749](https://github.com/VirtusLab/scala-cli/pull/3749)
* chore: Set latest RC to 3.7.2-RC1 by [@tgodzik](https://github.com/tgodzik) in [#3750](https://github.com/VirtusLab/scala-cli/pull/3750)
* Bump @easyops-cn/docusaurus-search-local from 0.51.0 to 0.51.1 in /website by [@dependabot](https://github.com/dependabot) in [#3757](https://github.com/VirtusLab/scala-cli/pull/3757)
* Update jsoup to 1.21.1 by [@scala-steward](https://github.com/scala-steward) in [#3762](https://github.com/VirtusLab/scala-cli/pull/3762)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.8.2...v1.8.3

## [v1.8.2](https://github.com/VirtusLab/scala-cli/releases/tag/v1.8.2)

:::warning
Due to technical difficulties, this version is not available on Sonatype Central Portal and on coursier. 
If you care about those installation methods, please be patient as we resolve the issue and work on a subsequent release.
:::

### Support for Scala Native 0.5.8
This Scala CLI version switches the default Scala Native version to 0.5.8.

```bash
scala-cli -e 'println("Hello from Scala Native 0.5.8!")' --native
# Compiling project (Scala 3.7.1, Scala Native 0.5.8)
# Compiled project (Scala 3.7.1, Scala Native 0.5.8)
# [info] Linking (multithreadingEnabled=true, disable if not used) (1052 ms)
# [info] Discovered 919 classes and 5640 methods after classloading
# [info] Checking intermediate code (quick) (59 ms)
# [info] Multithreading was not explicitly enabled - initial class loading has not detected any usage of system threads. Multithreading support will be disabled to improve performance.
# [info] Linking (multithreadingEnabled=false) (369 ms)
# [info] Discovered 511 classes and 2553 methods after classloading
# [info] Checking intermediate code (quick) (7 ms)
# [info] Discovered 491 classes and 1986 methods after optimization
# [info] Optimizing (debug mode) (519 ms)
# [info] Produced 9 LLVM IR files
# [info] Generating intermediate code (521 ms)
# [info] Compiling to native code (1762 ms)
# [info] Linking with [pthread, dl, m]
# [info] Linking native code (immix gc, none lto) (98 ms)
# [info] Postprocessing (0 ms)
# [info] Total (4379 ms)
# Hello from Scala Native 0.5.8!
```

Added in [#3728](https://github.com/VirtusLab/scala-cli/pull/3728)

### Internal and build changes
* Migrate publishing to Sonatype Maven Central by [@Gedochao](https://github.com/Gedochao) in [#3704](https://github.com/VirtusLab/scala-cli/pull/3704)
* Fix version string of published artifacts by [@Gedochao](https://github.com/Gedochao) in [#3721](https://github.com/VirtusLab/scala-cli/pull/3721)

### Updates
* Update scala-cli.sh launcher for 1.8.1 by [@github-actions](https://github.com/github-actions) in [#3719](https://github.com/VirtusLab/scala-cli/pull/3719)
* Bump `sbt` to 1.11.1 by [@Gedochao](https://github.com/Gedochao) in [#3723](https://github.com/VirtusLab/scala-cli/pull/3723)
* Bump deps to versions migrated to Sonatype Maven Central by [@Gedochao](https://github.com/Gedochao) in [#3722](https://github.com/VirtusLab/scala-cli/pull/3722)
* Update publish_2.13 to 0.2.1 by [@scala-steward](https://github.com/scala-steward) in [#3727](https://github.com/VirtusLab/scala-cli/pull/3727)
* Bump sass from 1.89.1 to 1.89.2 in /website by [@dependabot](https://github.com/dependabot) in [#3733](https://github.com/VirtusLab/scala-cli/pull/3733)
* Bump @easyops-cn/docusaurus-search-local from 0.49.2 to 0.50.0 in /website by [@dependabot](https://github.com/dependabot) in [#3730](https://github.com/VirtusLab/scala-cli/pull/3730)
* Bump react and react-dom in /website by [@dependabot](https://github.com/dependabot) in [#3732](https://github.com/VirtusLab/scala-cli/pull/3732)
* Bump `scala-cli-signing` to 0.2.9 & coursier `publish` to 0.3.0 by [@Gedochao](https://github.com/Gedochao) in [#3734](https://github.com/VirtusLab/scala-cli/pull/3734)
* Bump `docusaurus` to 3.8.1 by [@Gedochao](https://github.com/Gedochao) in [#3737](https://github.com/VirtusLab/scala-cli/pull/3737)
* Bump SBT to 1.11.2 by [@Gedochao](https://github.com/Gedochao) in [#3739](https://github.com/VirtusLab/scala-cli/pull/3739)
* Update Scala Native to 0.5.8 by [@scala-steward](https://github.com/scala-steward) in [#3728](https://github.com/VirtusLab/scala-cli/pull/3728)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.8.1...v1.8.2

## [v1.8.1](https://github.com/VirtusLab/scala-cli/releases/tag/v1.8.1)

### Support for Scala 3.7.1
This Scala CLI version switches the default Scala version to 3.7.1.

```bash
scala-cli version
# Scala CLI version: 1.8.1
# Scala version (default): 3.7.1
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3707](https://github.com/VirtusLab/scala-cli/pull/3707)

### Support for URLs in `using file` directives
It is now possible to use URLs in `using file` directives, which allows linking sources from the net.

```scala compile
//> using file https://raw.githubusercontent.com/softwaremill/sttp/refs/heads/master/examples/src/main/scala/sttp/client4/examples/json/GetAndParseJsonCatsEffectCirce.scala
```

Added during a [Scala Tooling Spree](https://scalameta.org/scala-tooling-spree/) by [@ivan-klass](https://github.com/ivan-klass), [@majk-p](https://github.com/majk-p) and [@tgodzik](https://github.com/tgodzik) in [#3681](https://github.com/VirtusLab/scala-cli/pull/3681)

### Features
* Support links in using file directive (implements #1328) by [@ivan-klass](https://github.com/ivan-klass), [@majk-p](https://github.com/majk-p) and [@tgodzik](https://github.com/tgodzik) in [#3681](https://github.com/VirtusLab/scala-cli/pull/3681)

### Fixes
* Fix race condition in local repo setup by [@unlsycn](https://github.com/unlsycn) in [#3693](https://github.com/VirtusLab/scala-cli/pull/3693)
* Fix for #3481 by [@philwalk](https://github.com/philwalk) in [#3677](https://github.com/VirtusLab/scala-cli/pull/3677)

### Internal and build changes
* bugfix: Fix mill script on fish by [@tgodzik](https://github.com/tgodzik) in [#3700](https://github.com/VirtusLab/scala-cli/pull/3700)
* Make `test-fish-shell`, `test-hypothetical-sbt-export` and `bloop-memory-footprint` required for publishing by [@Gedochao](https://github.com/Gedochao) in [#3701](https://github.com/VirtusLab/scala-cli/pull/3701)
* [NIT] Refactor Scala CLI CI scripts by [@Gedochao](https://github.com/Gedochao) in [#3702](https://github.com/VirtusLab/scala-cli/pull/3702)
* Remove the `github-dependency-graph` CI workflow by [@Gedochao](https://github.com/Gedochao) in [#3703](https://github.com/VirtusLab/scala-cli/pull/3703)
* Add more tests for URLs in using file directives by [@Gedochao](https://github.com/Gedochao) in [#3706](https://github.com/VirtusLab/scala-cli/pull/3706)

### Documentation changes
* Fix Changing Java versions document by [@tmrkw1497](https://github.com/tmrkw1497) in [#3697](https://github.com/VirtusLab/scala-cli/pull/3697)
* Back port of documentation changes to main by [@github-actions](https://github.com/github-actions) in [#3699](https://github.com/VirtusLab/scala-cli/pull/3699)

### Updates
* Update scala-cli.sh launcher for 1.8.0 by [@github-actions](https://github.com/github-actions) in [#3672](https://github.com/VirtusLab/scala-cli/pull/3672)
* Update semanticdb-shared_2.13 to 4.13.6 by [@scala-steward](https://github.com/scala-steward) in [#3678](https://github.com/VirtusLab/scala-cli/pull/3678)
* Bump sass from 1.87.0 to 1.88.0 in /website by [@dependabot](https://github.com/dependabot) in [#3676](https://github.com/VirtusLab/scala-cli/pull/3676)
* Bump `java-class-name` to 0.1.6 by [@Gedochao](https://github.com/Gedochao) in [#3679](https://github.com/VirtusLab/scala-cli/pull/3679)
* Update `scala-cli-signing` to 0.2.7 by [@scala-steward](https://github.com/scala-steward) in [#3683](https://github.com/VirtusLab/scala-cli/pull/3683)
* Update `mill` scripts by [@Gedochao](https://github.com/Gedochao) in [#3686](https://github.com/VirtusLab/scala-cli/pull/3686)
* Bump sass from 1.88.0 to 1.89.0 in /website by [@dependabot](https://github.com/dependabot) in [#3687](https://github.com/VirtusLab/scala-cli/pull/3687)
* Bump `scala-js-cli` to 1.19.0.1 by [@Gedochao](https://github.com/Gedochao) in [#3689](https://github.com/VirtusLab/scala-cli/pull/3689)
* Bump Scala 3 Next RC to 3.7.1-RC2 by [@Gedochao](https://github.com/Gedochao) in [#3692](https://github.com/VirtusLab/scala-cli/pull/3692)
* Update sbt, scripted-plugin to 1.11.0 by [@scala-steward](https://github.com/scala-steward) in [#3696](https://github.com/VirtusLab/scala-cli/pull/3696)
* Bump `mill` to 0.12 by [@Gedochao](https://github.com/Gedochao) in [#3691](https://github.com/VirtusLab/scala-cli/pull/3691)
* Bump `scalafmt` to 3.9.7 & reformat by [@Gedochao](https://github.com/Gedochao) in [#3705](https://github.com/VirtusLab/scala-cli/pull/3705)
* Bump @docusaurus/preset-classic from 3.7.0 to 3.8.0 in /website by [@dependabot](https://github.com/dependabot) in [#3709](https://github.com/VirtusLab/scala-cli/pull/3709)
* Bump sass from 1.89.0 to 1.89.1 in /website by [@dependabot](https://github.com/dependabot) in [#3710](https://github.com/VirtusLab/scala-cli/pull/3710)
* Bump Scala Next to 3.7.1 by [@Gedochao](https://github.com/Gedochao) in [#3707](https://github.com/VirtusLab/scala-cli/pull/3707)

## New Contributors
* [@unlsycn](https://github.com/unlsycn) made their first contribution in [#3693](https://github.com/VirtusLab/scala-cli/pull/3693)
* [@tmrkw1497](https://github.com/tmrkw1497) made their first contribution in [#3697](https://github.com/VirtusLab/scala-cli/pull/3697)
* [@ivan-klass](https://github.com/ivan-klass) made their first contribution in [#3681](https://github.com/VirtusLab/scala-cli/pull/3681)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.8.0...v1.8.1

## [v1.8.0](https://github.com/VirtusLab/scala-cli/releases/tag/v1.8.0)

### Support for Scala 3.7.0 and 3.3.6
This Scala CLI version switches the default Scala version to 3.7.0.
The CLI internals are now built with Scala 3.3.6.

```bash
scala-cli version
# Scala CLI version: 1.8.0
# Scala version (default): 3.7.0
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3661](https://github.com/VirtusLab/scala-cli/pull/3661) and [#3671](https://github.com/VirtusLab/scala-cli/pull/3671)

### Support for Scala.js 1.19.0
This Scala CLI version adds support for Scala.js 1.19.0.

```bash
scala-cli -e 'println("Hello")' --js
# Compiling project (Scala 3.7.0, Scala.js 1.19.0)
# Compiled project (Scala 3.7.0, Scala.js 1.19.0)
# Hello
```

Added in [#3643](https://github.com/VirtusLab/scala-cli/pull/3643) and [scala-js-cli#134](https://github.com/VirtusLab/scala-js-cli/pull/134)

### Drop support for Scala older than 3.3 in `runner` and `test-runner` modules
Starting with Scala CLI v1.8.0, the `runner` and `test-runner` modules are built with Scala 3.3.5 LTS (on par with other modules built with Scala 3). 
They used to be built with Scala 3.0.2, as those modules may get added to the project class path when running, respectively,
the main scope and tests. This means that if the application is using pre-3.3 Scala 3, TASTy versions will be incompatible.

This is mostly informative, as the change should not be breaking for standard Scala CLI usage, even if an older Scala 3 version is being used. 
For builds using Scala older than 3.3, the CLI will automatically fall back to version 1.7.1 of the modules, with an appropriate warning being printed.
As the fallback will not be updated in the future, some Scala CLI features might start breaking at some point, as the APIs will stop being fully in sync.

```bash
scala-cli -e 'println("Hello")' --runner -S 3.1
# [warn] Scala 3.1.3 is no longer supported by the runner module.
# [warn] Defaulting to a legacy runner module version: 1.7.1.
# [warn] To use the latest runner, upgrade Scala to at least Scala 3.3.
# Compiling project (Scala 3.1.3, JVM (17))
# Compiled project (Scala 3.1.3, JVM (17))
# Hello
```

```bash ignore
scala-cli test . -S 3.2
# [warn] Scala 3.2.2 is no longer supported by the test-runner module.
# [warn] Defaulting to a legacy test-runner module version: 1.7.1.
# [warn] To use the latest test-runner, upgrade Scala to at least 3.3.
# Compiling project (test, Scala 3.2.2, JVM (17))
# Compiled project (test, Scala 3.2.2, JVM (17))
# Test run started
# Test MyTests.foo started
# Hello, world!
# Test MyTests.foo finished, took 0.001 sec
# Test run finished: 0 failed, 0 ignored, 1 total, 0.003s
```

Realistically, the change is only breaking for apps using those modules directly themselves, either depending on them or using them to run things.
In either case, it is recommended to update Scala up to at least 3.3 LTS.

Added by [@Gedochao](https://github.com/Gedochao) in [#3650](https://github.com/VirtusLab/scala-cli/pull/3650)

### Scala CLI now detects and runs multiple test frameworks, rather than just one
When running tests in a project with multiple test frameworks in use, Scala CLI will now attempt to detect and run all of them, rather than just one.

```bash ignore
scala-cli test .
# Compiling project (Scala 3.7.0, JVM (23))
# Compiled project (Scala 3.7.0, JVM (23))
# Compiling project (test, Scala 3.7.0, JVM (23))
# Compiled project (test, Scala 3.7.0, JVM (23))
# Munit:
#   + foo 0.007s
# -------------------------------- Running Tests --------------------------------
# + MyTests.foo 1ms  
# Tests: 1, Passed: 1, Failed: 0
# + SimpleSpec
# Hello from zio-test
#   + print hello and assert true
# 1 tests passed. 0 tests failed. 0 tests ignored.
# 
# Executed in 97 ms
# 
# Completed tests
# ScalaTestSpec:
# example
# - should work
# Run completed in 44 milliseconds.
# Total number of tests run: 1
# Suites: completed 1, aborted 0
# Tests: succeeded 1, failed 0, canceled 0, ignored 0, pending 0
# All tests passed.
```

Additionally, it is now possible to pre-define multiple test frameworks to use (rather than just one, as was possible before).

```scala compile
//> using test.frameworks org.scalatest.tools.Framework munit.Framework custom.CustomFramework
```

Pre-defining test frameworks may be preferable for bigger projects, as it allows to skip framework detection and run them directly. 
This is significant particularly for running tests with Scala Native and Scala.js.

Added by [@Gedochao](https://github.com/Gedochao) in [#3653](https://github.com/VirtusLab/scala-cli/pull/3653)

### Features
* Support the `--test` flag with the `publish` & `publish local` sub-commands by [@Gedochao](https://github.com/Gedochao) in [#3538](https://github.com/VirtusLab/scala-cli/pull/3538)
* Misc no-op and/or error handling for the `--test` command line flag by [@Gedochao](https://github.com/Gedochao) in [#3586](https://github.com/VirtusLab/scala-cli/pull/3586)
* Add scala-cli version to the BuildInfo by [@yadavan88](https://github.com/yadavan88) in [#3617](https://github.com/VirtusLab/scala-cli/pull/3617)
* `fix` sub-command tweaks by [@Gedochao](https://github.com/Gedochao) in [#3646](https://github.com/VirtusLab/scala-cli/pull/3646)
* Run all found test frameworks, rather than just one by [@Gedochao](https://github.com/Gedochao) in [#3621](https://github.com/VirtusLab/scala-cli/pull/3621)
* Allow to preconfigure multiple test frameworks by [@Gedochao](https://github.com/Gedochao) in [#3653](https://github.com/VirtusLab/scala-cli/pull/3653)
* Add support for some missing Scala compiler options & aliases without the need for `-O` by [@Gedochao](https://github.com/Gedochao) in [#3665](https://github.com/VirtusLab/scala-cli/pull/3665)
* Add support for the --repl-quit-after-init REPL option by [@Gedochao](https://github.com/Gedochao) in [#3664](https://github.com/VirtusLab/scala-cli/pull/3664)

### Fixes
* Fix `fmt` to format the `project.scala` configuration file as any other Scala input by [@Gedochao](https://github.com/Gedochao) in [#3609](https://github.com/VirtusLab/scala-cli/pull/3609)
* Apply `scalafix` rules to test scope inputs, too by [@Gedochao](https://github.com/Gedochao) in [#3641](https://github.com/VirtusLab/scala-cli/pull/3641)

### Internal and build changes
* Cross compile everything on the CI by [@Gedochao](https://github.com/Gedochao) in [#3570](https://github.com/VirtusLab/scala-cli/pull/3570)
* Add tests for the current behaviour of `--cross` by [@Gedochao](https://github.com/Gedochao) in [#3589](https://github.com/VirtusLab/scala-cli/pull/3589)
* Run `test` sub-command integration tests on default JVM settings by [@Gedochao](https://github.com/Gedochao) in [#3592](https://github.com/VirtusLab/scala-cli/pull/3592)
* Retry docs' tests on the CI by [@Gedochao](https://github.com/Gedochao) in [#3618](https://github.com/VirtusLab/scala-cli/pull/3618)
* Move `ScopeOptions` to `SharedOptions` by [@Gedochao](https://github.com/Gedochao) in [#3612](https://github.com/VirtusLab/scala-cli/pull/3612)
* Include missing Scala `3.6.*` versions in `Scala.listAll` by [@Gedochao](https://github.com/Gedochao) in [#3652](https://github.com/VirtusLab/scala-cli/pull/3652)
* Check formatting with Scala CLI, rather than the `scalafmt` launcher itself by [@Gedochao](https://github.com/Gedochao) in [#3660](https://github.com/VirtusLab/scala-cli/pull/3660)

### Documentation changes
* compileOnly option added to the documentation by [@yadavan88](https://github.com/yadavan88) in [#3600](https://github.com/VirtusLab/scala-cli/pull/3600)
* Back port of documentation changes to main by [@github-actions](https://github.com/github-actions) in [#3601](https://github.com/VirtusLab/scala-cli/pull/3601)
* docs: guide for compile only deps by [@scarf005](https://github.com/scarf005) in [#3602](https://github.com/VirtusLab/scala-cli/pull/3602)
* Back port of documentation changes to main by [@github-actions](https://github.com/github-actions) in [#3607](https://github.com/VirtusLab/scala-cli/pull/3607)
* Add missing `using` directive reference docs by [@Gedochao](https://github.com/Gedochao) in [#3608](https://github.com/VirtusLab/scala-cli/pull/3608)
* Back port of documentation changes to main by [@github-actions](https://github.com/github-actions) in [#3610](https://github.com/VirtusLab/scala-cli/pull/3610)
* Fix formatting in directives' reference docs by [@Gedochao](https://github.com/Gedochao) in [#3611](https://github.com/VirtusLab/scala-cli/pull/3611)
* Back port of documentation changes to main by [@github-actions](https://github.com/github-actions) in [#3616](https://github.com/VirtusLab/scala-cli/pull/3616)
* Fixed DEV.md file related to test command by [@yadavan88](https://github.com/yadavan88) in [#3619](https://github.com/VirtusLab/scala-cli/pull/3619)
* Correct doc with --project-version by [@joan38](https://github.com/joan38) in [#3662](https://github.com/VirtusLab/scala-cli/pull/3662)

### Updates
* Bump webfactory/ssh-agent from 0.9.0 to 0.9.1 by [@dependabot](https://github.com/dependabot) in [#3576](https://github.com/VirtusLab/scala-cli/pull/3576)
* Bump react from 18.2.0 to 18.3.1 in /website by [@dependabot](https://github.com/dependabot) in [#3573](https://github.com/VirtusLab/scala-cli/pull/3573)
* Update scala-cli.sh launcher for 1.7.1 by [@github-actions](https://github.com/github-actions) in [#3579](https://github.com/VirtusLab/scala-cli/pull/3579)
* Update guava to 33.4.5-jre by [@scala-steward](https://github.com/scala-steward) in [#3581](https://github.com/VirtusLab/scala-cli/pull/3581)
* Update bloop-rifle_2.13 to 2.0.9 by [@scala-steward](https://github.com/scala-steward) in [#3580](https://github.com/VirtusLab/scala-cli/pull/3580)
* Update sbt, scripted-plugin to 1.10.11 by [@scala-steward](https://github.com/scala-steward) in [#3582](https://github.com/VirtusLab/scala-cli/pull/3582)
* Pin & update docker images by [@Gedochao](https://github.com/Gedochao) in [#3558](https://github.com/VirtusLab/scala-cli/pull/3558)
* Bump clsx from 1.2.1 to 2.1.1 in /website by [@dependabot](https://github.com/dependabot) in [#3560](https://github.com/VirtusLab/scala-cli/pull/3560)
* Bump `docusaurus` to 3.7.0 by [@Gedochao](https://github.com/Gedochao) in [#3585](https://github.com/VirtusLab/scala-cli/pull/3585)
* Bump react-dom from 18.2.0 to 18.3.1 in /website by [@dependabot](https://github.com/dependabot) in [#3587](https://github.com/VirtusLab/scala-cli/pull/3587)
* Bump sass from 1.58.3 to 1.86.0 in /website by [@dependabot](https://github.com/dependabot) in [#3588](https://github.com/VirtusLab/scala-cli/pull/3588)
* Bump @easyops-cn/docusaurus-search-local from 0.49.1 to 0.49.2 in /website by [@dependabot](https://github.com/dependabot) in [#3604](https://github.com/VirtusLab/scala-cli/pull/3604)
* Update asm to 9.8 by [@scala-steward](https://github.com/scala-steward) in [#3606](https://github.com/VirtusLab/scala-cli/pull/3606)
* Update guava to 33.4.6-jre by [@scala-steward](https://github.com/scala-steward) in [#3605](https://github.com/VirtusLab/scala-cli/pull/3605)
* Bump sass from 1.86.0 to 1.86.1 in /website by [@dependabot](https://github.com/dependabot) in [#3603](https://github.com/VirtusLab/scala-cli/pull/3603)
* Bump @mdx-js/react from 3.0.0 to 3.1.0 in /website by [@dependabot](https://github.com/dependabot) in [#3575](https://github.com/VirtusLab/scala-cli/pull/3575)
* Bump sass from 1.86.1 to 1.86.3 in /website by [@dependabot](https://github.com/dependabot) in [#3622](https://github.com/VirtusLab/scala-cli/pull/3622)
* Bump estree-util-value-to-estree from 3.0.1 to 3.3.3 in /website by [@dependabot](https://github.com/dependabot) in [#3623](https://github.com/VirtusLab/scala-cli/pull/3623)
* Update guava to 33.4.7-jre by [@scala-steward](https://github.com/scala-steward) in [#3625](https://github.com/VirtusLab/scala-cli/pull/3625)
* Update Scala Next RC to 3.7.0-RC2 by [@Gedochao](https://github.com/Gedochao) in [#3628](https://github.com/VirtusLab/scala-cli/pull/3628)
* Update core_2.13 to 3.11.0 by [@scala-steward](https://github.com/scala-steward) in [#3630](https://github.com/VirtusLab/scala-cli/pull/3630)
* Update scala3-library to 3.7.0-RC3 by [@scala-steward](https://github.com/scala-steward) in [#3638](https://github.com/VirtusLab/scala-cli/pull/3638)
* Update guava to 33.4.8-jre by [@scala-steward](https://github.com/scala-steward) in [#3637](https://github.com/VirtusLab/scala-cli/pull/3637)
* Bump announced Scala Next RC to 3.7.0-RC2 by [@Gedochao](https://github.com/Gedochao) in [#3636](https://github.com/VirtusLab/scala-cli/pull/3636)
* Bump http-proxy-middleware from 2.0.6 to 2.0.9 in /website by [@dependabot](https://github.com/dependabot) in [#3642](https://github.com/VirtusLab/scala-cli/pull/3642)
* Declare BSP server as capable of providing output paths by [@Gedochao](https://github.com/Gedochao) in [#3645](https://github.com/VirtusLab/scala-cli/pull/3645)
* Update Scala.js to 1.19.0 by [@scala-steward](https://github.com/scala-steward) in [#3643](https://github.com/VirtusLab/scala-cli/pull/3643)
* Update metaconfig-typesafe-config to 0.16.0 by [@scala-steward](https://github.com/scala-steward) in [#3649](https://github.com/VirtusLab/scala-cli/pull/3649)
* Update Scala 3 Next RC to 3.7.0-RC4 by [@scala-steward](https://github.com/scala-steward) in [#3648](https://github.com/VirtusLab/scala-cli/pull/3648)
* Bump sass from 1.86.3 to 1.87.0 in /website by [@dependabot](https://github.com/dependabot) in [#3651](https://github.com/VirtusLab/scala-cli/pull/3651)
* Update semanticdb-shared_2.13 to 4.13.5 by [@scala-steward](https://github.com/scala-steward) in [#3658](https://github.com/VirtusLab/scala-cli/pull/3658)
* Update munit to 1.1.1 by [@scala-steward](https://github.com/scala-steward) in [#3656](https://github.com/VirtusLab/scala-cli/pull/3656)
* Update jsoup to 1.20.1 by [@scala-steward](https://github.com/scala-steward) in [#3655](https://github.com/VirtusLab/scala-cli/pull/3655)
* Update scalafmt-cli_2.13, scalafmt-core to 3.9.5 by [@scala-steward](https://github.com/scala-steward) in [#3657](https://github.com/VirtusLab/scala-cli/pull/3657)
* Bump Scala 3 Next to 3.7.0 by [@Gedochao](https://github.com/Gedochao) in [#3661](https://github.com/VirtusLab/scala-cli/pull/3661)
* Bump Scala 3 Next RC to 3.7.1-RC1 by [@Gedochao](https://github.com/Gedochao) in [#3663](https://github.com/VirtusLab/scala-cli/pull/3663)
* Update `runner` & `test-runner` to Scala 3.3.5 LTS (was 3.0.2) by [@Gedochao](https://github.com/Gedochao) in [#3650](https://github.com/VirtusLab/scala-cli/pull/3650)
* Update `scalafmt` to 3.9.6 by [@scala-steward](https://github.com/scala-steward) in [#3667](https://github.com/VirtusLab/scala-cli/pull/3667)
* chore: Bump Bloop to 2.0.10 by [@tgodzik](https://github.com/tgodzik) in [#3670](https://github.com/VirtusLab/scala-cli/pull/3670)
* Bump Scala 3 LTS to 3.3.6 by [@Gedochao](https://github.com/Gedochao) in [#3671](https://github.com/VirtusLab/scala-cli/pull/3671)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.7.1...v1.8.0

## [v1.7.1](https://github.com/VirtusLab/scala-cli/releases/tag/v1.7.1)

### Support for Scala 3.6.4
This Scala CLI version switches the default Scala version to 3.6.4.

```bash
scala-cli version
# Scala CLI version: 1.7.1
# Scala version (default): 3.6.4
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3544](https://github.com/VirtusLab/scala-cli/pull/3544)

### Support the `--test` command line option for `doc`

It is now possible to generate docs from the test scope with the `--test` flag.

Added by [@Gedochao](https://github.com/Gedochao) in [#3539](https://github.com/VirtusLab/scala-cli/pull/3539)

### Features
* Support the `--test` flag with the `doc` sub-command by [@Gedochao](https://github.com/Gedochao) in [#3539](https://github.com/VirtusLab/scala-cli/pull/3539)

### Internal and build changes
* Adjust `dependabot` configuration for the docs website by [@Gedochao](https://github.com/Gedochao) in [#3555](https://github.com/VirtusLab/scala-cli/pull/3555)

### Documentation changes
* docs: document Scala Native flag options by [@scarf005](https://github.com/scarf005) in [#3557](https://github.com/VirtusLab/scala-cli/pull/3557)

### Updates
* Update jsoup to 1.19.1 by [@scala-steward](https://github.com/scala-steward) in [#3542](https://github.com/VirtusLab/scala-cli/pull/3542)
* Update scala-cli.sh launcher for 1.7.0 by [@github-actions](https://github.com/github-actions) in [#3543](https://github.com/VirtusLab/scala-cli/pull/3543)
* Bump Scala 3 Next to 3.6.4 by [@Gedochao](https://github.com/Gedochao) in [#3544](https://github.com/VirtusLab/scala-cli/pull/3544)
* Bump SBT to 1.10.10 by [@Gedochao](https://github.com/Gedochao) in [#3545](https://github.com/VirtusLab/scala-cli/pull/3545)
* Update semanticdb-shared_2.13 to 4.13.3 by [@scala-steward](https://github.com/scala-steward) in [#3549](https://github.com/VirtusLab/scala-cli/pull/3549)
* Update scalafmt-cli_2.13, scalafmt-core to 3.9.3 by [@scala-steward](https://github.com/scala-steward) in [#3548](https://github.com/VirtusLab/scala-cli/pull/3548)
* Bump `scala-cli-signing` to 0.2.6 by [@Gedochao](https://github.com/Gedochao) in [#3552](https://github.com/VirtusLab/scala-cli/pull/3552)
* Bump react-player from 2.11.2 to 2.16.0 in /website by [@dependabot](https://github.com/dependabot) in [#3561](https://github.com/VirtusLab/scala-cli/pull/3561)
* Bump dorny/test-reporter from 1 to 2 by [@dependabot](https://github.com/dependabot) in [#3559](https://github.com/VirtusLab/scala-cli/pull/3559)
* Bump docusaurus-plugin-sass from 0.2.5 to 0.2.6 in /website by [@dependabot](https://github.com/dependabot) in [#3563](https://github.com/VirtusLab/scala-cli/pull/3563)
* Back port of documentation changes to main by [@github-actions](https://github.com/github-actions) in [#3565](https://github.com/VirtusLab/scala-cli/pull/3565)
* Update scalafmt-cli_2.13, scalafmt-core to 3.9.4 by [@scala-steward](https://github.com/scala-steward) in [#3567](https://github.com/VirtusLab/scala-cli/pull/3567)
* Bump @easyops-cn/docusaurus-search-local from 0.38.1 to 0.49.1 in /website by [@dependabot](https://github.com/dependabot) in [#3562](https://github.com/VirtusLab/scala-cli/pull/3562)
* Update announced Scala 3 Next to 3.6.4 by [@Gedochao](https://github.com/Gedochao) in [#3569](https://github.com/VirtusLab/scala-cli/pull/3569)
* Update semanticdb-shared_2.13 to 4.13.4 by [@scala-steward](https://github.com/scala-steward) in [#3568](https://github.com/VirtusLab/scala-cli/pull/3568)

## [v1.7.0](https://github.com/VirtusLab/scala-cli/releases/tag/v1.7.0)

### Switch to `scalameta/scalafmt` images of `scalafmt` 3.9.1+

Since version 3.9.1 `scalafmt` ships with native images built with Scala Native. As a result, 
we are sunsetting https://github.com/virtuslab/scalafmt-native-image and Scala CLI will use the artifacts 
from https://github.com/scalameta/scalafmt for `scalafmt` versions >=3.9.1

Note that older Scala CLI versions may still attempt to download a native image from the old repository for the new versions.
We will keep releasing those for a short while to help late upgraders migrate.

```bash
scala-cli fmt -F -version
# scalafmt 3.9.2
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3521](https://github.com/VirtusLab/scala-cli/pull/3521)

### Support the `--test` command line option for `run` and `package`

It is now possible to `run` a main method from the test scope with the `--test` flag.

```scala title=HelloFromTestScope.scala
//> using target.scope test
@main def helloFromTestScope(): Unit = println("Hello from the test scope!")
```

```bash
scala-cli run HelloFromTestScope.scala --test --power

# Hello from the test scope!  
```

Similarly, it is now possible to `package` the main and test scopes together, using the same `--test` flag.

```bash
scala-cli package HelloFromTestScope.scala --test --power
# # Wrote /Users/pchabelski/IdeaProjects/scala-cli-tests-2/untitled/v170/helloFromTestScope, run it with                                       
#  ./helloFromTestScope  
./helloFromTestScope
# Hello from the test scope!  
```

Keep in mind that the test and main scopes are still separate compilation units, where the test scope depends on the main scope (while the reverse isn't true).

Added by [@Gedochao](https://github.com/Gedochao) in [#3502](https://github.com/VirtusLab/scala-cli/pull/3502) and [#3519](https://github.com/VirtusLab/scala-cli/pull/3519)

### Detect objects with main class in scripts

Scala CLI now detects objects with a main method in scripts and runs them by default.

```scala title=scriptMainObject.sc
object Main {
  def main(args: Array[String]): Unit = println("Hello")
}
```

Do note that, this is chiefly a convenience feature for migration of old scripts, using the old, legacy `scala` runner.

If any top-level code is present alongside an object with a main method, the top-level code will be run instead and a warning printed.

```scala title=scriptWithMainObjectAndTopLevel.sc
object Main {
  def main(args: Array[String]): Unit = println("Hello")
}
println("Top level code says hello")
```

```bash
scala-cli run scriptWithMainObjectAndTopLevel.sc
# [warn]  Script contains objects with main methods and top-level statements, only the latter will be run.                                   
# Compiling project (Scala 3.6.3, JVM (23))
# Compiled project (Scala 3.6.3, JVM (23))
# Top level code says hello
```

Additionally, cases where multiple main methods are present in the same script are not supported, inidicated by a warning.

```scala title=scriptWithMultipleMainObjects.sc
object Main {
  def main(args: Array[String]): Unit = println("Hello1")
}

object Main2 {
  def main(args: Array[String]): Unit = println("Hello2")
}
```

Note that no output is printed in this example:

```bash
scala-cli run scriptWithMultipleMainObjects.sc
# [warn]  Only a single main is allowed within scripts. Multiple main classes were found in the script: Main, Main2                          
# Compiling project (Scala 3.6.3, JVM (23))
# Compiled project (Scala 3.6.3, JVM (23))
```

Finally, main methods defined in this way cannot be chosen via the `--main-class` command line option directive,
and neither will they be printed by the `--list-main-methods` flag.

Added by [@tgodzik](https://github.com/tgodzik) in [#3479](https://github.com/VirtusLab/scala-cli/pull/3479)

### Support for Scala Native 0.5.7
This Scala CLI version switches the default Scala Native version to 0.5.7.

```bash
scala-cli -e 'println("Hello from Scala Native 0.5.7!")' --native
# Compiling project (Scala 3.6.3, Scala Native 0.5.7)
# Compiled project (Scala 3.6.3, Scala Native 0.5.7)
# [info] Linking (multithreadingEnabled=true, disable if not used) (1045 ms)
# [info] Discovered 915 classes and 5608 methods after classloading
# [info] Checking intermediate code (quick) (41 ms)
# [info] Multithreading was not explicitly enabled - initial class loading has not detected any usage of system threads. Multithreading support will be disabled to improve performance.
# [info] Linking (multithreadingEnabled=false) (352 ms)
# [info] Discovered 498 classes and 2506 methods after classloading
# [info] Checking intermediate code (quick) (9 ms)
# [info] Discovered 477 classes and 1930 methods after optimization
# [info] Optimizing (debug mode) (608 ms)
# [info] Produced 9 LLVM IR files
# [info] Generating intermediate code (650 ms)
# [info] Compiling to native code (1674 ms)
# [info] Linking with [pthread, dl, m]
# [info] Linking native code (immix gc, none lto) (339 ms)
# [info] Postprocessing (0 ms)
# [info] Total (4655 ms)
# Hello from Scala Native 0.5.7!
```

Added in [#3527](https://github.com/VirtusLab/scala-cli/pull/3527)

### Features
* improvement: Detect objects with main class in scripts by [@tgodzik](https://github.com/tgodzik) in [#3479](https://github.com/VirtusLab/scala-cli/pull/3479)
* Add support for running a main method from the test scope by [@Gedochao](https://github.com/Gedochao) in [#3502](https://github.com/VirtusLab/scala-cli/pull/3502)
* Support the `--test` flag with the `package` sub-command by [@Gedochao](https://github.com/Gedochao) in [#3519](https://github.com/VirtusLab/scala-cli/pull/3519)

### Fixes
* Improve handling for parallel Scala CLI runs by [@Gedochao](https://github.com/Gedochao) in [#3399](https://github.com/VirtusLab/scala-cli/pull/3399)
* fix: Don't compile docs if there is no need by [@ghostbuster91](https://github.com/ghostbuster91) in [#3503](https://github.com/VirtusLab/scala-cli/pull/3503)
* fix: correctly report error position on unknown directive without values by [@kasiaMarek](https://github.com/kasiaMarek) in [#3518](https://github.com/VirtusLab/scala-cli/pull/3518)

### Internal and build changes
* fix for #3510 by [@philwalk](https://github.com/philwalk) in [#3513](https://github.com/VirtusLab/scala-cli/pull/3513)
* Fall back to the `cs` command on `PATH` in the `mill` script by [@Gedochao](https://github.com/Gedochao) in [#3517](https://github.com/VirtusLab/scala-cli/pull/3517)

### Documentation changes
* Curl install launcher in your repo by [@joan38](https://github.com/joan38) in [#3532](https://github.com/VirtusLab/scala-cli/pull/3532)

### Updates
* Update scala-cli.sh launcher for 1.6.2 by [@github-actions](https://github.com/github-actions) in [#3495](https://github.com/VirtusLab/scala-cli/pull/3495)
* Update metaconfig-typesafe-config to 0.15.0 by [@scala-steward](https://github.com/scala-steward) in [#3497](https://github.com/VirtusLab/scala-cli/pull/3497)
* Update bloop-config_2.13 to 2.3.2 by [@scala-steward](https://github.com/scala-steward) in [#3496](https://github.com/VirtusLab/scala-cli/pull/3496)
* Update semanticdb-shared_2.13 to 4.13.0 by [@scala-steward](https://github.com/scala-steward) in [#3500](https://github.com/VirtusLab/scala-cli/pull/3500)
* Update ammonite to 3.0.2 by [@scala-steward](https://github.com/scala-steward) in [#3504](https://github.com/VirtusLab/scala-cli/pull/3504)
* Update semanticdb-shared_2.13 to 4.13.1.1 by [@scala-steward](https://github.com/scala-steward) in [#3508](https://github.com/VirtusLab/scala-cli/pull/3508)
* Update scalafix-interfaces to 0.14.1 by [@scala-steward](https://github.com/scala-steward) in [#3511](https://github.com/VirtusLab/scala-cli/pull/3511)
* Update semanticdb-shared_2.13 to 4.13.2 by [@scala-steward](https://github.com/scala-steward) in [#3515](https://github.com/VirtusLab/scala-cli/pull/3515)
* Update scalafix-interfaces to 0.14.2 by [@scala-steward](https://github.com/scala-steward) in [#3514](https://github.com/VirtusLab/scala-cli/pull/3514)
* Update slf4j-nop to 2.0.17 by [@scala-steward](https://github.com/scala-steward) in [#3520](https://github.com/VirtusLab/scala-cli/pull/3520)
* Bump Scala 3 Next RC to 3.6.4-RC2 by [@Gedochao](https://github.com/Gedochao) in [#3525](https://github.com/VirtusLab/scala-cli/pull/3525)
* Update Scala Native to 0.5.7 by [@scala-steward](https://github.com/scala-steward) in [#3527](https://github.com/VirtusLab/scala-cli/pull/3527)
* Bump `scala-packager` to 0.1.32 & linux CI runners to `ubuntu-24.04` by [@Gedochao](https://github.com/Gedochao) in [#3528](https://github.com/VirtusLab/scala-cli/pull/3528)
* Update `scalafmt` to 3.9.1 by [@Gedochao](https://github.com/Gedochao) in [#3521](https://github.com/VirtusLab/scala-cli/pull/3521)
* Bump `scalafmt` to 3.9.2 by [@Gedochao](https://github.com/Gedochao) in [#3533](https://github.com/VirtusLab/scala-cli/pull/3533)
* Bump gifs tests Ubuntu Docker image to `ubuntu:24.04` by [@Gedochao](https://github.com/Gedochao) in [#3534](https://github.com/VirtusLab/scala-cli/pull/3534)
* Update sbt, scripted-plugin to 1.10.9 by [@scala-steward](https://github.com/scala-steward) in [#3537](https://github.com/VirtusLab/scala-cli/pull/3537)
* Bump Linux ARM64 Docker image to `ubuntu:24.04` by [@Gedochao](https://github.com/Gedochao) in [#3535](https://github.com/VirtusLab/scala-cli/pull/3535)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.6.2...v1.7.0

## [v1.6.2](https://github.com/VirtusLab/scala-cli/releases/tag/v1.6.2)

### Support for Scala.js 1.18.2
This Scala CLI version adds support for Scala.js 1.18.2.

```bash
scala-cli -e 'println("Hello")' --js
# Compiling project (Scala 3.6.3, Scala.js 1.18.2)
# Compiled project (Scala 3.6.3, Scala.js 1.18.2)
# Hello
```

Added in [#3454](https://github.com/VirtusLab/scala-cli/pull/3454)

### Support for Scala 3.3.5
The Scala CLI internals are now built with Scala 3.3.5.

Added by [@Gedochao](https://github.com/Gedochao) in [#3466](https://github.com/VirtusLab/scala-cli/pull/3466)

### Deprecations
* Add a deprecation warning for using Scala 2.12.4 with Bloop by [@Gedochao](https://github.com/Gedochao) in [#3470](https://github.com/VirtusLab/scala-cli/pull/3470)

### Fixes
* Remove conflicting Scala 2.13 `io.get-coursier:dependency` dependency & add a CI check by [@Gedochao](https://github.com/Gedochao) in [#3472](https://github.com/VirtusLab/scala-cli/pull/3472)

### Internal and build changes
* Fix Scala Steward post-update hook by [@fthomas](https://github.com/fthomas) in [#3465](https://github.com/VirtusLab/scala-cli/pull/3465)
* Bump Windows runners to windows-2025 on the CI by [@Gedochao](https://github.com/Gedochao) in [#3489](https://github.com/VirtusLab/scala-cli/pull/3489)

### Documentation changes
* Add warning about test files in publish docs by [@majk-p](https://github.com/majk-p) in [#3486](https://github.com/VirtusLab/scala-cli/pull/3486)
* Back port of documentation changes to main by [@github-actions](https://github.com/github-actions) in [#3487](https://github.com/VirtusLab/scala-cli/pull/3487)

## Updates
* Update scala-cli.sh launcher for 1.6.1 by [@github-actions](https://github.com/github-actions) in [#3452](https://github.com/VirtusLab/scala-cli/pull/3452)
* Update munit to 1.1.0 by [@scala-steward](https://github.com/scala-steward) in [#3455](https://github.com/VirtusLab/scala-cli/pull/3455)
* Update latest announced Scala 3 Next RC to 3.6.4-RC1 by [@Gedochao](https://github.com/Gedochao) in [#3458](https://github.com/VirtusLab/scala-cli/pull/3458)
* Update scalajs-sbt-test-adapter_2.13 to 1.18.2 by [@scala-steward](https://github.com/scala-steward) in [#3454](https://github.com/VirtusLab/scala-cli/pull/3454)
* Update Scala 3 LTS to 3.3.5 by [@Gedochao](https://github.com/Gedochao) in [#3466](https://github.com/VirtusLab/scala-cli/pull/3466)
* Update scalafmt-cli_2.13, scalafmt-core to 3.8.6 by [@scala-steward](https://github.com/scala-steward) in [#3462](https://github.com/VirtusLab/scala-cli/pull/3462)
* Update sbt, scripted-plugin to 1.10.7 by [@scala-steward](https://github.com/scala-steward) in [#3461](https://github.com/VirtusLab/scala-cli/pull/3461)
* Bump Scala 3 Next announced version to 3.6.3 by [@Gedochao](https://github.com/Gedochao) in [#3469](https://github.com/VirtusLab/scala-cli/pull/3469)
* Bump `ammonite` to 3.0.1 by [@Gedochao](https://github.com/Gedochao) in [#3468](https://github.com/VirtusLab/scala-cli/pull/3468)
* Update core_2.13 to 3.10.3 by [@scala-steward](https://github.com/scala-steward) in [#3476](https://github.com/VirtusLab/scala-cli/pull/3476)
* Update scala-collection-compat to 2.13.0 by [@scala-steward](https://github.com/scala-steward) in [#3478](https://github.com/VirtusLab/scala-cli/pull/3478)
* Update case-app to 2.1.0-M30 by [@scala-steward](https://github.com/scala-steward) in [#3485](https://github.com/VirtusLab/scala-cli/pull/3485)
* Bump Scala toolkit to 0.6.0 by [@Gedochao](https://github.com/Gedochao) in [#3471](https://github.com/VirtusLab/scala-cli/pull/3471)
* Update Scala Toolkit to 0.7.0 by [@Gedochao](https://github.com/Gedochao) in [#3488](https://github.com/VirtusLab/scala-cli/pull/3488)

## New Contributors
* [@fthomas](https://github.com/fthomas) made their first contribution in [#3465](https://github.com/VirtusLab/scala-cli/pull/3465)
* [@majk-p](https://github.com/majk-p) made their first contribution in [#3486](https://github.com/VirtusLab/scala-cli/pull/3486)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.6.1...v1.6.2

## [v1.6.1](https://github.com/VirtusLab/scala-cli/releases/tag/v1.6.1)

## Pass `--repl-init-script` directly to the Scala REPL
Passing an initialization script to the REPL with `--repl-init-script` is now allowed directly, rather than after `--` or with `-O`.
The `--repl-init-script` is a REPL option introduced in Scala 3.6.4, so it's not available for earlier Scala versions.
```bash ignore
scala-cli repl -S 3.6.4-RC1 --repl-init-script 'println("Hello")'
# Hello
# Welcome to Scala 3.6.4-RC1 (23.0.1, Java OpenJDK 64-Bit Server VM).
# Type in expressions for evaluation. Or try :help.
#                                                                                                                  
# scala> 
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3447](https://github.com/VirtusLab/scala-cli/pull/3447)

### Hotfix release
Although Scala CLI 1.6.1 includes a few updates and improvements, it is primarily a hotfix release for version 1.6.0, which due to technical limitations wasn't available on some of our distribution channels.

For extra context refer to:
* [Scala CLI 1.6.0 release notes](#v160)

## Features
- Enable direct usage of --repl-init-script with Scala REPL >= 3.6.4-RC1 by [@Gedochao](https://github.com/Gedochao) in [#3447](https://github.com/VirtusLab/scala-cli/pull/3447)

## Internal and build changes
* Fix `update-packages` step of the release job on the CI by [@Gedochao](https://github.com/Gedochao) in [#3446](https://github.com/VirtusLab/scala-cli/pull/3446)

## Updates
* Bump Scala CLI launchers to v1.6.0 by [@Gedochao](https://github.com/Gedochao) in [#3450](https://github.com/VirtusLab/scala-cli/pull/3450)
* chore: Update Bloop to 2.0.8 by [@tgodzik](https://github.com/tgodzik) in [#3449](https://github.com/VirtusLab/scala-cli/pull/3449)
* Update scalafmt to 3.8.5 by [@scala-steward](https://github.com/scala-steward) in [#3442](https://github.com/VirtusLab/scala-cli/pull/3442)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.6.0...v1.6.1

## [v1.6.0](https://github.com/VirtusLab/scala-cli/releases/tag/v1.6.0)

### Scala CLI 1.6.0 will not be available on all distribution channels
Due to technical difficulties with our release pipeline, Scala CLI 1.6.0 release distribution channels were limited to:
- its [GitHub release page](https://github.com/VirtusLab/scala-cli/releases/tag/v1.6.0), where launchers for all platforms are available as normal
- Maven Central
- WinGet
- Chocolatey

While it can be used as such, we followed it up with [a hotfix 1.6.1 release](#v161), which should be available through all standard distribution channels.

```bash
scala-cli --cli-version 1.6.0 --version
# Scala CLI version: 1.6.0
# Scala version (default): 3.6.3
```

### Fixed commas being treated as `using` directive value separators & deprecated using them with whitespace
:::warning
These are breaking changes affecting using directives syntax.
They're technically fixes + a deprecation, but in a very rare scenario existing builds could break, if they were relying on the erroneous syntax.
:::

This Scala CLI version fixes commas (`,`) being treated as `using` directive value separators on their own.

Formerly, a directive like:

```scala compile
//> using options -Wunused:locals,privates
```

Would be (erroneously) interpreted as the following 2 options for the compiler: `-Wunused:locals` and `privates`.
As a comma will now no longer be treated as a separator (which it never should have been), it will now be interpreted correctly as
a single option: `-Wunused:locals,privates`.
Before this change, the only way to pass this value to the `options` directive key was escaping the comma with double quotes:
```scala compile
//> using options "-Wunused:locals,privates"
```
The escaping is no longer necessary.

Additionally, using commas along with whitespace as separators is now deprecated for future removal.
```bash ignore
scala-cli compile --scala-snippet '//> using options -Wunused:locals, -Wunused:privates'
# [warn] <snippet>-scala-snippet:1:34
# [warn] Use of commas as separators is deprecated. Only whitespace is neccessary.
# Starting compilation server
# Compiling project (Scala 3.6.3, JVM (23))
# Compiled project (Scala 3.6.3, JVM (23))
```

Finally, the use of `/* (..) */` comments in `using` directives is no longer supported.
```scala fail
//> using /* some comment */ options -Wunused:locals /* some other comment */ -Wunused:privates
// this syntax used to be supported, but will now fail.
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3381](https://github.com/VirtusLab/scala-cli/pull/3381) and [#3333](https://github.com/VirtusLab/scala-cli/pull/3333)

### Cap vague Scala versions at defaults
:::warning
This is a breaking change regarding how the Scala version is resolved.
:::
We have changed how a Scala version is picked when `major` or `major.minor` prefixes are passed, rather than the full version tag:
- `-S 3` will now point to the [launcher default Scala 3 Next version](https://github.com/VirtusLab/scala-cli/blob/72c23a54ac3ef0d3b09aa2646733225a1ac8426a/project/deps.sc#L11), rather than whatever is the latest stable version that `coursier` can find upstream
- similarly, `-S 3.<current launcher default minor>` will now point to the [launcher default Scala 3 Next version](https://github.com/VirtusLab/scala-cli/blob/72c23a54ac3ef0d3b09aa2646733225a1ac8426a/project/deps.sc#L11)
- `-S 2.13` will point to the[ launcher default Scala 2.13 version](https://github.com/VirtusLab/scala-cli/blob/72c23a54ac3ef0d3b09aa2646733225a1ac8426a/project/deps.sc#L7) (which up till now only affected tests and generated docs)
- similarly, `-S 2.12` will now point to the [launcher default Scala 2.12 version](https://github.com/VirtusLab/scala-cli/blob/72c23a54ac3ef0d3b09aa2646733225a1ac8426a/project/deps.sc#L6)
- launcher defaults are overridden for a particular Scala series with the `--cli-user-scala-version` to accommodate for Scala CLI installed as `scala`

For example:
```scala
//> using 3
// When compiled with Scala CLI v1.6.0, this snippet will use Scala 3.6.3 (the built-in default), even if a newer version has been released.
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3259](https://github.com/VirtusLab/scala-cli/pull/3259)

### Support for Scala 3.6.3 and 2.13.16
This Scala CLI version switches the default Scala version to 3.6.3.

```bash
scala-cli version
# Scala CLI version: 1.6.0
# Scala version (default): 3.6.3
```

It has also been tested with Scala 2.13.16.

Added by [@Gedochao](https://github.com/Gedochao) in [#3426](https://github.com/VirtusLab/scala-cli/pull/3426) and [#3418](https://github.com/VirtusLab/scala-cli/pull/3418)

### Support for Scala.js 1.18.1
This Scala CLI version adds support for Scala.js 1.18.1.

```bash
scala-cli -e 'println("Hello")' --js
# Compiling project (Scala 3.6.3, Scala.js 1.18.1)
# Compiled project (Scala 3.6.3, Scala.js 1.18.1)
# Hello
```

Added in [#3440](https://github.com/VirtusLab/scala-cli/pull/3440) and [scala-js-cli#113](https://github.com/VirtusLab/scala-js-cli/pull/113)

### (⚡️ experimental) `scalafix` integration
We now support running `scalafix` rules with the `fix` sub-command.
```bash ignore
scala-cli fix . --power
# The `fix` sub-command is experimental
# Please bear in mind that non-ideal user experience should be expected.
# If you encounter any bugs or have feedback to share, make sure to reach out to the maintenance team at https://github.com/VirtusLab/scala-cli
# Running built-in rules...
# Writing project.scala
# Removing directives from Smth.scala
# Built-in rules completed.
# Running scalafix rules...
# Starting compilation server
# Compiling project (Scala 3.6.3, JVM (23))
# [warn] ./Main.scala:2:7
# [warn] unused local definition
# [warn]   val unused = "unused"
# [warn]       ^^^^^^
# Compiled project (Scala 3.6.3, JVM (23))
# scalafix rules completed.
```

Former fix functionalities are now referred to in the code as the built-in rules.
Effectively, fix now runs 2 separate sets of rules (both enabled by default): built-in and scalafix.
They can be controlled via the `--enable-scalafix` and `--enable-built-in` command line options.

`scalafix` rules are ran according to the configuration in `<project-root>/.scalafix.conf`.

It is possible to run [external scalafix rules](https://scalacenter.github.io/scalafix/docs/rules/external-rules.html) with the (⚡️ experimental) `scalafix.dep` directive:
```scala compile power
//> using scalafix.dep com.github.xuwei-k::scalafix-rules:0.6.0
```

Added by [@Vigorge](https://github.com/Vigorge) and [@dos65](https://github.com/dos65) in [#2968](https://github.com/VirtusLab/scala-cli/pull/2968)

### Support for running snapshot versions of the build server (Bloop)
It is now possible to pass a snapshot version to the `--bloop-version` command line option.

```bash ignore
scala-cli compile . --bloop-version 2.0.7-8-fe3f53d9-SNAPSHOT
# Starting compilation server
# Compiling project (Scala 3.6.3, JVM (23))
# Compiled project (Scala 3.6.3, JVM (23))
scala-cli --power bloop about
# bloop v2.0.7-8-fe3f53d9-SNAPSHOT
# 
# Using Scala v2.12.20 and Zinc v1.10.7
# Running on Java JDK v23.0.1 (~/Library/Caches/Coursier/arc/https/github.com/adoptium/temurin23-binaries/releases/download/jdk-23.0.1%252B11/OpenJDK23U-jdk_aarch64_mac_hotspot_23.0.1_11.tar.gz/jdk-23.0.1+11/Contents/Home)
#   -> Supports debugging user code, Java Debug Interface (JDI) is available.
# Maintained by the Scala Center and the community.
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3405](https://github.com/VirtusLab/scala-cli/pull/3405)

### Support for suppressing deprecation warnings
It is now possible to suppress deprecation warnings with the `--suppress-deprecated-warnings` command line option.

```bash ignore
scala-cli project-with-deprecated-stuff --suppress-deprecated-warnings
````

You can also suppress deprecation warnings globally by setting the `suppress-warning.deprecated-features` configuration key.
```bash ignore
scala-cli config suppress-warning.deprecated-features true
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3406](https://github.com/VirtusLab/scala-cli/pull/3406)

### Features
* Scalafix command for scala-cli with basic options and tests by [@Vigorge](https://github.com/Vigorge) and [@dos65](https://github.com/dos65) in [#2968](https://github.com/VirtusLab/scala-cli/pull/2968)
* Ensure vague Scala versions are capped at defaults by [@Gedochao](https://github.com/Gedochao) in [#3259](https://github.com/VirtusLab/scala-cli/pull/3259)
* Merge `scalafix` into `fix` by [@Gedochao](https://github.com/Gedochao) in [#3400](https://github.com/VirtusLab/scala-cli/pull/3400)
* Allow to use Bloop snapshot versions by [@Gedochao](https://github.com/Gedochao) in [#3405](https://github.com/VirtusLab/scala-cli/pull/3405)
* Add ways to suppress deprecation warnings by [@Gedochao](https://github.com/Gedochao) in [#3406](https://github.com/VirtusLab/scala-cli/pull/3406)

### Fixes
* Misc improvements in compiler options handling by [@Gedochao](https://github.com/Gedochao) in [#3253](https://github.com/VirtusLab/scala-cli/pull/3253)
* Allow shading of single-choice compiler options from the command line regardless of `-`/`--` prefix by [@Gedochao](https://github.com/Gedochao) in [#3279](https://github.com/VirtusLab/scala-cli/pull/3279)
* Fix dependency main class detection throwing an NPE when JAR manifest doesn't list the main class correctly by [@Gedochao](https://github.com/Gedochao) in [#3319](https://github.com/VirtusLab/scala-cli/pull/3319)
* Fix commas being treated as `using` directives value separators & deprecate using them with whitespace by [@Gedochao](https://github.com/Gedochao) in [#3333](https://github.com/VirtusLab/scala-cli/pull/3333)
* Retain Bloop connection when restarting a build with `--watch` by [@Gedochao](https://github.com/Gedochao) in [#3351](https://github.com/VirtusLab/scala-cli/pull/3351)
* Improve deprecation warnings for commas with whitespace used as using directive value separators by [@Gedochao](https://github.com/Gedochao) in [#3366](https://github.com/VirtusLab/scala-cli/pull/3366)
* Recover from invalid paths returned from Bloop diagnostics by [@Gedochao](https://github.com/Gedochao) in [#3372](https://github.com/VirtusLab/scala-cli/pull/3372)
* Add missing support for excluding transient dependencies when publishing by [@Gedochao](https://github.com/Gedochao) in [#3357](https://github.com/VirtusLab/scala-cli/pull/3357)
* Fix using directives crashing on `*/` by removing `/* (..) */` comments support in `using_directives` by [@Gedochao](https://github.com/Gedochao) in [#3381](https://github.com/VirtusLab/scala-cli/pull/3381)
* `fix` built-in rules: don't wrap directive values in double quotes if not necessary by [@Gedochao](https://github.com/Gedochao) in [#3414](https://github.com/VirtusLab/scala-cli/pull/3414)
* Don't migrate directives with `fix` for single-file projects by [@Gedochao](https://github.com/Gedochao) in [#3422](https://github.com/VirtusLab/scala-cli/pull/3422)
* Temporarily disable built-in rules of `fix` when `--check` is enabled, until fully supported by [@Gedochao](https://github.com/Gedochao) in [#3427](https://github.com/VirtusLab/scala-cli/pull/3427)
* Ensure test source directives with test scope equivalents are migrated by `fix` to `project.scala` by [@Gedochao](https://github.com/Gedochao) in [#3425](https://github.com/VirtusLab/scala-cli/pull/3425)

### Internal and build changes
* Retry some of the occasionally flaky tests when failing on the CI by [@Gedochao](https://github.com/Gedochao) in [#3320](https://github.com/VirtusLab/scala-cli/pull/3320)
* Retry more occasionally flaky tests on the CI by [@Gedochao](https://github.com/Gedochao) in [#3331](https://github.com/VirtusLab/scala-cli/pull/3331)
* Tag `scalajs-dom` tests as flaky by [@Gedochao](https://github.com/Gedochao) in [#3336](https://github.com/VirtusLab/scala-cli/pull/3336)
* Tag native packager tests as flaky by [@Gedochao](https://github.com/Gedochao) in [#3344](https://github.com/VirtusLab/scala-cli/pull/3344)
* Make `generate-junit-reports.sc` script recover from test failures containing no trace data by [@Gedochao](https://github.com/Gedochao) in [#3341](https://github.com/VirtusLab/scala-cli/pull/3341)
* Support `coursier`-downloaded `scala` wrapper tests on Windows by [@Gedochao](https://github.com/Gedochao) in [#3325](https://github.com/VirtusLab/scala-cli/pull/3325)
* Get rid of duplicate names for uploaded/downloaded artifacts on the CI by [@Gedochao](https://github.com/Gedochao) in [#3342](https://github.com/VirtusLab/scala-cli/pull/3342)
* Retry generating the Windows launcher up to 5 times by [@Gedochao](https://github.com/Gedochao) in [#3349](https://github.com/VirtusLab/scala-cli/pull/3349)
* Retry generating Windows launchers in the `generate-native-image.sh` script directly, rather than the entire CI step by [@Gedochao](https://github.com/Gedochao) in [#3350](https://github.com/VirtusLab/scala-cli/pull/3350)
* Fix integration tests when run with a Scala 3 LTS RC version by [@Gedochao](https://github.com/Gedochao) in [#3362](https://github.com/VirtusLab/scala-cli/pull/3362)
* Retry some more flaky tests on the CI by [@Gedochao](https://github.com/Gedochao) in [#3382](https://github.com/VirtusLab/scala-cli/pull/3382)
* Run extra tests for main supported JVM versions by [@Gedochao](https://github.com/Gedochao) in [#3375](https://github.com/VirtusLab/scala-cli/pull/3375)
* tests: Add tests for issue with rereporting errors by [@tgodzik](https://github.com/tgodzik) in [#3390](https://github.com/VirtusLab/scala-cli/pull/3390)
* Add extra logs when retrying flaky tests by [@Gedochao](https://github.com/Gedochao) in [#3433](https://github.com/VirtusLab/scala-cli/pull/3433)

### Deprecations
* Deprecate `--src` and `--sources` to disambiguate with `--source` compiler option by [@Gedochao](https://github.com/Gedochao) in [#3412](https://github.com/VirtusLab/scala-cli/pull/3412)

### Documentation changes
* docs: document Scala Native flag options by [@scarf005](https://github.com/scarf005) in [#3386](https://github.com/VirtusLab/scala-cli/pull/3386)
* docs: document Scala Native flag options by [@scarf005](https://github.com/scarf005) in [#3416](https://github.com/VirtusLab/scala-cli/pull/3416)
* Back port of documentation changes to main by [@github-actions](https://github.com/github-actions) in [#3419](https://github.com/VirtusLab/scala-cli/pull/3419)
* Merge `scalafix` doc into `fix` by [@Gedochao](https://github.com/Gedochao) in [#3420](https://github.com/VirtusLab/scala-cli/pull/3420)

### Updates
* Update Scala 3 Next RC to 3.6.2-RC1 by [@Gedochao](https://github.com/Gedochao) in [#3305](https://github.com/VirtusLab/scala-cli/pull/3305)
* Update scala-cli.sh launcher for 1.5.4 by [@github-actions](https://github.com/github-actions) in [#3308](https://github.com/VirtusLab/scala-cli/pull/3308)
* Update `coursier` to 2.1.18 by [@Gedochao](https://github.com/Gedochao) in [#3312](https://github.com/VirtusLab/scala-cli/pull/3312)
* Update `scala-packager` to 0.1.31 by [@Gedochao](https://github.com/Gedochao) in [#3311](https://github.com/VirtusLab/scala-cli/pull/3311)
* Update jsoup to 1.18.2 by [@scala-steward](https://github.com/scala-steward) in [#3323](https://github.com/VirtusLab/scala-cli/pull/3323)
* Update Scala 3 Next RC to 3.6.2-RC2 by [@Gedochao](https://github.com/Gedochao) in [#3321](https://github.com/VirtusLab/scala-cli/pull/3321)
* Bump `coursier` to 2.1.19 by [@Gedochao](https://github.com/Gedochao) in [#3326](https://github.com/VirtusLab/scala-cli/pull/3326)
* Bump Typelevel toolkit to 0.1.29 by [@Gedochao](https://github.com/Gedochao) in [#3332](https://github.com/VirtusLab/scala-cli/pull/3332)
* Bump Scala 3 Next RC to 3.6.2-RC3 by [@Gedochao](https://github.com/Gedochao) in [#3334](https://github.com/VirtusLab/scala-cli/pull/3334)
* Update jsoup to 1.18.3 by [@scala-steward](https://github.com/scala-steward) in [#3338](https://github.com/VirtusLab/scala-cli/pull/3338)
* Update sbt, scripted-plugin to 1.10.6 by [@scala-steward](https://github.com/scala-steward) in [#3339](https://github.com/VirtusLab/scala-cli/pull/3339)
* Bump actions/upload-artifact & actions/download-artifact from 3 to 4 by [@Gedochao](https://github.com/Gedochao) in [#2701](https://github.com/VirtusLab/scala-cli/pull/2701)
* Update munit to 1.0.3 by [@scala-steward](https://github.com/scala-steward) in [#3346](https://github.com/VirtusLab/scala-cli/pull/3346)
* Update dependency to 0.3.2 by [@scala-steward](https://github.com/scala-steward) in [#3353](https://github.com/VirtusLab/scala-cli/pull/3353)
* Update `coursier` to 2.1.20 by [@Gedochao](https://github.com/Gedochao) in [#3356](https://github.com/VirtusLab/scala-cli/pull/3356)
* Bump Scala Next to 3.6.2 by [@Gedochao](https://github.com/Gedochao) in [#3358](https://github.com/VirtusLab/scala-cli/pull/3358)
* Bump Scala Next RC to 3.6.3-RC1 by [@Gedochao](https://github.com/Gedochao) in [#3360](https://github.com/VirtusLab/scala-cli/pull/3360)
* Update metaconfig-typesafe-config to 0.14.0 by [@scala-steward](https://github.com/scala-steward) in [#3364](https://github.com/VirtusLab/scala-cli/pull/3364)
* Update coursier-jvm_2.13, ... to 2.1.21 by [@scala-steward](https://github.com/scala-steward) in [#3363](https://github.com/VirtusLab/scala-cli/pull/3363)
* Set Scala 3.6.2 as the latest Next announced version by [@Gedochao](https://github.com/Gedochao) in [#3365](https://github.com/VirtusLab/scala-cli/pull/3365)
* Update bloop-rifle_2.13 to 2.0.6 by [@scala-steward](https://github.com/scala-steward) in [#3368](https://github.com/VirtusLab/scala-cli/pull/3368)
* Update guava to 33.4.0-jre by [@scala-steward](https://github.com/scala-steward) in [#3376](https://github.com/VirtusLab/scala-cli/pull/3376)
* Update `coursier` to 2.1.22 by [@scala-steward](https://github.com/scala-steward) in [#3378](https://github.com/VirtusLab/scala-cli/pull/3378)
* Bump the announced Scala 3 Next RC version to 3.6.3-RC1 by [@Gedochao](https://github.com/Gedochao) in [#3380](https://github.com/VirtusLab/scala-cli/pull/3380)
* Update bloop-config_2.13 to 2.2.0 by [@scala-steward](https://github.com/scala-steward) in [#3392](https://github.com/VirtusLab/scala-cli/pull/3392)
* Update `coursier` to 2.1.23 by [@scala-steward](https://github.com/scala-steward) in [#3395](https://github.com/VirtusLab/scala-cli/pull/3395)
* Update Scala 3 Next RC to 3.6.3-RC2 by [@Gedochao](https://github.com/Gedochao) in [#3398](https://github.com/VirtusLab/scala-cli/pull/3398)
* Set Scala 3.6.3-RC2 as the latest announced RC version by [@Gedochao](https://github.com/Gedochao) in [#3407](https://github.com/VirtusLab/scala-cli/pull/3407)
* Update Scala 2.13 series to 2.13.16 by [@Gedochao](https://github.com/Gedochao) in [#3418](https://github.com/VirtusLab/scala-cli/pull/3418)
* Update `coursier` to 2.1.24 by [@Gedochao](https://github.com/Gedochao) in [#3429](https://github.com/VirtusLab/scala-cli/pull/3429)
* Update Scala Next to 3.6.3 by [@Gedochao](https://github.com/Gedochao) in [#3426](https://github.com/VirtusLab/scala-cli/pull/3426)
* Update Scala Next RC to 3.6.4-RC1 by [@Gedochao](https://github.com/Gedochao) in [#3431](https://github.com/VirtusLab/scala-cli/pull/3431)
* Update bloop-config_2.13 to 2.3.1 by [@scala-steward](https://github.com/scala-steward) in [#3435](https://github.com/VirtusLab/scala-cli/pull/3435)
* Update core_2.13 to 3.10.2 by [@scala-steward](https://github.com/scala-steward) in [#3439](https://github.com/VirtusLab/scala-cli/pull/3439)
* Update scalafix-interfaces to 0.14.0 by [@scala-steward](https://github.com/scala-steward) in [#3437](https://github.com/VirtusLab/scala-cli/pull/3437)
* Update munit to 1.0.4 by [@scala-steward](https://github.com/scala-steward) in [#3441](https://github.com/VirtusLab/scala-cli/pull/3441)
* Update Scala.js to 1.18.1 by [@scala-steward](https://github.com/scala-steward) in [#3440](https://github.com/VirtusLab/scala-cli/pull/3440)

### New Contributors
* [@Vigorge](https://github.com/Vigorge) made their first contribution in [#2968](https://github.com/VirtusLab/scala-cli/pull/2968)
* [@scarf005](https://github.com/scarf005) made their first contribution in [#3386](https://github.com/VirtusLab/scala-cli/pull/3386)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.5.4...v1.6.0

## [v1.5.4](https://github.com/VirtusLab/scala-cli/releases/tag/v1.5.4)

### Hotfix release
Although Scala CLI 1.5.4 includes a few updates and improvements, it is primarily a hotfix release for versions 1.5.2 and 1.5.3, which due to technical limitations weren't available on some of our distribution channels.

For extra context refer to:
* [Scala CLI 1.5.2 release notes](#v152)
* [Scala CLI 1.5.3 release notes](#v153)

### Support for Scala Native 0.5.6
This Scala CLI version switches the default Scala Native version to 0.5.6.

```bash
scala-cli -e 'println("Hello from Scala Native 0.5.6!")' --native
# Compiling project (Scala 3.5.2, Scala Native 0.5.6)
# Compiled project (Scala 3.5.2, Scala Native 0.5.6)
# [info] Linking (multithreadingEnabled=true, disable if not used) (949 ms)
# [info] Discovered 887 classes and 5408 methods after classloading
# [info] Checking intermediate code (quick) (40 ms)
# [info] Multithreading was not explicitly enabled - initial class loading has not detected any usage of system threads. Multithreading support will be disabled to improve performance.
# [info] Linking (multithreadingEnabled=false) (285 ms)
# [info] Discovered 499 classes and 2500 methods after classloading
# [info] Checking intermediate code (quick) (7 ms)
# [info] Discovered 478 classes and 1914 methods after optimization
# [info] Optimizing (debug mode) (429 ms)
# [info] Produced 9 LLVM IR files
# [info] Generating intermediate code (296 ms)
# [info] Compiling to native code (1464 ms)
# [info] Linking with [pthread, dl]
# [info] Linking native code (immix gc, none lto) (208 ms)
# [info] Postprocessing (0 ms)
# [info] Total (3728 ms)
# Hello from Scala Native 0.5.6!
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3295](https://github.com/VirtusLab/scala-cli/pull/3295)

### Fixes
* Pin Fedora docker image at `fedora:40` by [@Gedochao](https://github.com/Gedochao) in [#3283](https://github.com/VirtusLab/scala-cli/pull/3283)
* Don't fail the `update-packages` and `windows-packages` jobs on individual distributions' steps by [@Gedochao](https://github.com/Gedochao) in [#3288](https://github.com/VirtusLab/scala-cli/pull/3288)

### Documentation changes
* Fix broken example in `//> using dep` reference doc by [@Gedochao](https://github.com/Gedochao) in [#3281](https://github.com/VirtusLab/scala-cli/pull/3281)
* Mention distribution limitations in the Scala CLI 1.5.3 release notes by [@Gedochao](https://github.com/Gedochao) in [#3286](https://github.com/VirtusLab/scala-cli/pull/3286)
* Back port of documentation changes to main by [@github-actions](https://github.com/github-actions) in [#3287](https://github.com/VirtusLab/scala-cli/pull/3287)

### Updates
* Update `mill-native-image` to 0.1.29 by [@Gedochao](https://github.com/Gedochao) in [#3278](https://github.com/VirtusLab/scala-cli/pull/3278)
* Update expecty to 0.17.0 by [@scala-steward](https://github.com/scala-steward) in [#3277](https://github.com/VirtusLab/scala-cli/pull/3277)
* Update Bloop to 2.0.5 by [@Gedochao](https://github.com/Gedochao) in [#3276](https://github.com/VirtusLab/scala-cli/pull/3276)
* Update dependency to 0.2.5 by [@scala-steward](https://github.com/scala-steward) in [#3269](https://github.com/VirtusLab/scala-cli/pull/3269)
* Update `coursier` to 2.1.17 by [@Gedochao](https://github.com/Gedochao) in [#3275](https://github.com/VirtusLab/scala-cli/pull/3275)
* Update SBT to 1.10.5 by [@Gedochao](https://github.com/Gedochao) in [#3280](https://github.com/VirtusLab/scala-cli/pull/3280)
* Update `java-class-name` to 0.1.4 by [@Gedochao](https://github.com/Gedochao) in [#3284](https://github.com/VirtusLab/scala-cli/pull/3284)
* Update scala-cli.sh launcher for 1.5.3 by [@Gedochao](https://github.com/Gedochao) in [#3285](https://github.com/VirtusLab/scala-cli/pull/3285)
* Update Scala Native to 0.5.6 by [@Gedochao](https://github.com/Gedochao) in [#3295](https://github.com/VirtusLab/scala-cli/pull/3295)
* Update Mill to 0.11.13 by [@Gedochao](https://github.com/Gedochao) in [#3296](https://github.com/VirtusLab/scala-cli/pull/3296)
* Update coursier to 2.1.17 for Linux arm64 builds by [@Gedochao](https://github.com/Gedochao) in [#3298](https://github.com/VirtusLab/scala-cli/pull/3298)
* Update coursier/dependency to 0.3.1 by [@Gedochao](https://github.com/Gedochao) in [#3297](https://github.com/VirtusLab/scala-cli/pull/3297)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.5.3...v1.5.4

## [v1.5.3](https://github.com/VirtusLab/scala-cli/releases/tag/v1.5.3)

This is a hotfix release, which makes all the fixes and enhancements of Scala CLI 1.5.2 available through most standard distribution channels (rather than just Maven Central).
For the main release notes, please refer to the [v1.5.2 ones](#v152).

### Distribution limitations
Due to technical difficulties within our release pipeline, Scala CLI 1.5.3 is **not** available via the following channels:
- `yum` (on RedHat/Cent OS/Fedora)
- `SDKMAN!`

We have followed up with a 1.5.4 hotfix release to address this issue.

### Hot-fixes 
- Tag failing native packager tests as flaky by [@Gedochao](https://github.com/Gedochao) in [#3270](https://github.com/VirtusLab/scala-cli/pull/3270)
- Make publishing depend on all integration tests & docs tests by [@Gedochao](https://github.com/Gedochao) in [#3272](https://github.com/VirtusLab/scala-cli/pull/3272)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.5.2...v1.5.3

## [v1.5.2](https://github.com/VirtusLab/scala-cli/releases/tag/v1.5.2)

### Scala CLI 1.5.2 will only be available on JVM
Due to technical difficulties with our release pipeline, Scala CLI 1.5.2 was only released as a JVM launcher on Maven Central. While it can be used as such, we followed it up with a hotfix 1.5.3 release, which should be available through all standard distribution channels.

```bash
scala-cli --cli-version 1.5.2 --version
# Scala CLI version: 1.5.2
# Scala version (default): 3.5.2
```

### `--source` is now deprecated and scheduled for removal in Scala CLI v1.6.x
Due to how easy it is to confuse `--source` (the command line option for producing source JARs 
with the `package` sub-command) and `-source` (the Scala compiler option, which can also be passed 
as `--source` in recent Scala 3 versions), using the former is now deprecated, and will likely be removed 
in Scala CLI v1.6.x.

```bash ignore
scala-cli --power package --source .                       
# [warn] The --source option alias has been deprecated and may be removed in a future version.
# (...)
```

Do note that the deprecation (and future removal) only affects the option alias.
The feature of packaging source JARs remains unchanged.
It is now recommended to switch to using the `--src` alias instead.

```bash ignore
scala-cli --power package --src .  
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3257](https://github.com/VirtusLab/scala-cli/pull/3257).

### Support for Scala 3.5.2
This Scala CLI version switches the default Scala version to 3.5.2.

```bash
scala-cli version
# Scala CLI version: 1.5.2
# Scala version (default): 3.5.2
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3230](https://github.com/VirtusLab/scala-cli/pull/3230).

### (experimental) Initial support for emitting Wasm with a command line option and a directive
It is now possible to emit Wasm via Scala.js with the `//> using jsEmitWasm` directive:
```scala title=wasm.sc compile power
//> using platform js
//> using jsEmitWasm
//> using jsModuleKind es
//> using jsModuleSplitStyleStr fewestmodules
println("Hello")
```
Or with the `--js-emit-wasm` command line option:
```bash ignore
scala-cli --power package wasm.sc --js --js-emit-wasm
# The `--js-emit-wasm` option is experimental
# Please bear in mind that non-ideal user experience should be expected.
# If you encounter any bugs or have feedback to share, make sure to reach out to the maintenance team at https://github.com/VirtusLab/scala-cli
# Compiling project (Scala 3.5.2, Scala.js 1.17.0)
# Compiled project (Scala 3.5.2, Scala.js 1.17.0)
# Wrote ~/wasm/wasm.js/main.js, run it with
#   node ./wasm.js/main.js
tree wasm.js
# wasm.js
# ├── __loader.js
# ├── main.js
# └── main.wasm
# 
# 1 directory, 3 files
```

For more information about Wasm (WebAssembly) support via Scala.js, refer [here](https://www.scala-js.org/doc/project/webassembly.html).

Added by [@Quafadas](https://github.com/Quafadas) in [#3255](https://github.com/VirtusLab/scala-cli/pull/3255).

### Features
* Add a `--js-emit-wasm` option and a corresponding `using` directive by [@Quafadas](https://github.com/Quafadas) in [#3255](https://github.com/VirtusLab/scala-cli/pull/3255)

### Deprecations
* Deprecate the `--source` command line option for the package sub-command by [@Gedochao](https://github.com/Gedochao) in [#3257](https://github.com/VirtusLab/scala-cli/pull/3257)

### Fixes
* Fix `--watch` to work correctly with changing `using` directives & sources requiring code generation (scripts, markdown, etc) by [@Gedochao](https://github.com/Gedochao) in [#3218](https://github.com/VirtusLab/scala-cli/pull/3218)
* Ensure resource directories passed via a using directive aren't ignored in `--watch` mode by [@Gedochao](https://github.com/Gedochao) in [#3221](https://github.com/VirtusLab/scala-cli/pull/3221)
* Ensure consecutive `-Wconf:*` flags are not ignored by [@Gedochao](https://github.com/Gedochao) in [#3245](https://github.com/VirtusLab/scala-cli/pull/3245)

### Documentation changes
* Mention the `Fix` command in the `Using directives` guide by [@dabrowski-adam](https://github.com/dabrowski-adam) in [#3239](https://github.com/VirtusLab/scala-cli/pull/3239)
* Back port of documentation changes to main by [@github-actions](https://github.com/github-actions) in [#3242](https://github.com/VirtusLab/scala-cli/pull/3242)

### Updates
* Update scala-cli.sh launcher for 1.5.1 by [@github-actions](https://github.com/github-actions) in [#3217](https://github.com/VirtusLab/scala-cli/pull/3217)
* Update sttp to 3.10.0 by [@scala-steward](https://github.com/scala-steward) in [#3219](https://github.com/VirtusLab/scala-cli/pull/3219)
* Update asm to 9.7.1 by [@scala-steward](https://github.com/scala-steward) in [#3223](https://github.com/VirtusLab/scala-cli/pull/3223)
* Update bloop-rifle_2.13 to 2.0.3 by [@scala-steward](https://github.com/scala-steward) in [#3225](https://github.com/VirtusLab/scala-cli/pull/3225)
* Update bloop-config_2.13 to 2.1.0 by [@scala-steward](https://github.com/scala-steward) in [#3228](https://github.com/VirtusLab/scala-cli/pull/3228)
* chore: Update next to 3.5.2-RC2 by [@tgodzik](https://github.com/tgodzik) in [#3224](https://github.com/VirtusLab/scala-cli/pull/3224)
* Update `coursier` to 2.1.14 by [@scala-steward](https://github.com/scala-steward) in [#3226](https://github.com/VirtusLab/scala-cli/pull/3226)
* Update core_2.13 to 3.10.1 by [@scala-steward](https://github.com/scala-steward) in [#3229](https://github.com/VirtusLab/scala-cli/pull/3229)
* Update `os-lib` to 0.11.2 by [@Gedochao](https://github.com/Gedochao) in [#3232](https://github.com/VirtusLab/scala-cli/pull/3232)
* Update sbt, scripted-plugin to 1.10.3 by [@scala-steward](https://github.com/scala-steward) in [#3235](https://github.com/VirtusLab/scala-cli/pull/3235)
* Update dependency to 0.2.4 by [@scala-steward](https://github.com/scala-steward) in [#3234](https://github.com/VirtusLab/scala-cli/pull/3234)
* Bump Scala Next to 3.5.2 by [@Gedochao](https://github.com/Gedochao) in [#3230](https://github.com/VirtusLab/scala-cli/pull/3230)
* Update os-lib to 0.11.3 by [@scala-steward](https://github.com/scala-steward) in [#3240](https://github.com/VirtusLab/scala-cli/pull/3240)
* Set Scala 3.5.2 as the latest announced Scala Next version by [@Gedochao](https://github.com/Gedochao) in [#3243](https://github.com/VirtusLab/scala-cli/pull/3243)
* Set Scala 3.6.1 as the Next RC version (which it effectively is) by [@Gedochao](https://github.com/Gedochao) in [#3244](https://github.com/VirtusLab/scala-cli/pull/3244)
* Update dependencies in `gh-action.md` examples by [@kubukoz](https://github.com/kubukoz) in [#3249](https://github.com/VirtusLab/scala-cli/pull/3249)
* Bump `scala-js-cli` to 1.17.0.1 by [@Gedochao](https://github.com/Gedochao) in [#3252](https://github.com/VirtusLab/scala-cli/pull/3252)

## New Contributors
* [@dabrowski-adam](https://github.com/dabrowski-adam) made their first contribution in [#3239](https://github.com/VirtusLab/scala-cli/pull/3239)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.5.1...v1.5.2

## [v1.5.1](https://github.com/VirtusLab/scala-cli/releases/tag/v1.5.1)

### Support for Scala 3.5.1, 3.3.4, 2.13.15 and 2.12.20
This Scala CLI version switches the default Scala version to 3.5.1.

```bash
scala-cli version
# Scala CLI version: 1.5.1
# Scala version (default): 3.5.1
```

It has also been tested with Scala 3.3.4, 2.13.15 and 2.12.20.
The Scala CLI internals are now built with Scala 3.3.4.

### Features
* Apply increased verbosity when compiling via BSP by [@Gedochao](https://github.com/Gedochao) in [#3202](https://github.com/VirtusLab/scala-cli/pull/3202)

### Fixes
* improvement: Use distinct on ScalacOpt by [@tgodzik](https://github.com/tgodzik) in [#3139](https://github.com/VirtusLab/scala-cli/pull/3139)
* bugfix: Check if last segment of path exists by [@tgodzik](https://github.com/tgodzik) in [#3131](https://github.com/VirtusLab/scala-cli/pull/3131)
* bugfix: Fix duplicate options detection by [@tgodzik](https://github.com/tgodzik) in [#3151](https://github.com/VirtusLab/scala-cli/pull/3151)
* bugfix: Also deduplicate if options split by space by [@tgodzik](https://github.com/tgodzik) in [#3154](https://github.com/VirtusLab/scala-cli/pull/3154)
* Fix `setup-ide` for `--cli-version` by [@Gedochao](https://github.com/Gedochao) in [#3161](https://github.com/VirtusLab/scala-cli/pull/3161)
* Ensure main classes from inputs take precedence before those found in JARs added to the class path by [@Gedochao](https://github.com/Gedochao) in [#3165](https://github.com/VirtusLab/scala-cli/pull/3165)
* Ensure that passing Java props into Scala CLI as launcher args would also pass it into BSP configuration by [@Gedochao](https://github.com/Gedochao) in [#3169](https://github.com/VirtusLab/scala-cli/pull/3169)
* NIT fixes for the `export` sub-command by [@Gedochao](https://github.com/Gedochao) in [#3197](https://github.com/VirtusLab/scala-cli/pull/3197)
* Ensure `--version` passed to the default command works with `--offline` by [@Gedochao](https://github.com/Gedochao) in [#3207](https://github.com/VirtusLab/scala-cli/pull/3207)

### Documentation changes
* Docs: Fix suppress option for directives-in-multiple-files warning by [@mims-github](https://github.com/mims-github) in [#3133](https://github.com/VirtusLab/scala-cli/pull/3133)
* Doc: Tips on how to list available JVMs using coursier by [@jatcwang](https://github.com/jatcwang) in [#3129](https://github.com/VirtusLab/scala-cli/pull/3129)
* Back port of documentation changes to main by [@github-actions](https://github.com/github-actions) in [#3160](https://github.com/VirtusLab/scala-cli/pull/3160)
* Use Scala 3 in the Scala Native gif by [@Gedochao](https://github.com/Gedochao) in [#3195](https://github.com/VirtusLab/scala-cli/pull/3195)

### Build and internal changes
* Add tests for `setup-ide` with `--cli-version` by [@Gedochao](https://github.com/Gedochao) in [#3163](https://github.com/VirtusLab/scala-cli/pull/3163)
* Change how help is referenced to avoid initialization oddness & update `case-app` to 2.1.0-M29 by [@coreyoconnor](https://github.com/coreyoconnor) in [#3152](https://github.com/VirtusLab/scala-cli/pull/3152)
* Adjust tests for Scala 3.3.4 by [@Gedochao](https://github.com/Gedochao) in [#3164](https://github.com/VirtusLab/scala-cli/pull/3164)
* NIT Refactor existing `--watch` tests by [@Gedochao](https://github.com/Gedochao) in [#3175](https://github.com/VirtusLab/scala-cli/pull/3175)
* Generate an empty JUnit report when no tests were run, rather than fail by [@Gedochao](https://github.com/Gedochao) in [#3179](https://github.com/VirtusLab/scala-cli/pull/3179)
* NIT Extract REPL tests relying on Ammonite into dedicated traits by [@Gedochao](https://github.com/Gedochao) in [#3209](https://github.com/VirtusLab/scala-cli/pull/3209)

### Updates
* Update scala-cli.sh launcher for 1.5.0 by [@github-actions](https://github.com/github-actions) in [#3125](https://github.com/VirtusLab/scala-cli/pull/3125)
* Bump webpack from 5.89.0 to 5.94.0 in /website by [@dependabot](https://github.com/dependabot) in [#3136](https://github.com/VirtusLab/scala-cli/pull/3136)
* Bump micromatch from 4.0.5 to 4.0.8 in /website by [@dependabot](https://github.com/dependabot) in [#3135](https://github.com/VirtusLab/scala-cli/pull/3135)
* Update os-lib to 0.10.5 by [@scala-steward](https://github.com/scala-steward) in [#3140](https://github.com/VirtusLab/scala-cli/pull/3140)
* Update Scala Next latest announced version to 3.5.0 by [@Gedochao](https://github.com/Gedochao) in [#3145](https://github.com/VirtusLab/scala-cli/pull/3145)
* Update Scala 2.12 to 2.12.20 by [@Gedochao](https://github.com/Gedochao) in [#3144](https://github.com/VirtusLab/scala-cli/pull/3144)
* Update Scala CLI as `scala` related docs  by [@Gedochao](https://github.com/Gedochao) in [#3155](https://github.com/VirtusLab/scala-cli/pull/3155)
* Update os-lib to 0.10.6 by [@scala-steward](https://github.com/scala-steward) in [#3159](https://github.com/VirtusLab/scala-cli/pull/3159)
* Update coursier to 2.1.11 by [@scala-steward](https://github.com/scala-steward) in [#3166](https://github.com/VirtusLab/scala-cli/pull/3166)
* Update coursier to 2.1.12 by [@scala-steward](https://github.com/scala-steward) in [#3174](https://github.com/VirtusLab/scala-cli/pull/3174)
* Update ammonite to 3.0.0-M2-30-486378af by [@scala-steward](https://github.com/scala-steward) in [#3172](https://github.com/VirtusLab/scala-cli/pull/3172)
* Update sbt to 1.10.2 by [@scala-steward](https://github.com/scala-steward) in [#3180](https://github.com/VirtusLab/scala-cli/pull/3180)
* Update munit to 1.0.2 by [@scala-steward](https://github.com/scala-steward) in [#3176](https://github.com/VirtusLab/scala-cli/pull/3176)
* Bump `scala-cli-signing` to 0.2.4 by [@Gedochao](https://github.com/Gedochao) in [#3183](https://github.com/VirtusLab/scala-cli/pull/3183)
* Bump `coursier` to 2.1.13 and `mill-native-image` to 0.1.26 by [@Gedochao](https://github.com/Gedochao) in [#3182](https://github.com/VirtusLab/scala-cli/pull/3182)
* Update Scala Next to 3.5.1 by [@Gedochao](https://github.com/Gedochao) in [#3190](https://github.com/VirtusLab/scala-cli/pull/3190)
* Update Scala 3 Next RC to 3.5.2-RC1 by [@scala-steward](https://github.com/scala-steward) in [#3187](https://github.com/VirtusLab/scala-cli/pull/3187)
* Update Scala 2.13 to 2.13.15 by [@Gedochao](https://github.com/Gedochao) in [#3201](https://github.com/VirtusLab/scala-cli/pull/3201)
* Update guava to 33.3.1-jre by [@scala-steward](https://github.com/scala-steward) in [#3203](https://github.com/VirtusLab/scala-cli/pull/3203)
* chore: Update Bloop to 2.0.2 by [@tgodzik](https://github.com/tgodzik) in [#3192](https://github.com/VirtusLab/scala-cli/pull/3192)
* Update Scala 3 LTS to 3.3.4 by [@Gedochao](https://github.com/Gedochao) in [#3208](https://github.com/VirtusLab/scala-cli/pull/3208)
* Set Scala 3.5.1 as the latest announced version by [@Gedochao](https://github.com/Gedochao) in [#3206](https://github.com/VirtusLab/scala-cli/pull/3206)

## New Contributors
* [@mims-github](https://github.com/mims-github) made their first contribution in [#3133](https://github.com/VirtusLab/scala-cli/pull/3133)
* [@jatcwang](https://github.com/jatcwang) made their first contribution in [#3129](https://github.com/VirtusLab/scala-cli/pull/3129)
* [@coreyoconnor](https://github.com/coreyoconnor) made their first contribution in [#3152](https://github.com/VirtusLab/scala-cli/pull/3152)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.5.0...v1.5.1

## [v1.5.0](https://github.com/VirtusLab/scala-cli/releases/tag/v1.5.0)

### Support for Scala 3.5.0
This Scala CLI version switches the default Scala version to 3.5.0.

```bash
scala-cli version
# Scala CLI version: 1.5.0
# Scala version (default): 3.5.0
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3093](https://github.com/VirtusLab/scala-cli/pull/3093).

### Support for Scala Native 0.5.5
This Scala CLI version switches the default Scala Native version to 0.5.5.

```bash
scala-cli -e 'println("Hello from Scala Native 0.5.5!")' --native
# Compiling project (Scala 3.5.0, Scala Native 0.5.5)
# Compiled project (Scala 3.5.0, Scala Native 0.5.5)
# [info] Linking (multithreadingEnabled=true, disable if not used) (894 ms)
# [info] Discovered 888 classes and 5407 methods after classloading
# [info] Checking intermediate code (quick) (31 ms)
# [info] Multithreading was not explicitly enabled - initial class loading has not detected any usage of system threads. Multithreading support will be disabled to improve performance.
# [info] Linking (multithreadingEnabled=false) (299 ms)
# [info] Discovered 499 classes and 2497 methods after classloading
# [info] Checking intermediate code (quick) (5 ms)
# [info] Discovered 478 classes and 1912 methods after optimization
# [info] Optimizing (debug mode) (403 ms)
# [info] Produced 9 LLVM IR files
# [info] Generating intermediate code (368 ms)
# [info] Compiling to native code (1565 ms)
# [info] Linking with [pthread, dl]
# [info] Linking native code (immix gc, none lto) (83 ms)
# [info] Postprocessing (0 ms)
# [info] Total (3625 ms)
# Hello from Scala Native 0.5.5!
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3117](https://github.com/VirtusLab/scala-cli/pull/3117).

### (⚡️ experimental) Support for exporting to a Maven project
It is now possible to export a Scala CLI project to Maven.

```bash
scala-cli export --script-snippet 'println("No need to create the pom.xml yourself!")' --mvn --power -o mvn-demo
# Some utilized features are marked as experimental:
#  - `export` sub-command
#  - `--mvn` option
# Please bear in mind that non-ideal user experience should be expected.
# If you encounter any bugs or have feedback to share, make sure to reach out to the maintenance team at https://github.com/VirtusLab/scala-cli
# Exporting to a maven project...
# Exported to: ~/scala-cli-tests/mvn-demo
cd mvn-demo
mvn scala:run -DmainClass=snippet_sc
# (...)
# No need to create the pom.xml yourself!
# [INFO] ------------------------------------------------------------------------
# [INFO] BUILD SUCCESS
# [INFO] ------------------------------------------------------------------------
# [INFO] Total time:  2.589 s
# [INFO] Finished at: 2024-08-22T12:08:36+02:00
# [INFO] ------------------------------------------------------------------------
```

Added by [@yadavan88](https://github.com/yadavan88) in [#3003](https://github.com/VirtusLab/scala-cli/pull/3003).

### Support for launching apps from dependencies without other inputs
It is now possible to launch an app by just specifying its dependency, without the need to provide any source files.
In such a case the build server will not be started, as there's no compilation to be done.
There's also no need to specify the main class, as it's now being detected automatically in dependencies as well.
Do note that explicitly calling the `run` sub-command is necessary here, as otherwise Scala CLI will default to the REPL.

```bash
scala-cli run --dep io.get-coursier:coursier-cli_2.13:2.1.10 -- version
# 2.1.10
```

This can be used similarly to [Coursier's `cs launch`](https://get-coursier.io/docs/cli-launch).

Added by [@kasiaMarek](https://github.com/kasiaMarek) in [#3079](https://github.com/VirtusLab/scala-cli/pull/3079).

### (⚡️ experimental) JMH available in various commands and via `using` directives
Some improvements have been done to the experimental support for JMH (Java Microbenchmark Harness).

The `--jmh` and `--jmh-version` options can now be passed to a number of commands:
- `run`, as it was before (note that when `--jmh` is passed to `run`, the project's main class will default to running the benchmarks rather than the project's default main method; this behaviour is likely to be changed in future versions);
- `compile`, so that a Scala CLI project with benchmarking can be compiled separately from being run;
- `package`, although the resulting artifacts will run the project as normal for now, rather than benchmarks;
- `setup-ide`, so that benchmarking projects can be imported to your IDE of choice;
- `test` and `export` will now also no longer fail with `--jmh`, although no specific implementations for benchmarking are in place there yet.

It is now also possible to control JMH with `using` directives:
- `//> using jmh` allows to enable JMH for the project, being the equivalent of the `--jmh` option.
- `//> using jmhVersion <version>` allows to set the JMH version to use, being the equivalent of the `--jmh-version` option.

```scala compile power
//> using jmh
//> using jmhVersion 1.37
package bench

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(0)
class Benchmarks {
  @Benchmark
  def foo(): Unit = {
    (1L to 1000L).sum
  }
}
```

Expect more improvements in this area in the future.
Also, do play with it and give us feedback about the current implementation!

Added by [@Gedochao](https://github.com/Gedochao) in [#3091](https://github.com/VirtusLab/scala-cli/pull/3091) and [#3118](https://github.com/VirtusLab/scala-cli/pull/3118).

### Support for auto-completions in `fish`
We now have command line auto-completions for the `fish` shell.

Added by [@KristianLentino99](https://github.com/KristianLentino99) in [#3104](https://github.com/VirtusLab/scala-cli/pull/3104).

### `--js-es-module-import-map` no longer requires `--power` mode
A bit of a minor thing, but you can now use the `--js-es-module-import-map` option without enabling `--power` mode.

Added by [@Gedochao](https://github.com/Gedochao) in [#3086](https://github.com/VirtusLab/scala-cli/pull/3086).

### Features
* Add support for exporting to a Maven project by [@yadavan88](https://github.com/yadavan88) in [#3003](https://github.com/VirtusLab/scala-cli/pull/3003)
* improvement: allow to run main class from deps with no inputs by [@kasiaMarek](https://github.com/kasiaMarek) in [#3079](https://github.com/VirtusLab/scala-cli/pull/3079)
* Promote `--js-es-module-import-map` to stable by [@Gedochao](https://github.com/Gedochao) in [#3086](https://github.com/VirtusLab/scala-cli/pull/3086)
* Tweak benchmarking with JMH by [@Gedochao](https://github.com/Gedochao) in [#3091](https://github.com/VirtusLab/scala-cli/pull/3091)
* Add support for fish auto-completions by [@KristianLentino99](https://github.com/KristianLentino99) in [#3104](https://github.com/VirtusLab/scala-cli/pull/3104)
* Add directives for JMH by [@Gedochao](https://github.com/Gedochao) in [#3118](https://github.com/VirtusLab/scala-cli/pull/3118)

### Fixes
* bugfix: Exclude sourcecode dependency by [@tgodzik](https://github.com/tgodzik) in [#3094](https://github.com/VirtusLab/scala-cli/pull/3094)
* bugfix: Exclude both sourcecode and collection-compat correctly by [@tgodzik](https://github.com/tgodzik) in [#3105](https://github.com/VirtusLab/scala-cli/pull/3105)
* Make package command handle directories in extra classpath by [@joan38](https://github.com/joan38) in [#3096](https://github.com/VirtusLab/scala-cli/pull/3096)
* Add extra try-catch clause + extra logging in `LocalRepo` by [@Gedochao](https://github.com/Gedochao) in [#3114](https://github.com/VirtusLab/scala-cli/pull/3114)
* Fix/changing options from sources should not require reload by [@MaciejG604](https://github.com/MaciejG604) in [#3112](https://github.com/VirtusLab/scala-cli/pull/3112)
* fix: remove the --release flag by [@kasiaMarek](https://github.com/kasiaMarek) in [#3119](https://github.com/VirtusLab/scala-cli/pull/3119)
* Remove adding test options to the project/build target name hash by [@MaciejG604](https://github.com/MaciejG604) in [#3107](https://github.com/VirtusLab/scala-cli/pull/3107)

### Internal changes
* Make the `publish` CI job depend on `jvm-tests-5` (Scala 3 Next RC test suite) by [@Gedochao](https://github.com/Gedochao) in [#3078](https://github.com/VirtusLab/scala-cli/pull/3078)
* Include scanning the `.exe` launcher in the release procedure by [@Gedochao](https://github.com/Gedochao) in [#3081](https://github.com/VirtusLab/scala-cli/pull/3081)
* refactor: Switch to original fork of Bloop by [@tgodzik](https://github.com/tgodzik) in [#3020](https://github.com/VirtusLab/scala-cli/pull/3020)
* Extract used Java versions to constants by [@Gedochao](https://github.com/Gedochao) in [#3087](https://github.com/VirtusLab/scala-cli/pull/3087)
* NIT Extract bsp testing utils to a helper trait by [@Gedochao](https://github.com/Gedochao) in [#3092](https://github.com/VirtusLab/scala-cli/pull/3092)
* Fix/simplify code by [@MaciejG604](https://github.com/MaciejG604) in [#3106](https://github.com/VirtusLab/scala-cli/pull/3106)

### Documentation changes
* Add more env vars & generate reference docs for them by [@Gedochao](https://github.com/Gedochao) in [#3075](https://github.com/VirtusLab/scala-cli/pull/3075)

### Updates
* Update scala-cli.sh launcher for 1.4.3 by [@github-actions](https://github.com/github-actions) in [#3073](https://github.com/VirtusLab/scala-cli/pull/3073)
* Update bloop-config_2.13 to 2.0.3 by [@scala-steward](https://github.com/scala-steward) in [#3072](https://github.com/VirtusLab/scala-cli/pull/3072)
* Update Scala toolkit to 0.5.0 by [@Gedochao](https://github.com/Gedochao) in [#3076](https://github.com/VirtusLab/scala-cli/pull/3076)
* Update Typelevel toolkit to 0.1.27 by [@Gedochao](https://github.com/Gedochao) in [#3077](https://github.com/VirtusLab/scala-cli/pull/3077)
* Update Scala 3 Next RC to 3.5.0-RC7 by [@Gedochao](https://github.com/Gedochao) in [#3080](https://github.com/VirtusLab/scala-cli/pull/3080)
* Update bloop-rifle_2.13 to 2.0.0 by [@scala-steward](https://github.com/scala-steward) in [#3108](https://github.com/VirtusLab/scala-cli/pull/3108)
* Update munit to 1.0.1 by [@scala-steward](https://github.com/scala-steward) in [#3100](https://github.com/VirtusLab/scala-cli/pull/3100)
* Update Scala 3 Next to 3.5.0 by [@Gedochao](https://github.com/Gedochao) in [#3093](https://github.com/VirtusLab/scala-cli/pull/3093)
* Update sttp to 3.9.8 by [@scala-steward](https://github.com/scala-steward) in [#3098](https://github.com/VirtusLab/scala-cli/pull/3098)
* Update guava to 33.3.0-jre by [@scala-steward](https://github.com/scala-steward) in [#3113](https://github.com/VirtusLab/scala-cli/pull/3113)
* Update slf4j-nop to 2.0.16 by [@scala-steward](https://github.com/scala-steward) in [#3101](https://github.com/VirtusLab/scala-cli/pull/3101)
* Update Scala 3 Next RC to 3.5.1-RC2 by [@scala-steward](https://github.com/scala-steward) in [#3099](https://github.com/VirtusLab/scala-cli/pull/3099)
* Update Scala Native to 0.5.5 by [@Gedochao](https://github.com/Gedochao) in [#3117](https://github.com/VirtusLab/scala-cli/pull/3117)
* Update os-lib to 0.10.4 by [@scala-steward](https://github.com/scala-steward) in [#3121](https://github.com/VirtusLab/scala-cli/pull/3121)
* Update mill-main to 0.11.12 by [@scala-steward](https://github.com/scala-steward) in [#3120](https://github.com/VirtusLab/scala-cli/pull/3120)

## New Contributors
* @KristianLentino99 made their first contribution in [#3104](https://github.com/VirtusLab/scala-cli/pull/3104)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.4.3...v1.5.0

## [v1.4.3](https://github.com/VirtusLab/scala-cli/releases/tag/v1.4.3)

This release is a hotfix for v1.4.2, which due to technical difficulties was not released to Maven Central 
(and, as an extension, wasn't available as a JAR). 

All changes introduced by v1.4.2 are included in this release.

### Internal changes
* Ensure the `publish` step to be necessary for updating the native packages upon release by [@Gedochao](https://github.com/Gedochao) in [#3067](https://github.com/VirtusLab/scala-cli/pull/3067)

### Updates
* Update mill-main to 0.11.10 by [@scala-steward](https://github.com/scala-steward) in [#3060](https://github.com/VirtusLab/scala-cli/pull/3060)
* Update mill-main to 0.11.11 by [@Gedochao](https://github.com/Gedochao) in [#3068](https://github.com/VirtusLab/scala-cli/pull/3068)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.4.2...v1.4.3

## [v1.4.2](https://github.com/VirtusLab/scala-cli/releases/tag/v1.4.2)

:::caution
v1.4.2 encountered certain difficulties during its release, which made it unavailable on Maven Central.
It is recommended to upgrade directly to v1.4.3 or higher, rather than use it.
:::

### Environment variable help with `--env-help`
You can now list environment variables used internally with the `--envs-help` flag.
This does include some environment variable used by Scala CLI's dependencies (like Coursier, Bloop, etc.), but should not be treated as an exhaustive list.

```bash
scala-cli --env-help --power
# The following is the list of environment variables used and recognized by Scala CLI.
# It should by no means be treated as an exhaustive list.
# Some tools and libraries Scala CLI integrates with may have their own, which may or may not be listed here.
# 
# Scala CLI
#   SCALA_CLI_CONFIG              Scala CLI configuration file path
#   SCALA_CLI_HOME                Scala CLI home directory
#   SCALA_CLI_INTERACTIVE         Interactive mode toggle
#   SCALA_CLI_INTERACTIVE_INPUTS  Interactive mode inputs
#   SCALA_CLI_POWER               Power mode toggle
#   SCALA_CLI_PRINT_STACK_TRACES  Print stack traces toggle
#   SCALA_CLI_SODIUM_JNI_ALLOW    Allow to load libsodiumjni
#   SCALA_CLI_VENDORED_ZIS        Toggle io.github.scala_cli.zip.ZipInputStream
# 
# Java
#   JAVA_HOME                     Java installation directory
#   JAVA_OPTS                     Java options
#   JDK_JAVA_OPTIONS              JDK Java options
# 
# Coursier
#   COURSIER_CACHE                Coursier cache location
#   COURSIER_MODE                 Coursier mode (can be set to 'offline')
# 
# Spark
#   SPARK_HOME                    (power) Spark installation directory
# 
# Miscellaneous
#   PATH                          The app path variable
#   DYLD_LIBRARY_PATH             Runtime library paths on Mac OS X
#   LD_LIBRARY_PATH               Runtime library paths on Linux
#   PATHEXT                       Executable file extensions on Windows
#   SHELL                         The currently used shell
#   VCVARSALL                     Visual C++ Redistributable Runtimes
#   ZDOTDIR                       Zsh configuration directory
# 
# Internal
#   CI                            (power) Marker for running on the CI
```

Added by [@Gedochao](https://github.com/Gedochao) in [#3055.](https://github.com/VirtusLab/scala-cli/pull/3055.)

### Features
* Add environment variable help under `--envs-help` & refactor environment variable usage by [@Gedochao](https://github.com/Gedochao) in [#3055](https://github.com/VirtusLab/scala-cli/pull/3055)

### Fixes
* Fix default scaladoc config, so that id doesn't break all scaladoc links by [@KacperFKorban](https://github.com/KacperFKorban) in [#3041](https://github.com/VirtusLab/scala-cli/pull/3041)
* Fix the REPL crashing when a dependency's classpath is called by a macro by [@Gedochao](https://github.com/Gedochao) in [#3043](https://github.com/VirtusLab/scala-cli/pull/3043)
* Fix Mill export for projects with just the test scope by [@Gedochao](https://github.com/Gedochao) in [#3046](https://github.com/VirtusLab/scala-cli/pull/3046)
* Ensure `--cli-default-scala-version` is respected by `--scalac-help` by [@Gedochao](https://github.com/Gedochao) in [#3048](https://github.com/VirtusLab/scala-cli/pull/3048)
* Fix `generate-linux-arm64-native-launcher` by [@Gedochao](https://github.com/Gedochao) in [#3053](https://github.com/VirtusLab/scala-cli/pull/3053)

### Internal changes
* Prevent some flaky tests from failing on the CI by [@Gedochao](https://github.com/Gedochao) in [#3049](https://github.com/VirtusLab/scala-cli/pull/3049)
* Switch to GitHub M1/`aarch64` runners on the CI by [@Gedochao](https://github.com/Gedochao) in [#3050](https://github.com/VirtusLab/scala-cli/pull/3050)
* Fix Scala 2 nightly test failures by tagging them as flaky or skipping by [@Gedochao](https://github.com/Gedochao) in [#3064](https://github.com/VirtusLab/scala-cli/pull/3064)

### Updates
* Update scala-cli.sh launcher for 1.4.1 by [@github-actions](https://github.com/features/actions) in [#3039](https://github.com/VirtusLab/scala-cli/pull/3039)
* Update ammonite to 3.0.0-M2-15-9bed9700 by [@scala-steward](https://github.com/scala-steward) in [#3059](https://github.com/VirtusLab/scala-cli/pull/3059)
* Update metaconfig-typesafe-config to 0.13.0 by [@scala-steward](https://github.com/scala-steward) in [#3058](https://github.com/VirtusLab/scala-cli/pull/3058)
* Update semanticdb-shared_2.13.14 to 4.9.9 by [@scala-steward](https://github.com/scala-steward) in [#3063](https://github.com/VirtusLab/scala-cli/pull/3063)
* Update scalafmt-cli_2.13, scalafmt-core to 3.8.3 by [@scala-steward](https://github.com/scala-steward) in [#3062](https://github.com/VirtusLab/scala-cli/pull/3062)
* Update os-lib to 0.10.3 by [@scala-steward](https://github.com/scala-steward) in [#3061](https://github.com/VirtusLab/scala-cli/pull/3061)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.4.1...v1.4.2

## [v1.4.1](https://github.com/VirtusLab/scala-cli/releases/tag/v1.4.1)

### Pass compiler args as an `@argument` file
You can shorten or simplify a Scala CLI command by using an `@argument` file to specify a text file that contains compiler arguments. 
```text title=args.txt
-d
outputDirectory
```
The feature may help to work around the [Windows command line character limit](https://learn.microsoft.com/en-us/troubleshoot/windows-client/shell-experience/command-line-string-limitation), 
among other things, making sure your scripts run on any operating system of your choice.
```bash
scala-cli run -e 'println("Hey, I am using an @args file!")' @args.txt
```
The feature works similarly to the [command-line argument files feature of Java 9](https://docs.oracle.com/javase/9/tools/java.htm#JSWOR-GUID-4856361B-8BFD-4964-AE84-121F5F6CF111) 
and fixes backwards compatibility with the old `scala` runner (pre-Scala-3.5.0).

Added by [@kasiaMarek](https://github.com/kasiaMarek) in [#3012](https://github.com/VirtusLab/scala-cli/pull/3012)

### Explicitly enable or disable multithreading in Scala Native
It is now possible to explicitly enable or disable multithreading in Scala Native builds.
You can do it by setting the `//> using nativeMultithreading` directive:
```scala title=native_multithreading.sc
//> using platform native
//> using nativeMultithreading
import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
val promise = Promise[Int]()
val thread = new Thread(new Runnable {
    def run(): Unit = {
      Thread.sleep(100)
      promise.success(42)
    }
  })
thread.start()
val result = Await.result(promise.future, 2.seconds)
println(result)
```
Or the `--native-multithreading` command line option:
```bash
scala-cli run native_multithreading.sc --native --native-multithreading
```
Added by [@Gedochao](https://github.com/Gedochao) in [#3011.](https://github.com/VirtusLab/scala-cli/pull/3011.)

### Features
* Add a command line option & directive for enabling/disabling Scala Native multithreading by [@Gedochao](https://github.com/Gedochao) in [#3011](https://github.com/VirtusLab/scala-cli/pull/3011)
* feat: allow to pass scalac options using files by [@kasiaMarek](https://github.com/kasiaMarek) in [#3012](https://github.com/VirtusLab/scala-cli/pull/3012)

### Fixes
* fix for 2954 running script in root dir by [@philwalk](https://github.com/philwalk) in [#2988](https://github.com/VirtusLab/scala-cli/pull/2988)
* Pass `javaHome` to Bloop by [@kasiaMarek](https://github.com/kasiaMarek) in [#2985](https://github.com/VirtusLab/scala-cli/pull/2985)
* bugfix: Print info diagnostics by [@tgodzik](https://github.com/tgodzik) in [#2990](https://github.com/VirtusLab/scala-cli/pull/2990)
* Ensure BSP respects --power mode by [@Gedochao](https://github.com/Gedochao) in [#2997](https://github.com/VirtusLab/scala-cli/pull/2997)
* Add Scala to pure Java test builds by [@Gedochao](https://github.com/Gedochao) in [#3009](https://github.com/VirtusLab/scala-cli/pull/3009)
* Fix --offline mode for scala-cli as scala installation via coursier by [@Gedochao](https://github.com/Gedochao) in [#3029](https://github.com/VirtusLab/scala-cli/pull/3029)

### Documentation changes
* Fix typo in docs by [@ghostdogpr](https://github.com/ghostdogpr) in [#2996](https://github.com/VirtusLab/scala-cli/pull/2996)
* docs: remove `.` from command snippet by [@spaceunifyfifty](https://github.com/spaceunifyfifty) in [#2998](https://github.com/VirtusLab/scala-cli/pull/2998)

### Updates
* Update scala-cli.sh launcher for 1.4.0 by [@github-actions](https://github.com/features/actions) in [#2992](https://github.com/VirtusLab/scala-cli/pull/2992)
* Update winget-releaser to latest by [@vedantmgoyal9](https://github.com/vedantmgoyal9) in [#2991](https://github.com/VirtusLab/scala-cli/pull/2991)
* Update ammonite to 3.0.0-M2-13-23a8ef64 by [@scala-steward](https://github.com/scala-steward) in [#2989](https://github.com/VirtusLab/scala-cli/pull/2989)
* Update Scala 3 Next RC to 3.5.0-RC2 by [@scala-steward](https://github.com/scala-steward) in [#2981](https://github.com/VirtusLab/scala-cli/pull/2981)
* chore: Bump outdated `javac-semanticdb` plugin to 0.10.0 by [@tgodzik](https://github.com/tgodzik) in [#3004](https://github.com/VirtusLab/scala-cli/pull/3004)
* Update Scala 3 Next RC to 3.5.0-RC3 by [@scala-steward](https://github.com/scala-steward) in [#3002](https://github.com/VirtusLab/scala-cli/pull/3002)
* Update sbt to 1.10.1 by [@scala-steward](https://github.com/scala-steward) in [#3015](https://github.com/VirtusLab/scala-cli/pull/3015)
* Bump Scala 3 Next RC to 3.5.0-RC4 by [@Gedochao](https://github.com/Gedochao) in [#3018](https://github.com/VirtusLab/scala-cli/pull/3018)
* Swap `scalameta` `trees` for `semanticdb-shared` & bump `scalameta` to 4.9.8 by [@Gedochao](https://github.com/Gedochao) in [#3017](https://github.com/VirtusLab/scala-cli/pull/3017)
* Update Scala 3 Next RC to 3.5.1-RC1 by [@scala-steward](https://github.com/scala-steward) in [#3027](https://github.com/VirtusLab/scala-cli/pull/3027)

## New Contributors
* [@vedantmgoyal9](https://github.com/vedantmgoyal9) made their first contribution in [#2991](https://github.com/VirtusLab/scala-cli/pull/2991)
* [@ghostdogpr](https://github.com/ghostdogpr) made their first contribution in [#2996](https://github.com/VirtusLab/scala-cli/pull/2996)
* [@spaceunifyfifty](https://github.com/spaceunifyfifty) made their first contribution in [#2998](https://github.com/VirtusLab/scala-cli/pull/2998)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.4.0...v1.4.1

## [v1.4.0](https://github.com/VirtusLab/scala-cli/releases/tag/v1.4.0)

### Running the REPL with the test scope included
It is now possible to start the Scala REPL with access to the test scope. 
To do so, it's enough to pass the `--test` flag with the `repl` sub-command.

```scala title=ReplTestScopeExample.test.scala
package example
object ReplTestScopeExample {
  def message: String = "calling test scope from repl"
}
```

```bash ignore
scala-cli repl ReplTestScopeExample.test.scala --test
# Compiling project (test, Scala 3.4.2, JVM (17))
# Compiled project (test, Scala 3.4.2, JVM (17))
# Welcome to Scala 3.4.2 (17, Java OpenJDK 64-Bit Server VM).
# Type in expressions for evaluation. Or try :help.
#                                                                                                                                          
# scala> example.ReplTestScopeExample.message
# val res0: String = calling test scope from repl
#                                                                                                                                          
# scala> 
```

Added by [@Gedochao](https://github.com/Gedochao) in [#2971](https://github.com/VirtusLab/scala-cli/pull/2971.)

### The `using jvm` directives are now always respected
Formerly, if the build server (Bloop) was running on an older JVM than the one specified in a `using jvm` directive, the directive wouldn't be respected. We now restart the build server based on both the directive and the respective command line option (`--jvm`).

```java title=Simple.java
//> using jvm 22
//> using javacOpt --enable-preview -Xlint:preview
//> using javaOpt --enable-preview
//> using mainClass Simple

void main() {
    System.out.println("Hello from Java 22");
}
```

Added by [@kasiaMarek](https://github.com/kasiaMarek) in [#2972](https://github.com/VirtusLab/scala-cli/pull/2972)

### Support for Scala Native 0.5.4
This Scala CLI version adds support for Scala Native 0.5.4.
Native platform builds will now use 0.5.4 as the default version.

```bash
scala-cli -e 'println("Hello, Scala Native!")' --native
# Compiling project (Scala 3.4.2, Scala Native 0.5.4)
# Compiled project (Scala 3.4.2, Scala Native 0.5.4)
# [info] Linking (multithreadingEnabled=true, disable if not used) (902 ms)
# [info] Discovered 882 classes and 5384 methods after classloading
# [info] Checking intermediate code (quick) (37 ms)
# [info] Multithreading was not explicitly enabled - initial class loading has not detected any usage of system threads. Multithreading support will be disabled to improve performance.
# [info] Linking (multithreadingEnabled=false) (292 ms)
# [info] Discovered 499 classes and 2497 methods after classloading
# [info] Checking intermediate code (quick) (10 ms)
# [info] Discovered 478 classes and 1912 methods after optimization
# [info] Optimizing (debug mode) (445 ms)
# [info] Produced 9 LLVM IR files
# [info] Generating intermediate code (353 ms)
# [info] Compiling to native code (1619 ms)
# [info] Linking with [pthread, dl]
# [info] Linking native code (immix gc, none lto) (137 ms)
# [info] Postprocessing (0 ms)
# [info] Total (3753 ms)
# Hello, Scala Native!
```

Added by [@scala-steward](https://github.com/scala-steward) in [#2982.](https://github.com/VirtusLab/scala-cli/pull/2982.)

### Scala Toolkit 0.4.0 & 0.3.0 defaults
This Scala CLI version treats Scala Toolkit 0.4.0 as the default version under most circumstances.
```scala
//> using toolkit default
@main def main() = println(os.pwd)
```
This unlocks the Scala Toolkit to be used with Scala Native 0.5.x.

```bash
scala-cli -e 'println(os.pwd)' --toolkit default --native   
# Compiling project (Scala 3.4.2, Scala Native 0.5.4)
# Compiled project (Scala 3.4.2, Scala Native 0.5.4)
# [info] Linking (multithreadingEnabled=true, disable if not used) (1051 ms)
# [info] Discovered 1047 classes and 6745 methods after classloading
# [info] Checking intermediate code (quick) (46 ms)
# [info] Multithreading was not explicitly enabled - initial class loading has not detected any usage of system threads. Multithreading support will be disabled to improve performance.
# [info] Linking (multithreadingEnabled=false) (543 ms)
# [info] Discovered 880 classes and 5417 methods after classloading
# [info] Checking intermediate code (quick) (15 ms)
# [info] Discovered 857 classes and 4238 methods after optimization
# [info] Optimizing (debug mode) (651 ms)
# [info] Produced 9 LLVM IR files
# [info] Generating intermediate code (663 ms)
# [info] Compiling to native code (1621 ms)
# [info] Linking with [pthread, dl]
# [info] Linking native code (immix gc, none lto) (81 ms)
# [info] Postprocessing (0 ms)
# [info] Total (4542 ms)
```

Scala Native 0.4.x has been dropped in Scala Toolkit 0.4.0 and above, so the last version supporting it, 0.3.0 (and lower), will now make the build default to Scala Native 0.4.17.

```bash
scala-cli -e 'println(os.pwd)' --toolkit 0.3.0 --native                          
# [warn] Scala Toolkit Version(0.3.0) does not support Scala Native 0.5.3, 0.4.17 should be used instead.
# [warn] Scala Native default version 0.5.3 is not supported in this build. Using 0.4.17 instead.
# Compiling project (Scala 3.4.2, Scala Native 0.4.17)
# Compiled project (Scala 3.4.2, Scala Native 0.4.17)
# [info] Linking (900 ms)
# [info] Checking intermediate code (quick) (63 ms)
# [info] Discovered 888 classes and 5298 methods
# [info] Optimizing (debug mode) (836 ms)
# [info] Generating intermediate code (620 ms)
# [info] Produced 10 files
# [info] Compiling to native code (1860 ms)
# [info] Linking with [pthread, dl]
# [info] Total (4406 ms)
# ~/scala-cli-tests
```
:::caution
The troublesome case is when Scala Native 0.4.x is passed explicitly, while the Scala Toolkit is set to the default.
Scala CLI does not currently support downgrading the Scala Toolkit in this case, and fails the build.

```bash fail
scala-cli -e 'println(os.pwd)' --toolkit default --native --native-version 0.4.17
# Downloading 4 dependencies and 2 internal dependencies
# [error]  Error downloading org.scala-lang:toolkit-test_native0.4_3:0.4.0
# [error]   not found: ~/.ivy2/local/org.scala-lang/toolkit-test_native0.4_3/0.4.0/ivys/ivy.xml
# [error]   not found: https://repo1.maven.org/maven2/org/scala-lang/toolkit-test_native0.4_3/0.4.0/toolkit-test_native0.4_3-0.4.0.pom
# [error]   not found: ~/Library/Caches/ScalaCli/local-repo/1.4.0/org.scala-lang/toolkit-test_native0.4_3/0.4.0/ivys/ivy.xml
# [error]   No fallback URL found
# [error] COMMAND_LINE
# [error]  Error downloading org.scala-lang:toolkit_native0.4_3:0.4.0
# [error]   not found: ~/.ivy2/local/org.scala-lang/toolkit_native0.4_3/0.4.0/ivys/ivy.xml
# [error]   not found: https://repo1.maven.org/maven2/org/scala-lang/toolkit_native0.4_3/0.4.0/toolkit_native0.4_3-0.4.0.pom
# [error]   not found: ~/Library/Caches/ScalaCli/local-repo/1.4.0/org.scala-lang/toolkit_native0.4_3/0.4.0/ivys/ivy.xml
# [error]   No fallback URL found
# [error] COMMAND_LINE
```
:::

Added by [@Gedochao](https://github.com/Gedochao) in [#2955](https://github.com/VirtusLab/scala-cli/pull/2955)

### Features
* Include test scope in the REPL  when the `--test` flag is passed by [@Gedochao](https://github.com/Gedochao) in [#2971](https://github.com/VirtusLab/scala-cli/pull/2971)

### Fixes
* Fix BSP IllegalArgumentException when loading project in Metals by [@joan38](https://github.com/joan38) in [#2950](https://github.com/VirtusLab/scala-cli/pull/2950)
* Don't check for newer CLI versions when the `--cli-version` launcher param is passed (v1.4.0 and onwards, only) by [@Gedochao](https://github.com/Gedochao) in [#2957](https://github.com/VirtusLab/scala-cli/pull/2957)
* fix: start bloop with jvm version from using directives for JVMs > 17 by [@kasiaMarek](https://github.com/kasiaMarek) in [#2972](https://github.com/VirtusLab/scala-cli/pull/2972)

### Documentation changes
* Typo fixed in scripts.md by [@vaivanov95](https://github.com/vaivanov95) in [#2974](https://github.com/VirtusLab/scala-cli/pull/2974)

### Internal changes
* Tag flaky docker image with scala.js app test by [@Gedochao](https://github.com/Gedochao) in [#2977](https://github.com/VirtusLab/scala-cli/pull/2977)

### Updates
* Update scala-cli.sh launcher for 1.3.2 by [@github-actions](https://github.com/github-actions) in [#2938](https://github.com/VirtusLab/scala-cli/pull/2938)
* Update Scala Native to 0.5.2 by [@scala-steward](https://github.com/scala-steward) in [#2946](https://github.com/VirtusLab/scala-cli/pull/2946)
* Update guava to 33.2.1-jre by [@scala-steward](https://github.com/scala-steward) in [#2947](https://github.com/VirtusLab/scala-cli/pull/2947)
* Update os-lib to 0.10.2 by [@scala-steward](https://github.com/scala-steward) in [#2949](https://github.com/VirtusLab/scala-cli/pull/2949)
* Update ammonite to 3.0.0-M2-8-ba4429a2 by [@scala-steward](https://github.com/scala-steward) in [#2948](https://github.com/VirtusLab/scala-cli/pull/2948)
* Update Scala Native to 0.5.3 by [@scala-steward](https://github.com/scala-steward) in [#2951](https://github.com/VirtusLab/scala-cli/pull/2951)
* Update case-app to 2.1.0-M28 by [@scala-steward](https://github.com/scala-steward) in [#2956](https://github.com/VirtusLab/scala-cli/pull/2956)
* Update Scala Toolkit to 0.4.0 & dynamically adjust Scala Native defaults by [@Gedochao](https://github.com/Gedochao) in [#2955](https://github.com/VirtusLab/scala-cli/pull/2955)
* Update munit to 1.0.0 by [@scala-steward](https://github.com/scala-steward) in [#2935](https://github.com/VirtusLab/scala-cli/pull/2935)
* Update ammonite to 3.0.0-M2-9-88291dd8 by [@scala-steward](https://github.com/scala-steward) in [#2960](https://github.com/VirtusLab/scala-cli/pull/2960)
* Update `scalameta` to 4.9.6 by [@scala-steward](https://github.com/scala-steward) in [#2967](https://github.com/VirtusLab/scala-cli/pull/2967)
* Update ammonite to 3.0.0-M2-10-f6e2c001 by [@scala-steward](https://github.com/scala-steward) in [#2965](https://github.com/VirtusLab/scala-cli/pull/2965)
* Update scalafmt-cli_2.13, scalafmt-core to 3.8.2 by [@scala-steward](https://github.com/scala-steward) in [#2966](https://github.com/VirtusLab/scala-cli/pull/2966)
* Update scalameta to 4.9.7 by [@scala-steward](https://github.com/scala-steward) in [#2983](https://github.com/VirtusLab/scala-cli/pull/2983)
* Pin `scala-cli-setup` to v1 and update CI scripts' dependencies by [@Gedochao](https://github.com/Gedochao) in [#2984](https://github.com/VirtusLab/scala-cli/pull/2984)
* Update Scala Native to 0.5.4 by [@scala-steward](https://github.com/scala-steward) in [#2982](https://github.com/VirtusLab/scala-cli/pull/2982)
* Update mill-main to 0.11.8 by [@scala-steward](https://github.com/scala-steward) in [#2980](https://github.com/VirtusLab/scala-cli/pull/2980)
* Update bloop-config_2.13 to 2.0.2 by [@scala-steward](https://github.com/scala-steward) in [#2978](https://github.com/VirtusLab/scala-cli/pull/2978)
* Update ammonite to 3.0.0-M2-12-951bbc1e by [@scala-steward](https://github.com/scala-steward) in [#2979](https://github.com/VirtusLab/scala-cli/pull/2979)

## New Contributors
* [@vaivanov95](https://github.com/vaivanov95) made their first contribution in [#2974](https://github.com/VirtusLab/scala-cli/pull/2974)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.3.2...v1.4.0

## [v1.3.2](https://github.com/VirtusLab/scala-cli/releases/tag/v1.3.2)

### Support for Scala 3.4.2
This Scala CLI version adds support for Scala 3.4.2.

```bash
scala-cli version
# Scala CLI version: 1.3.2
# Scala version (default): 3.4.2
```

Added by [@Gedochao](https://github.com/Gedochao) in [#2911](https://github.com/VirtusLab/scala-cli/pull/2911).

### Incremental Scala.js linking

Scala CLI now can take advantage of Scala.js' powerful incremental linker, which makes linking very fast for multiple links in a row.
For Scala.js builds, the `scala-js-cli` process is now run with the newly added `--longRunning` mode.
The process is then reused if the inputs did not change.

Added by [@lolgab](https://github.com/lolgab) in [#2928](https://github.com/VirtusLab/scala-cli/pull/2928) and [VirtusLab/scala-js-cli#64](https://github.com/VirtusLab/scala-js-cli/pull/64).

### Features
* Support ARM64 architecture to the launcher script for Mac OS by [@carlosedp](https://github.com/carlosedp) in [#2895](https://github.com/VirtusLab/scala-cli/pull/2895)
* Incremental Scala.js Linking by [@lolgab](https://github.com/lolgab) in [#2928](https://github.com/VirtusLab/scala-cli/pull/2928)

### Fixes
* Fix support of multiline shebang by [@sierikov](https://github.com/sierikov) in [#2908](https://github.com/VirtusLab/scala-cli/pull/2908)
* Pass scala2-sbt-bridge to Bloop explicitly for Scala 2.13.12+ by [@Gedochao](https://github.com/Gedochao) in [#2927](https://github.com/VirtusLab/scala-cli/pull/2927)
* Ensure `JAVA_HOME` of `setup-ide` is respected by `bsp` by [@Gedochao](https://github.com/Gedochao) in [#2920](https://github.com/VirtusLab/scala-cli/pull/2920)
* Improve launcher options handling by [@Gedochao](https://github.com/Gedochao) in [#2931](https://github.com/VirtusLab/scala-cli/pull/2931)

### Documentation changes
* Add docs for `ignore` keyword in snippets in md by [@sierikov](https://github.com/sierikov) in [#2898](https://github.com/VirtusLab/scala-cli/pull/2898)
* Back port of documentation changes to main by [@github-actions](https://github.com/github-actions) in [#2900](https://github.com/VirtusLab/scala-cli/pull/2900)
* Back port of documentation changes to main by [@github-actions](https://github.com/github-actions) in [#2910](https://github.com/VirtusLab/scala-cli/pull/2910)
* Add Scalafmt Cookbook by [@sierikov](https://github.com/sierikov) in [#2903](https://github.com/VirtusLab/scala-cli/pull/2903)
* Back port of documentation changes to main by [@github-actions](https://github.com/github-actions) in [#2914](https://github.com/VirtusLab/scala-cli/pull/2914)
* remove duplicated word by [@naferx](https://github.com/naferx) in [#2915](https://github.com/VirtusLab/scala-cli/pull/2915)
* Remove unused imports by [@naferx](https://github.com/naferx) in [#2916](https://github.com/VirtusLab/scala-cli/pull/2916)
* corrected instructions for downloading the launcher in Windows (fixes #2921) by [@philwalk](https://github.com/philwalk) in [#2922](https://github.com/VirtusLab/scala-cli/pull/2922)

### Internal changes
* Fix instant-startup-scala-scripts.md overeager `docs-tests` by [@Gedochao](https://github.com/Gedochao) in [#2909](https://github.com/VirtusLab/scala-cli/pull/2909)

### Updates
* Update scala-cli.sh launcher for 1.3.1 by [@github-actions](https://github.com/github-actions) in [#2894](https://github.com/VirtusLab/scala-cli/pull/2894)
* Update ammonite to 3.0.0-M1-24-26133e66 by [@scala-steward](https://github.com/scala-steward) in [#2896](https://github.com/VirtusLab/scala-cli/pull/2896)
* Update ammonite to 3.0.0-M2-1-3763a1d4 by [@scala-steward](https://github.com/scala-steward) in [#2905](https://github.com/VirtusLab/scala-cli/pull/2905)
* Update scalameta to 4.9.4 by [@scala-steward](https://github.com/scala-steward) in [#2906](https://github.com/VirtusLab/scala-cli/pull/2906)
* Update Scala Next to 3.4.2 by [@Gedochao](https://github.com/Gedochao) in [#2911](https://github.com/VirtusLab/scala-cli/pull/2911)
* Update ammonite to 3.0.0-M2-2-741e5dbb by [@scala-steward](https://github.com/scala-steward) in [#2913](https://github.com/VirtusLab/scala-cli/pull/2913)
* Update os-lib to 0.10.1 by [@scala-steward](https://github.com/scala-steward) in [#2918](https://github.com/VirtusLab/scala-cli/pull/2918)
* Update `scalameta` to 4.9.5 by [@scala-steward](https://github.com/scala-steward) in [#2919](https://github.com/VirtusLab/scala-cli/pull/2919)
* Update ammonite to 3.0.0-M2-3-b5eb4787 by [@scala-steward](https://github.com/scala-steward) in [#2917](https://github.com/VirtusLab/scala-cli/pull/2917)
* Update Scala Next RC to 3.5.0-RC1 by [@Gedochao](https://github.com/Gedochao) in [#2912](https://github.com/VirtusLab/scala-cli/pull/2912)
* Update bloop-rifle_2.13 to 1.5.17-sc-2 by [@scala-steward](https://github.com/scala-steward) in [#2925](https://github.com/VirtusLab/scala-cli/pull/2925)
* Update sttp core_2.13 to 3.9.7 by [@scala-steward](https://github.com/scala-steward) in [#2924](https://github.com/VirtusLab/scala-cli/pull/2924)
* Update ammonite to 3.0.0-M2-4-c3312916 by [@scala-steward](https://github.com/scala-steward) in [#2923](https://github.com/VirtusLab/scala-cli/pull/2923)
* Bump `scala-js-cli` to v1.16.0.1 by [@Gedochao](https://github.com/Gedochao) in [#2929](https://github.com/VirtusLab/scala-cli/pull/2929)

## New Contributors
* [@sierikov](https://github.com/sierikov) made their first contribution in [#2898](https://github.com/VirtusLab/scala-cli/pull/2898)
* [@naferx](https://github.com/naferx) made their first contribution in [#2915](https://github.com/VirtusLab/scala-cli/pull/2915)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.3.1...v1.3.2

## [v1.3.1](https://github.com/VirtusLab/scala-cli/releases/tag/v1.3.1)

### Scala 2.13.14 Support
This Scala CLI version adds support for Scala 2.13.14.

```bash
scala-cli -e 'println(scala.util.Properties.versionNumberString)' -S 2.13
# Compiling project (Scala 2.13.14, JVM (17))
# Compiled project (Scala 2.13.14, JVM (17))
# 2.13.14
```

Added by [@Gedochao](https://github.com/Gedochao) in [#2882](https://github.com/VirtusLab/scala-cli/pull/2882).

### Fixes
* Adjust TASTY bump warnings to respect overridden Scala version defaults by [@Gedochao](https://github.com/Gedochao) in [#2888](https://github.com/VirtusLab/scala-cli/pull/2888)
* Include `scala3-staging` and `scala3-tasty-inspector` artifacts when the `--with-compiler` option is passed in Scala 3 by [@Gedochao](https://github.com/Gedochao) in [#2889](https://github.com/VirtusLab/scala-cli/pull/2889)

### Internal changes
* Allow to override prog name with a launcher arg by [@Gedochao](https://github.com/Gedochao) in [#2891](https://github.com/VirtusLab/scala-cli/pull/2891)

### Updates
* Update scala-cli.sh launcher for 1.3.0 by [@github-actions](https://github.com/github-actions) in [#2876](https://github.com/VirtusLab/scala-cli/pull/2876)
* Update Scala 2 to 2.13.14 by [@Gedochao](https://github.com/Gedochao) in [#2882](https://github.com/VirtusLab/scala-cli/pull/2882)
* Update guava to 33.2.0-jre by [@scala-steward](https://github.com/scala-steward) in [#2883](https://github.com/VirtusLab/scala-cli/pull/2883)
* Update `com.softwaremill.sttp.client3:core` to 3.9.6 by [@scala-steward](https://github.com/scala-steward) in [#2885](https://github.com/VirtusLab/scala-cli/pull/2885)
* Update sbt to 1.10.0 by [@scala-steward](https://github.com/scala-steward) in [#2887](https://github.com/VirtusLab/scala-cli/pull/2887)
* Update ammonite to 3.0.0-M1-19-a7973e17 by [@scala-steward](https://github.com/scala-steward) in [#2884](https://github.com/VirtusLab/scala-cli/pull/2884)
* Bump `coursier` to 2.1.10 by [@Gedochao](https://github.com/Gedochao) in [#2890](https://github.com/VirtusLab/scala-cli/pull/2890)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.3.0...v1.3.1

## [v1.3.0](https://github.com/VirtusLab/scala-cli/releases/tag/v1.3.0)

### Support for Scala Native 0.5.1
This Scala CLI version adds support for Scala Native 0.5.1.
All native platform builds will now use 0.5.1 as the default version.

```bash
scala-cli -e 'println("Hello, Scala Native!")' --native
# Compiling project (Scala 3.4.1, Scala Native 0.5.1)
# Compiled project (Scala 3.4.1, Scala Native 0.5.1)
# [info] Linking (multithreadingEnabled=true, disable if not used) (1059 ms)
# [info] Discovered 882 classes and 5388 methods after classloading
# [info] Checking intermediate code (quick) (39 ms)
# [info] Multithreading was not explicitly enabled - initial class loading has not detected any usage of system threads. Multithreading support will be disabled to improve performance.
# [info] Linking (multithreadingEnabled=false) (291 ms)
# [info] Discovered 499 classes and 2501 methods after classloading
# [info] Checking intermediate code (quick) (6 ms)
# [info] Discovered 478 classes and 1916 methods after optimization
# [info] Optimizing (debug mode) (432 ms)
# [info] Produced 9 LLVM IR files
# [info] Generating intermediate code (293 ms)
# [info] Compiling to native code (1504 ms)
# [info] Linking with [pthread, dl]
# [info] Linking native code (immix gc, none lto) (351 ms)
# [info] Postprocessing (0 ms)
# [info] Total (4012 ms)
# Hello, Scala Native!
```

Note that not all the tools Scala CLI integrates with support Scala Native 0.5.x just yet. 
When such an integration is being used, the default Scala Native version will get downgraded to 0.4.17.

```bash
scala-cli -e 'println("Hello, Scala Native!")' --native --toolkit default
# [warn] Scala Toolkit does not support Scala Native 0.5.1, 0.4.17 should be used instead.
# [warn] Scala Native default version 0.5.1 is not supported in this build. Using 0.4.17 instead.
# Compiling project (Scala 3.4.1, Scala Native 0.4.17)
# Compiled project (Scala 3.4.1, Scala Native 0.4.17)
# [info] Linking (1017 ms)
# [info] Checking intermediate code (quick) (53 ms)
# [info] Discovered 743 classes and 4242 methods
# [info] Optimizing (debug mode) (654 ms)
# [info] Generating intermediate code (898 ms)
# [info] Produced 10 files
# [info] Compiling to native code (2039 ms)
# [info] Linking with [pthread, dl]
# [info] Total (4812 ms)
# Hello, Scala Native!
```

Efforts for supporting Scala Native 0.5.x are ongoing, we expect the downgrade to 0.4.17 in such cases to be a temporary solution.
If you know for a fact that 0.5.x support has been delivered for a tool, you can always pass the `--native-version` option explicitly, which will prevent the downgrade.

Added by [@Gedochao](https://github.com/Gedochao) in [#2862](https://github.com/VirtusLab/scala-cli/pull/2862)

### Fixes
* Add missing Scala 2 compiler print options by [@Gedochao](https://github.com/Gedochao) in [#2848](https://github.com/VirtusLab/scala-cli/pull/2848)
* Don't recommend `latest` for toolkit version by [@keynmol](https://github.com/keynmol) in [#2852](https://github.com/VirtusLab/scala-cli/pull/2852)
* Explicitly pass `scala3-sbt-bridge` to Bloop by [@Gedochao](https://github.com/Gedochao) in [#2873](https://github.com/VirtusLab/scala-cli/pull/2873)

### Internal changes
* Add launcher options allowing to override the default Scala version by [@Gedochao](https://github.com/Gedochao) in [#2860](https://github.com/VirtusLab/scala-cli/pull/2860)

### Updates and maintenance
* Update scala-cli.sh launcher for 1.2.2 by [@github-actions](https://github.com/features/actions)  in [#2847](https://github.com/VirtusLab/scala-cli/pull/2847)
* Update os-lib to 0.10.0 by [@scala-steward](https://github.com/scala-steward) in [#2854](https://github.com/VirtusLab/scala-cli/pull/2854)
* Update scala-collection-compat to 2.12.0 by [@scala-steward](https://github.com/scala-steward) in [#2856](https://github.com/VirtusLab/scala-cli/pull/2856)
* Update slf4j-nop to 2.0.13 by [@scala-steward](https://github.com/scala-steward) in [#2857](https://github.com/VirtusLab/scala-cli/pull/2857)
* Update pprint to 0.9.0 by [@scala-steward](https://github.com/scala-steward) in [#2855](https://github.com/VirtusLab/scala-cli/pull/2855)
* Update fansi to 0.5.0 by [@scala-steward](https://github.com/scala-steward) in [#2853](https://github.com/VirtusLab/scala-cli/pull/2853)
* Update using_directives to 1.1.1 by [@scala-steward](https://github.com/scala-steward) in [#2863](https://github.com/VirtusLab/scala-cli/pull/2863)
* Update Scala Native to 0.5.1 by [@scala-steward](https://github.com/scala-steward) and [@Gedochao](https://github.com/Gedochao) in [#2862](https://github.com/VirtusLab/scala-cli/pull/2862)
* Update `bloop-core` to 1.5.17-sc-1 by [@scala-steward](https://github.com/scala-steward) in [#2873](https://github.com/VirtusLab/scala-cli/pull/2873)
* Update `bloop-config` to 2.0.0 by [@scala-steward](https://github.com/scala-steward) in [#2873](https://github.com/VirtusLab/scala-cli/pull/2873)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.2.2...v1.3.0

## [v1.2.2](https://github.com/VirtusLab/scala-cli/releases/tag/v1.2.2)

### Fixed the `Fatal invariant violated` false-positive error coming from Bloop
This small update fixes the `Fatal invariant violated` error ([#2829](https://github.com/VirtusLab/scala-cli/issues/2829)). 
The error was being thrown by Bloop when running Scala CLI repeatedly with the same sources.

Fixed by [@Gedochao](https://github.com/Gedochao) in [#2837](https://github.com/VirtusLab/scala-cli/pull/2837)

### Enhancements
* Log a warning when invalid java properties are being passed by env vars by [@Gedochao](https://github.com/Gedochao) in [#2843](https://github.com/VirtusLab/scala-cli/pull/2843)

### Updates and maintenance
* Update scala-cli.sh launcher for 1.2.1 by [@github-actions](https://github.com/github-actions) in [#2828](https://github.com/VirtusLab/scala-cli/pull/2828)
* Update `org.scalameta:trees_2.13` to 4.9.3 by [@scala-steward](https://github.com/scala-steward) in [#2831](https://github.com/VirtusLab/scala-cli/pull/2831)
* Update ammonite to 3.0.0-M1-10-105f9e32 by [@scala-steward](https://github.com/scala-steward) in [#2844](https://github.com/VirtusLab/scala-cli/pull/2844)
* Bump `bloop-core` to 1.5.16-sc-2 by [@Gedochao](https://github.com/Gedochao) in [#2837](https://github.com/VirtusLab/scala-cli/pull/2837)

## What's Changed

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.2.1...v1.2.2

## [v1.2.1](https://github.com/VirtusLab/scala-cli/releases/tag/v1.2.1)

### Support for Scala 3.4.1
This Scala CLI version adds support for Scala 3.4.1.

```bash
scala-cli version
# Scala CLI version: 1.2.1
# Scala version (default): 3.4.1
```

Additionally, from this version on Scala CLi is being tested against the latest Scala 3 Next RC.
And so, feel free to try out Scala 3.4.2-RC1!

```bash
scala-cli run -S 3.4.2-RC1 --with-compiler -e 'println(dotty.tools.dotc.config.Properties.simpleVersionString)'
# Compiling project (Scala 3.4.2-RC1, JVM (17))
# Compiled project (Scala 3.4.2-RC1, JVM (17))
# 3.4.2-RC1
```

Added by [@Gedochao](https://github.com/Gedochao) in [#2824](https://github.com/VirtusLab/scala-cli/pull/2824) & [#2822](https://github.com/VirtusLab/scala-cli/pull/2822)

### Support for Scala.js 1.16.0

This version adds Scala CLI support for Scala.js 1.16.0.
Added by [@scala-steward](https://github.com/scala-steward) in [#2807](https://github.com/VirtusLab/scala-cli/pull/2807) & [@Gedochao](https://github.com/Gedochao) in [scala-js-cli#55](https://github.com/VirtusLab/scala-js-cli/pull/55).

### Fixes
* Fix handling for `-Xlint:help` by [@Gedochao](https://github.com/Gedochao) in [#2781](https://github.com/VirtusLab/scala-cli/pull/2781)
* Fix `--semanticdb-targetroot` & `--semanticdb-sourceroot` for scripts by [@Gedochao](https://github.com/Gedochao) in [#2784](https://github.com/VirtusLab/scala-cli/pull/2784)
* Adjust actionable diagnostics for scripts by [@rochala](https://github.com/rochala) in [#2815](https://github.com/VirtusLab/scala-cli/pull/2815)
* Fix publishing of runner & test-runner artifacts by [@Gedochao](https://github.com/Gedochao) in [#2819](https://github.com/VirtusLab/scala-cli/pull/2819)
* bugfix: Fix Bloop import by [@tgodzik](https://github.com/tgodzik) in [#2825](https://github.com/VirtusLab/scala-cli/pull/2825)

### Enhancements
* Ensure external help options are mentioned in short help where available by [@Gedochao](https://github.com/Gedochao) in [#2808](https://github.com/VirtusLab/scala-cli/pull/2808)

### Internal changes
* Run integration tests for the latest Scala 3 Next RC by [@Gedochao](https://github.com/Gedochao) in [#2824](https://github.com/VirtusLab/scala-cli/pull/2824)

### Documentation changes
* Add installation guide for FreeBSD by [@spacebanana420](https://github.com/spacebanana420) in [#2793](https://github.com/VirtusLab/scala-cli/pull/2793)
* Back port of documentation changes to main by [@github-actions](https://github.com/features/actions) in [#2797](https://github.com/VirtusLab/scala-cli/pull/2797)

### Updates and maintenance
* Update scala-cli.sh launcher for 1.2.0 by [@github-actions](https://github.com/features/actions) in [#2783](https://github.com/VirtusLab/scala-cli/pull/2783)
* Update core_2.13 to 3.9.4 by [@scala-steward](https://github.com/scala-steward) in [#2787](https://github.com/VirtusLab/scala-cli/pull/2787)
* Update ammonite to 3.0.0-M1-8-35694880 by [@scala-steward](https://github.com/scala-steward) in [#2786](https://github.com/VirtusLab/scala-cli/pull/2786)
* Update trees_2.13 to 4.9.2 by [@scala-steward](https://github.com/scala-steward) in [#2795](https://github.com/VirtusLab/scala-cli/pull/2795)
* Update guava to 33.1.0-jre by [@scala-steward](https://github.com/scala-steward) in [#2801](https://github.com/VirtusLab/scala-cli/pull/2801)
* Bump follow-redirects from 1.15.4 to 1.15.6 in /website by [@dependabot](https://github.com/dependabot) in [#2803](https://github.com/VirtusLab/scala-cli/pull/2803)
* Add -unchecked to the list of options that don't require -O by [@joan38](https://github.com/joan38) in [#2800](https://github.com/VirtusLab/scala-cli/pull/2800)
* Update bloop-rifle_2.13 to 1.5.12-sc-1 by [@scala-steward](https://github.com/scala-steward) in [#2806](https://github.com/VirtusLab/scala-cli/pull/2806)
* Update sttp.client core to 3.9.5 by [@scala-steward](https://github.com/scala-steward) in [#2810](https://github.com/VirtusLab/scala-cli/pull/2810)
* Update asm to 9.7 by [@scala-steward](https://github.com/scala-steward) in [#2813](https://github.com/VirtusLab/scala-cli/pull/2813)
* Update Scala.js to 1.16.0 by [@scala-steward](https://github.com/scala-steward) in [#2807](https://github.com/VirtusLab/scala-cli/pull/2807)
* Bump express from 4.18.2 to 4.19.2 in /website by [@dependabot](https://github.com/dependabot) in [#2816](https://github.com/VirtusLab/scala-cli/pull/2816)
* Update Bloop to 1.5.16-sc-1 by [@Gedochao](https://github.com/Gedochao) in [#2818](https://github.com/VirtusLab/scala-cli/pull/2818)
* Bump Scala Next to 3.4.1 by [@Gedochao](https://github.com/Gedochao) in [#2822](https://github.com/VirtusLab/scala-cli/pull/2822)
* Bump Typelevel Toolkit to 0.1.23 by [@Gedochao](https://github.com/Gedochao) in [#2823](https://github.com/VirtusLab/scala-cli/pull/2823)

### New Contributors
* [@joan38](https://github.com/joan38) made their first contribution in [#2800](https://github.com/VirtusLab/scala-cli/pull/2800)
* [@rochala](https://github.com/rochala) made their first contribution in [#2815](https://github.com/VirtusLab/scala-cli/pull/2815)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.2.0...v1.2.1

## [v1.2.0](https://github.com/VirtusLab/scala-cli/releases/tag/v1.2.0)

### Scala 3.3.3, 3.4.0, 2.13.13 & 2.12.19 support

This version of Scala CLI adds support for a whooping 4 new Scala versions, it's been busy these past few days!
The default version used when using the CLI will from now on be the Scala 3 Next version (3.4.0 as of this release).
Using the `lts` tag will now point to Scala 3.3.3.
The LTS is also the version used for building the internals of Scala CLI (although we now also cross-compile with 3.4.0).
```bash
scala-cli version
# Scala CLI version: 1.2.0
# Scala version (default): 3.4.0
```
Added by [@Gedochao](https://github.com/Gedochao) in [#2772](https://github.com/VirtusLab/scala-cli/pull/2772), [#2736](https://github.com/VirtusLab/scala-cli/pull/2736), [#2755](https://github.com/VirtusLab/scala-cli/pull/2755), [#2753](https://github.com/VirtusLab/scala-cli/pull/2753) and [#2752](https://github.com/VirtusLab/scala-cli/pull/2752)

### Remapping EsModule imports at link time with Scala.js
Given the following `importMap.json` file:
```json title=importMap.json
{
  "imports": {
    "@stdlib/linspace": "https://cdn.skypack.dev/@stdlib/linspace"
  }
}
```

It is now possible to remap the imports at link time with the `jsEsModuleImportMap` directive.

```scala title=RemappingEsModuleImports.scala
//> using jsEsModuleImportMap importMap.json
//> using jsModuleKind es
//> using jsMode fastLinkJS
//> using platform js

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.typedarray.Float64Array

object Foo {
  def main(args: Array[String]): Unit = {
    println(Array(-10.0, 10.0, 10).mkString(", "))
    println(linspace(0, 10, 10).mkString(", "))
  }
}

@js.native
@JSImport("@stdlib/linspace", JSImport.Default)
object linspace extends js.Object {
  def apply(start: Double, stop: Double, num: Int): Float64Array = js.native
}
```

The same can be achieved with the `--js-es-module-import-map` command line option.
```bash
scala-cli --power package RemappingEsModuleImports.scala --js --js-module-kind ESModule -o main.js --js-es-module-import-map importMap.json
```

Added by [@Quafadas](https://github.com/Quafadas) in [#2737](https://github.com/VirtusLab/scala-cli/pull/2737) and [scala-js-cli#47](https://github.com/VirtusLab/scala-js-cli/pull/47)

### Fixes
* Updated method for choosing a free drive letter (fixes #2743) by [@philwalk](https://github.com/philwalk) in [#2749](https://github.com/VirtusLab/scala-cli/pull/2749)
* Make sure tasty-lib doesn't warn about Scala 3 Next by [@Gedochao](https://github.com/Gedochao) in [#2775](https://github.com/VirtusLab/scala-cli/pull/2775)

### Enhancements
* Add the ability to remap EsModule imports at link time by [@Quafadas](https://github.com/Quafadas) in [#2737](https://github.com/VirtusLab/scala-cli/pull/2737)

### Internal changes
* Fix overeager Scala version docs tests by [@Gedochao](https://github.com/Gedochao) in [#2750](https://github.com/VirtusLab/scala-cli/pull/2750)
* Lock script wrapper tests on the internally used Scala 2.13 version by [@Gedochao](https://github.com/Gedochao) in [#2754](https://github.com/VirtusLab/scala-cli/pull/2754)
* Use Scala LTS as the default version while cross compiling all Scala 3 modules on both LTS & Next by [@Gedochao](https://github.com/Gedochao) in [#2752](https://github.com/VirtusLab/scala-cli/pull/2752)
* Explicitly set sonatype publishing to use the default cross Scala version by [@Gedochao](https://github.com/Gedochao) in [#2757](https://github.com/VirtusLab/scala-cli/pull/2757)
* Fix publishing of artifacts to include non-cross-compiled modules by [@Gedochao](https://github.com/Gedochao) in [#2759](https://github.com/VirtusLab/scala-cli/pull/2759)
* Run integration tests with both Scala 3 LTS & Next versions by [@Gedochao](https://github.com/Gedochao) in [#2760](https://github.com/VirtusLab/scala-cli/pull/2760)

### Documentation changes
* Fix typo by [@imRentable](https://github.com/imRentable) in [#2739](https://github.com/VirtusLab/scala-cli/pull/2739)
* Add directive examples in Scala Native docs by [@spamegg1](https://github.com/spamegg1) in [#2774](https://github.com/VirtusLab/scala-cli/pull/2774)
* toolkit latest is deprecated, mention default instead by [@spamegg1](https://github.com/spamegg1) in [#2776](https://github.com/VirtusLab/scala-cli/pull/2776)

### Updates and maintenance
* Update scala-cli.sh launcher for 1.1.3 by [@github-actions](https://github.com/features/actions) in [#2734](https://github.com/VirtusLab/scala-cli/pull/2734)
* Bump webfactory/ssh-agent from 0.8.0 to 0.9.0 by [@dependabot](https://github.com/dependabot) in [#2731](https://github.com/VirtusLab/scala-cli/pull/2731)
* Update `coursier` to 2.1.9 by [@Gedochao](https://github.com/Gedochao) in [#2735](https://github.com/VirtusLab/scala-cli/pull/2735)
* Bump `scala-js-cli` to 1.15.0.1 by [@Gedochao](https://github.com/Gedochao) in [#2738](https://github.com/VirtusLab/scala-cli/pull/2738)
* Update Scala to 3.4.0 by [@Gedochao](https://github.com/Gedochao) in [#2736](https://github.com/VirtusLab/scala-cli/pull/2736)
* Update slf4j-nop to 2.0.12 by [@scala-steward](https://github.com/scala-steward) in [#2748](https://github.com/VirtusLab/scala-cli/pull/2748)
* Update trees_2.13 to 4.9.0 by [@scala-steward](https://github.com/scala-steward) in [#2747](https://github.com/VirtusLab/scala-cli/pull/2747)
* Update mill-main to 0.11.7 by [@scala-steward](https://github.com/scala-steward) in [#2744](https://github.com/VirtusLab/scala-cli/pull/2744)
* Update sttp client core_2.13 to 3.9.3 by [@scala-steward](https://github.com/scala-steward) in [#2745](https://github.com/VirtusLab/scala-cli/pull/2745)
* Bump Scala 2.12 to 2.12.19 by [@Gedochao](https://github.com/Gedochao) in [#2753](https://github.com/VirtusLab/scala-cli/pull/2753)
* Update sbt to 1.9.9 by [@scala-steward](https://github.com/scala-steward) in [#2756](https://github.com/VirtusLab/scala-cli/pull/2756)
* Bump Scala 2.13 to 2.13.13 by [@Gedochao](https://github.com/Gedochao) in [#2755](https://github.com/VirtusLab/scala-cli/pull/2755)
* Update scalameta to 4.9.1 by [@scala-steward](https://github.com/scala-steward) in [#2770](https://github.com/VirtusLab/scala-cli/pull/2770)
* Bump Scala LTS to 3.3.3 by [@Gedochao](https://github.com/Gedochao) in [#2772](https://github.com/VirtusLab/scala-cli/pull/2772)
* Update ammonite to 3.0.0-M0-71-1e75159e by [@scala-steward](https://github.com/scala-steward) in [#2773](https://github.com/VirtusLab/scala-cli/pull/2773)

### New Contributors
* [@imRentable](https://github.com/imRentable) made their first contribution in [#2739](https://github.com/VirtusLab/scala-cli/pull/2739)
* [@spamegg1](https://github.com/spamegg1) made their first contribution in [#2774](https://github.com/VirtusLab/scala-cli/pull/2774)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.1.3...v1.2.0

## [v1.1.3](https://github.com/VirtusLab/scala-cli/releases/tag/v1.1.3)

### Support for LTS Scala version aliases
It is now possible to use `lts` and `3.lts` as Scala version aliases in Scala CLI. 
They refer to the latest LTS version of Scala (the `3.3.x` line at the time of this release).

```bash
scala-cli run -S lts --with-compiler -e 'println(dotty.tools.dotc.config.Properties.simpleVersionString)'
# Compiling project (Scala 3.3.1, JVM (17))
# Compiled project (Scala 3.3.1, JVM (17))
# 3.3.1
```

Using the `2.lts`, `2.13.lts` & `2.12.lts` aliases returns a meaningful error, too.

```bash fail
scala-cli run -S 2.lts -e 'println(scala.util.Properties.versionString)'                                 
# [error]  Invalid Scala version: 2.lts. There is no official LTS version for Scala 2.
# You can only choose one of the 3.x, 2.13.x, and 2.12.x. versions.
# The latest supported stable versions are 2.12.18, 2.13.12, 3.3.1.
# In addition, you can request compilation with the last nightly versions of Scala,
# by passing the 2.nightly, 2.12.nightly, 2.13.nightly, or 3.nightly arguments.
# Specific Scala 2 or Scala 3 nightly versions are also accepted.
# You can also request the latest Scala 3 LTS by passing lts or 3.lts.
```

Added by [@kasiaMarek](https://github.com/kasiaMarek) in [#2710](https://github.com/VirtusLab/scala-cli/pull/2710)

### `--semanticdb-targetroot` and `--semanticdb-sourceroot` options
It is now possible to set the SemanticDB target root and source root directories with unified syntax,
independent of the target Scala and/or Java versions.

For a given `semanticdb-example.sc` script:

```scala title=src/semanticdb-example.sc
println("SemanticDB targetroot gets set to ./targetRootDir, while sourceroot gets set to the current working directory.")
```

You now can specify the `targetroot` and `sourceroot` directories like this:

```bash 
scala-cli compile src/semanticdb-example.sc --semanticdb-targetroot ./targetRootDir --semanticdb-sourceroot .
```

Added by [@Gedochao](https://github.com/Gedochao) in [#2692](https://github.com/VirtusLab/scala-cli/pull/2692)

### Fixes
* remove `user.home` hack by [@kasiaMarek](https://github.com/kasiaMarek) in [#2710](https://github.com/VirtusLab/scala-cli/pull/2710)
* Fix ultra-long invalid Scala version errors by [@Gedochao](https://github.com/Gedochao) in [#2724](https://github.com/VirtusLab/scala-cli/pull/2724)

### Documentation changes
* Add information about `--preamble` in assembly packaging documentation by [@spacebanana420](https://github.com/spacebanana420) in [#2713](https://github.com/VirtusLab/scala-cli/pull/2713)
* Back port of documentation changes to main by [@github-actions](https://github.com/features/actions) in [#2717](https://github.com/VirtusLab/scala-cli/pull/2717)
* Documentation for creation of custom toolkit by [@yadavan88](https://github.com/yadavan88) in [#2715](https://github.com/VirtusLab/scala-cli/pull/2715)
* Back port of documentation changes to main by [@github-actions](https://github.com/features/actions) in [#2718](https://github.com/VirtusLab/scala-cli/pull/2718)
* Fix formatting in custom toolkit doc by [@yadavan88](https://github.com/yadavan88) in [#2719](https://github.com/VirtusLab/scala-cli/pull/2719)
* Back port of documentation changes to main by [@github-actions](https://github.com/features/actions) in [#2720](https://github.com/VirtusLab/scala-cli/pull/2720)
* Added info about repl with toolkit by [@yadavan88](https://github.com/yadavan88) in [#2721](https://github.com/VirtusLab/scala-cli/pull/2721)
* Back port of documentation changes to main by [@github-actions](https://github.com/features/actions) in [#2723](https://github.com/VirtusLab/scala-cli/pull/2723)

### Updates and maintenance
* Update scala-cli.sh launcher for 1.1.2 by [@github-actions](https://github.com/features/actions) in [#2688](https://github.com/VirtusLab/scala-cli/pull/2688)
* Update bsp4j to 2.1.1 by [@scala-steward](https://github.com/scala-steward) in [#2700](https://github.com/VirtusLab/scala-cli/pull/2700)
* Update Scala Native to 0.4.17 by [@scala-steward](https://github.com/scala-steward) in [#2696](https://github.com/VirtusLab/scala-cli/pull/2696)
* Bump coursier/setup-action from 1.3.4 to 1.3.5 by [@dependabot](https://github.com/dependabot) in [#2716](https://github.com/VirtusLab/scala-cli/pull/2716)

### New Contributors
* [@kasiaMarek](https://github.com/kasiaMarek) made their first contribution in [#2710](https://github.com/VirtusLab/scala-cli/pull/2710)
* [@spacebanana420](https://github.com/spacebanana420) made their first contribution in [#2713](https://github.com/VirtusLab/scala-cli/pull/2713)
* [@yadavan88](https://github.com/yadavan88) made their first contribution in [#2715](https://github.com/VirtusLab/scala-cli/pull/2715)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.1.2...v1.1.3

## [v1.1.2](https://github.com/VirtusLab/scala-cli/releases/tag/v1.1.2)

### Support for Scala.js 1.15.0

This version adds Scala CLI support for Scala.js 1.15.0.
Added by [@scala-steward](https://github.com/scala-steward) in [#2672](https://github.com/VirtusLab/scala-cli/pull/2672) & [@Gedochao](https://github.com/Gedochao) in [scala-js-cli#43](https://github.com/VirtusLab/scala-js-cli/pull/43).

### Fixes
* Fix repeatable compiler options handling from the command line by [@Gedochao](https://github.com/Gedochao) in [#2666](https://github.com/VirtusLab/scala-cli/pull/2666)
* Fix script wrapper tests & script object wrapper `using` directive by [@Gedochao](https://github.com/Gedochao) in [#2668](https://github.com/VirtusLab/scala-cli/pull/2668)
* Prevent consecutive `-language:*` options from being ignored by [@Gedochao](https://github.com/Gedochao) in [#2667](https://github.com/VirtusLab/scala-cli/pull/2667)

### Documentation changes
* Fix test.md by [@MaciejG604](https://github.com/MaciejG604) in [#2679](https://github.com/VirtusLab/scala-cli/pull/2679)
* Back port of documentation changes to main by [@github-actions](https://github.com/features/actions) in [#2681](https://github.com/VirtusLab/scala-cli/pull/2681)

### Build and internal changes
* Update release procedure steps for `v1.1.x` by [@Gedochao](https://github.com/Gedochao) in [#2665](https://github.com/VirtusLab/scala-cli/pull/2665)
* Tag `GitHubTests.create secret` as flaky on all Mac tests (including M1) by [@Gedochao](https://github.com/Gedochao) in [#2677](https://github.com/VirtusLab/scala-cli/pull/2677)

### Updates and maintenance
* Update scala-cli.sh launcher for 1.1.1 by [@github-actions](https://github.com/features/actions) in [#2662](https://github.com/VirtusLab/scala-cli/pull/2662)
* Bump libsodiumjni to 0.0.4 by [@MaciejG604](https://github.com/MaciejG604) in [#2651](https://github.com/VirtusLab/scala-cli/pull/2651)
* Update guava to 33.0.0-jre by [@scala-steward](https://github.com/scala-steward) in [#2670](https://github.com/VirtusLab/scala-cli/pull/2670)
* Update os-lib to 0.9.3 by [@scala-steward](https://github.com/scala-steward) in [#2671](https://github.com/VirtusLab/scala-cli/pull/2671)
* Update sbt to 1.9.8 by [@scala-steward](https://github.com/scala-steward) in [#2673](https://github.com/VirtusLab/scala-cli/pull/2673)
* Update trees_2.13 to 4.8.15 by [@scala-steward](https://github.com/scala-steward) in [#2674](https://github.com/VirtusLab/scala-cli/pull/2674)
* Update slf4j-nop to 2.0.11 by [@scala-steward](https://github.com/scala-steward) in [#2675](https://github.com/VirtusLab/scala-cli/pull/2675)
* Update Scala.js to 1.15.0 by [@scala-steward](https://github.com/scala-steward) in [#2672](https://github.com/VirtusLab/scala-cli/pull/2672)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.1.1...v1.1.2

## [v1.1.1](https://github.com/VirtusLab/scala-cli/releases/tag/v1.1.1)

### Deprecate Scala Toolkit `latest` version in favour of `default`
Using toolkits with the `latest` version is now deprecated and will cause a warning. 
It will likely be removed completely in a future release.
```bash
scala-cli --toolkit latest -e 'println(os.pwd)'
# Using 'latest' for toolkit is deprecated, use 'default' to get more stable behaviour:
#  --toolkit default
# Compiling project (Scala 3.3.1, JVM (17))
# Compiled project (Scala 3.3.1, JVM (17))
# /home
```

It is now advised to either use an explicit toolkit version or rely on the new `default` alias.
```bash
scala-cli --toolkit default -e 'println(os.pwd)'
# Compiling project (Scala 3.3.1, JVM (17))
# Compiled project (Scala 3.3.1, JVM (17))
# /home
```

The `default` version for toolkits is tied to a particular Scala CLI version.
You can check which version is used by referring to Scala CLI help.
```bash ignore
scala-cli version                 
# Scala CLI version: 1.1.1
# Scala version (default): 3.3.1
scala-cli run -h|grep toolkit         
#   --toolkit, --with-toolkit version|default  Add toolkit to classPath (not supported in Scala 2.12), 'default' version for Scala toolkit: 0.2.1, 'default' version for typelevel toolkit: 0.1.20
```

Added by [@MaciejG604](https://github.com/MaciejG604) in [#2622](https://github.com/VirtusLab/scala-cli/pull/2622)

### Enhancements
* Remove semantics Compliant for asInstaceOf by [@MaciejG604](https://github.com/MaciejG604) in [#2614](https://github.com/VirtusLab/scala-cli/pull/2614)
* Scala js mode validation by [@MaciejG604](https://github.com/MaciejG604) in [#2630](https://github.com/VirtusLab/scala-cli/pull/2630)
* Add missing Scala.js mode aliases by [@Gedochao](https://github.com/Gedochao) in [#2655](https://github.com/VirtusLab/scala-cli/pull/2655)
* Add deprecation reporting mechanism for using directives by [@MaciejG604](https://github.com/MaciejG604) in [#2622](https://github.com/VirtusLab/scala-cli/pull/2622)
* Pass java opts to scalac by [@MaciejG604](https://github.com/MaciejG604) in [#2601](https://github.com/VirtusLab/scala-cli/pull/2601)

### Fixes
* Fallback to UTF-8 in setup-ide by [@JD557](https://github.com/JD557) in [#2599](https://github.com/VirtusLab/scala-cli/pull/2599)
* Separate Scala REPL classpath from user dependencies by [@Gedochao](https://github.com/Gedochao) in [#2607](https://github.com/VirtusLab/scala-cli/pull/2607)
* Prevent resource directories from breaking sources hash by [@Gedochao](https://github.com/Gedochao) in [#2654](https://github.com/VirtusLab/scala-cli/pull/2654)
* Fix special handling for the `-Xplugin-list` compiler option by [@Gedochao](https://github.com/Gedochao) in [#2635](https://github.com/VirtusLab/scala-cli/pull/2635)
* Remove superfluous traits by [@MaciejG604](https://github.com/MaciejG604) in [#2618](https://github.com/VirtusLab/scala-cli/pull/2618)
* Prevent the toolkit latest deprecation warning from being logged more than once by [@Gedochao](https://github.com/Gedochao) in [#2657](https://github.com/VirtusLab/scala-cli/pull/2657)

### Documentation changes
* Unify mentions of Java properties and link to the correct section of guides. by [@MaciejG604](https://github.com/MaciejG604) in [#2603](https://github.com/VirtusLab/scala-cli/pull/2603)
* Document script wrappers by [@MaciejG604](https://github.com/MaciejG604) in [#2596](https://github.com/VirtusLab/scala-cli/pull/2596)
* Shorten titles of cookbooks by [@MaciejG604](https://github.com/MaciejG604) in [#2609](https://github.com/VirtusLab/scala-cli/pull/2609)
* Add docs for bloop interaction by [@MaciejG604](https://github.com/MaciejG604) in [#2608](https://github.com/VirtusLab/scala-cli/pull/2608)
* Docs/java opts for compiler by [@MaciejG604](https://github.com/MaciejG604) in [#2619](https://github.com/VirtusLab/scala-cli/pull/2619)
* Add a subcategories layer for guides & cookbooks by [@Gedochao](https://github.com/Gedochao) in [#2612](https://github.com/VirtusLab/scala-cli/pull/2612)
* Merge documentations about proxy setup by [@MaciejG604](https://github.com/MaciejG604) in [#2597](https://github.com/VirtusLab/scala-cli/pull/2597)
* Update test framework versions by [@mbovel](https://github.com/mbovel) in [#2625](https://github.com/VirtusLab/scala-cli/pull/2625)
* Back port of documentation changes to main by [@github-actions](https://github.com/features/actions) in [#2604](https://github.com/VirtusLab/scala-cli/pull/2604)
* Back port of documentation changes to main by [@github-actions](https://github.com/features/actions) in [#2611](https://github.com/VirtusLab/scala-cli/pull/2611)
* Back port of documentation changes to main by [@github-actions](https://github.com/features/actions) in [#2615](https://github.com/VirtusLab/scala-cli/pull/2615)
* Back port of documentation changes to main by [@github-actions](https://github.com/features/actions) in [#2617](https://github.com/VirtusLab/scala-cli/pull/2617)
* Back port of documentation changes to main by [@github-actions](https://github.com/features/actions) in [#2620](https://github.com/VirtusLab/scala-cli/pull/2620)

### Build and internal changes
* Add debug mode by [@MaciejG604](https://github.com/MaciejG604) in [#2643](https://github.com/VirtusLab/scala-cli/pull/2643)
* Downgrade Xcode on macos CI runners by [@MaciejG604](https://github.com/MaciejG604) in [#2632](https://github.com/VirtusLab/scala-cli/pull/2632)
* Revert xcode version downgrade by [@MaciejG604](https://github.com/MaciejG604) in [#2650](https://github.com/VirtusLab/scala-cli/pull/2650)

### Updates and maintenance
* Update scala-cli.sh launcher for 1.1.0 by [@github-actions](https://github.com/features/actions) in [#2594](https://github.com/VirtusLab/scala-cli/pull/2594)
* Update org.eclipse.jgit to 6.8.0.202311291450-r by [@scala-steward](https://github.com/scala-steward) in [#2613](https://github.com/VirtusLab/scala-cli/pull/2613)
* Bump docusaurus version by [@MaciejG604](https://github.com/MaciejG604) in [#2610](https://github.com/VirtusLab/scala-cli/pull/2610)
* Bump actions/setup-python from 4 to 5 by [@dependabot](https://github.com/dependabot) in [#2624](https://github.com/VirtusLab/scala-cli/pull/2624)

## New Contributors
* [@mbovel](https://github.com/mbovel) made their first contribution in [#2625](https://github.com/VirtusLab/scala-cli/pull/2625)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.1.0...v1.1.1

## [v1.1.0](https://github.com/VirtusLab/scala-cli/releases/tag/v1.1.0)

### Breaking update to Scala 2 scripts

**Keep in mind that it ONLY applies to Scala 2! Scala 3 script wrappers are not affected!**

Scala CLI now uses a different kind of script wrappers for Scala 2 by default, which support running background threads.
This has been introduces as an answer to the [issue #2470](https://github.com/VirtusLab/scala-cli/issues/2470), where a running a script in Scala 2 would end up in a deadlock due to background threads being run.
Also the change makes the Scala 2 scripts run significantly faster, as the code can be optimized due to not residing in the object's initialization clause.

However, the new solution brings some incompatibilities with the old behaviour:
- main classes are now named the same as the file they are defined in, they do not have the '_sc' suffix anymore, so any calls like:
```bash ignore
scala-cli foo.sc bar.sc --main-class foo_sc
```
should be replaced with
```bash ignore
scala-cli foo.sc bar.sc --main-class foo
```
- it is impossible to access the contents of a script named `main.sc` from another source, any references to the script object `main` will result in a compilation error.
E.g. Accessing the contents of `main.sc` using the following code:
```scala
println(main.somethingDefinedInMainScript)
```
Will result in the following compilation error:
```bash ignore
[error] ./foo.sc:2:11
[error] missing argument list for method main in trait App
[error] Unapplied methods are only converted to functions when a function type is expected.
[error] You can make this conversion explicit by writing `main _` or `main(_)` instead of `main`.
```
When `main.sc` is passed as argument together with other scripts, a warning will be displayed:
```bash ignore
[warn]  Script file named 'main.sc' detected, keep in mind that accessing it from other scripts is impossible due to a clash of `main` symbols
```

Added by [@MaciejG604](https://github.com/MaciejG604) in [#2556](https://github.com/VirtusLab/scala-cli/pull/2556)

### "Drive relative" paths on Windows

Scala CLI now correctly recognizes "drive relative" paths on Windows, so paths like `/foo/bar` will be treated as relative from the root of the current drive - e.g. `C:\foo\bar`.
This allows for compatibility of programs referencing paths with e.g. `//> using file /foo/bar` with Windows.

Added by [@philwalk](https://github.com/philwalk) in [#2516](https://github.com/VirtusLab/scala-cli/pull/2516)

### UX improvements
* React to some HTTP responses by [@MaciejG604](https://github.com/MaciejG604) in [#2007](https://github.com/VirtusLab/scala-cli/pull/2007)
* Chore/group warnings about directives in multiple files by [@MaciejG604](https://github.com/MaciejG604) in [#2550](https://github.com/VirtusLab/scala-cli/pull/2550)
* Migrate to Docusaurus v3, add local search plugin by [@MaciejG604](https://github.com/MaciejG604) in [#2590](https://github.com/VirtusLab/scala-cli/pull/2590)

### Enhancements
* Default to publish repository configured for local machine when inferring publish.ci.repository by [@MaciejG604](https://github.com/MaciejG604) in [#2571](https://github.com/VirtusLab/scala-cli/pull/2571)
* Skip validation for default Scala versions, add build test by [@MaciejG604](https://github.com/MaciejG604) in [#2576](https://github.com/VirtusLab/scala-cli/pull/2576)

### Fixes
* Take into consideration --project-version when creating BuildInfo by [@MaciejG604](https://github.com/MaciejG604) in [#2548](https://github.com/VirtusLab/scala-cli/pull/2548)
* Workaround for home.dir property not being set by [@MaciejG604](https://github.com/MaciejG604) in [#2573](https://github.com/VirtusLab/scala-cli/pull/2573)
* Pass scalac arguments as file by [@MaciejG604](https://github.com/MaciejG604) in [#2584](https://github.com/VirtusLab/scala-cli/pull/2584)

### Documentation changes
* Add a doc on Windows anti-malware submission procedure by [@Gedochao](https://github.com/Gedochao) in [#2546](https://github.com/VirtusLab/scala-cli/pull/2546)
* Fix list of licenses URL by [@JD557](https://github.com/JD557) in [#2552](https://github.com/VirtusLab/scala-cli/pull/2552)
* Fix Windows secrets path in the documentation by [@JD557](https://github.com/JD557) in [#2561](https://github.com/VirtusLab/scala-cli/pull/2561)
* Update the pgp-pair section of publish setup docs by [@MaciejG604](https://github.com/MaciejG604) in [#2565](https://github.com/VirtusLab/scala-cli/pull/2565)
* Back port of documentation changes to main by [@github-actions](https://github.com/features/actions) in [#2569](https://github.com/VirtusLab/scala-cli/pull/2569)
* Document --python flag by [@MaciejG604](https://github.com/MaciejG604) in [#2574](https://github.com/VirtusLab/scala-cli/pull/2574)
* Document publishing process configuration by [@MaciejG604](https://github.com/MaciejG604) in [#2580](https://github.com/VirtusLab/scala-cli/pull/2580)
* Back port of documentation changes to main by [@github-actions](https://github.com/features/actions) in [#2593](https://github.com/VirtusLab/scala-cli/pull/2593)

### Build and internal changes
* Exclude conflicting dependencies by [@MaciejG604](https://github.com/MaciejG604) in [#2541](https://github.com/VirtusLab/scala-cli/pull/2541)
* Generate test reports on the CI by [@Gedochao](https://github.com/Gedochao) in [#2543](https://github.com/VirtusLab/scala-cli/pull/2543)
* Use the latest `scala-cli` in `macos-m1-tests` by [@Gedochao](https://github.com/Gedochao) in [#2554](https://github.com/VirtusLab/scala-cli/pull/2554)
* Install `scala-cli` with `cs` on M1 by [@Gedochao](https://github.com/Gedochao) in [#2555](https://github.com/VirtusLab/scala-cli/pull/2555)
* Fix generating test reports for failed suites by [@Gedochao](https://github.com/Gedochao) in [#2564](https://github.com/VirtusLab/scala-cli/pull/2564)
* Pin `scala-cli-setup` version to be M1-compatible & use it in `native-macos-m1-tests` by [@Gedochao](https://github.com/Gedochao) in [#2568](https://github.com/VirtusLab/scala-cli/pull/2568)
* Add log separators for integration and build tests by [@MaciejG604](https://github.com/MaciejG604) in [#2570](https://github.com/VirtusLab/scala-cli/pull/2570)
* Adjust test report generation to mill 0.11.6 bump changes by [@Gedochao](https://github.com/Gedochao) in [#2577](https://github.com/VirtusLab/scala-cli/pull/2577)
* Bump MacOS CI to `macOS-13` by [@Gedochao](https://github.com/Gedochao) in [#2579](https://github.com/VirtusLab/scala-cli/pull/2579)
* Add env for configuring home directory overriding by [@MaciejG604](https://github.com/MaciejG604) in [#2587](https://github.com/VirtusLab/scala-cli/pull/2587)

### Updates and maintenance
* Update trees_2.13 to 4.8.13 by [@scala-steward](https://github.com/scala-steward) in [#2532](https://github.com/VirtusLab/scala-cli/pull/2532)
* Update scala-cli.sh launcher for 1.0.6 by [@github-actions](https://github.com/features/actions) in [#2542](https://github.com/VirtusLab/scala-cli/pull/2542)
* chore: Update Bloop to v1.5.11-sc by [@tgodzik](https://github.com/tgodzik) in [#2557](https://github.com/VirtusLab/scala-cli/pull/2557)
* Update trees_2.13 to 4.8.14 by [@scala-steward](https://github.com/scala-steward) in [#2560](https://github.com/VirtusLab/scala-cli/pull/2560)
* Update scalafmt-cli_2.13, scalafmt-core to 3.7.17 by [@scala-steward](https://github.com/scala-steward) in [#2559](https://github.com/VirtusLab/scala-cli/pull/2559)
* Bump VirtusLab/scala-cli-setup from 1.0.5 to 1.0.6 by [@dependabot](https://github.com/dependabot) in [#2567](https://github.com/VirtusLab/scala-cli/pull/2567)
* Update ammonite to 3.0.0-M0-59-cdeaa580 by [@scala-steward](https://github.com/scala-steward) in [#2558](https://github.com/VirtusLab/scala-cli/pull/2558)
* Update mill-main to 0.11.6 by [@scala-steward](https://github.com/scala-steward) in [#2572](https://github.com/VirtusLab/scala-cli/pull/2572)
* Update coursier-jvm_2.13, ... to 2.1.8 by [@scala-steward](https://github.com/scala-steward) in [#2575](https://github.com/VirtusLab/scala-cli/pull/2575)
* Update ammonite to 3.0.0-M0-60-89836cd8 by [@scala-steward](https://github.com/scala-steward) in [#2586](https://github.com/VirtusLab/scala-cli/pull/2586)
* Bump `coursier` to `v2.1.8` where it wasn't consistent by [@Gedochao](https://github.com/Gedochao) in [#2588](https://github.com/VirtusLab/scala-cli/pull/2588)

## New Contributors
* [@philwalk](https://github.com/philwalk) made their first contribution in [#2516](https://github.com/VirtusLab/scala-cli/pull/2516)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.0.6...v1.1.0

## [v1.0.6](https://github.com/VirtusLab/scala-cli/releases/tag/v1.0.6)

### Scala CLI won't default to the system JVM if it's not supported anymore

If your `JAVA_HOME` environment variable has been pointing to a JVM that is no longer supported by Scala CLI 
(so anything below 17, really), you may have run into an error like this one with Scala CLI v1.0.5:

```bash ignore
scala-cli --power bloop exit 
# Stopped Bloop server.  
export JAVA_HOME=$(cs java-home --jvm zulu:8)
scala-cli -e 'println(System.getProperty("java.version"))'                
# Starting compilation server
# Error: bloop.rifle.FailedToStartServerExitCodeException: Server failed with exit code 1
# For more details, please see '/var/folders/5n/_ggj7kk93czdt_n0jzrk8s780000gn/T/1343202731019130640/.scala-build/stacktraces/1699527280-9858975811713766588.log'
# Running
#   scala-cli --power bloop output
# might give more details.
```

This is because we no longer support JVM \<17 with Scala CLI v1.0.5, but we still have been defaulting to whatever JVM 
was defined in `JAVA_HOME`. As a result, Bloop has been failing to start when running with, say, `JAVA_HOME` pointing 
to Java 8.

This is no longer the case. Scala CLI will now automatically download Java 17 for Bloop in such a situation 
(and still use the JVM from `JAVA_HOME` for running the code, while Bloop runs on 17).

```bash ignore
scala-cli --power bloop exit 
# Stopped Bloop server.  
export JAVA_HOME=$(cs java-home --jvm zulu:8)
scala-cli -e 'println(System.getProperty("java.version"))'                
# Starting compilation server
# Compiling project (Scala 3.3.1, JVM (8))
# Compiled project (Scala 3.3.1, JVM (8))
# 1.8.0_392
```

Added by [@tgodzik](https://github.com/tgodzik) in [#2508](https://github.com/VirtusLab/scala-cli/pull/2508).

## Other changes

### Fixes
* Fix `--watch` failing on invalid `PathWatchers.Event` & skip wonky tests on Mac CI by [@Gedochao](https://github.com/Gedochao) in [#2515](https://github.com/VirtusLab/scala-cli/pull/2515)
* bugfix: Don't try to always get system jvm first by [@tgodzik](https://github.com/tgodzik) in [#2508](https://github.com/VirtusLab/scala-cli/pull/2508)

### Documentation changes
* Back port of documentation changes to main by [@github-actions](https://github.com/features/actions) in [#2522](https://github.com/VirtusLab/scala-cli/pull/2522)
* add cookbook about Emacs integration by [@ag91](https://github.com/ag91) in [#2506](https://github.com/VirtusLab/scala-cli/pull/2506)

### Build and internal changes
* Bump actions/setup-node from 3 to 4 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#2493](https://github.com/VirtusLab/scala-cli/pull/2493)
* Update scala-cli.sh launcher for 1.0.5 by [@github-actions](https://github.com/features/actions) in [#2500](https://github.com/VirtusLab/scala-cli/pull/2500)
* Simplify build by [@lolgab](https://github.com/lolgab) in [#2512](https://github.com/VirtusLab/scala-cli/pull/2512)
* Fix wonky native MacOS CI on `stable` branch by [@Gedochao](https://github.com/Gedochao) in [#2518](https://github.com/VirtusLab/scala-cli/pull/2518)
* Add regexes for release-notes github reference swapping by [@MaciejG604](https://github.com/MaciejG604) in [#2519](https://github.com/VirtusLab/scala-cli/pull/2519)

### Updates and maintenance
* Update scalafmt-cli_2.13, scalafmt-core to 3.7.15 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2498](https://github.com/VirtusLab/scala-cli/pull/2498)
* Switch `lightweight-spark-distrib` to the VL fork & bump to `0.0.5` by [@Gedochao](https://github.com/Gedochao) in [#2503](https://github.com/VirtusLab/scala-cli/pull/2503)
* Bump VirtusLab/scala-cli-setup from 1.0.4 to 1.0.5 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#2504](https://github.com/VirtusLab/scala-cli/pull/2504)
* Switch `java-class-name` to the VL fork & bump to `0.1.3` by [@Gedochao](https://github.com/Gedochao) in [#2502](https://github.com/VirtusLab/scala-cli/pull/2502)
* Update sbt to 1.9.7 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2505](https://github.com/VirtusLab/scala-cli/pull/2505)
* Update os-lib to 0.9.2 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2514](https://github.com/VirtusLab/scala-cli/pull/2514)
* Update case-app to 2.1.0-M26 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2513](https://github.com/VirtusLab/scala-cli/pull/2513)
* Update mill-main to 0.11.5 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) & [@MaciejG604](https://github.com/MaciejG604) in [#2446](https://github.com/VirtusLab/scala-cli/pull/2446)
* Update core_2.13 to 3.9.1 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2521](https://github.com/VirtusLab/scala-cli/pull/2521)
* Switch `nocrc32-zip-input-stream` to the VL fork & bump it to `0.1.2` by [@Gedochao](https://github.com/Gedochao) in [#2520](https://github.com/VirtusLab/scala-cli/pull/2520)

## New Contributors
* [@ag91](https://github.com/ag91) made their first contribution in [#2506](https://github.com/VirtusLab/scala-cli/pull/2506)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.0.5...v1.0.6

## [v1.0.5](https://github.com/VirtusLab/scala-cli/releases/tag/v1.0.5)

## What's new

### Accept `--power` from anywhere

The `--power` flag used to be a launcher option, which means it used to only be accepted when passed
before the sub-command name. Now, it can be passed anywhere in the command line.

```bash
scala-cli --power package --help
scala-cli package --power --help
scala-cli package --help --power
```

Added by [@MaciejG604](https://github.com/MaciejG604) in [#2399](https://github.com/VirtusLab/scala-cli/pull/2399)

### Offline mode (experimental)

It is now possible to run Scala CLI in offline mode for the cases when you don't want the runner 
to make any network requests for whatever reason.
This changes Coursier's cache policy to `LocalOnly`, preventing it from downloading anything.

```bash ignore
scala-cli compile . --offline --power
```

Of course, this means that you will have to have all the dependencies relevant to your build 
already downloaded and available in your local cache.
Reasonable fallbacks will be used where possible, 
e.g. the Scala compiler may be used instead of Bloop if Bloop isn't available.

Added by [@MaciejG604](https://github.com/MaciejG604) in [#2404](https://github.com/VirtusLab/scala-cli/pull/2404)

### Shorter install script link

Scala CLI's install script is now available behind a conveniently shorter web address:
https://scala-cli.virtuslab.org/get

Added by [@Gedochao](https://github.com/Gedochao) in [#2450](https://github.com/VirtusLab/scala-cli/pull/2450)

### The `fix` sub-command (experimental)

The `fix` sub-command is a new addition to Scala CLI. It allows to scan your project for `using` directives 
and extract them into the `project.scala` file placed in the project root directory. 
This allows to easily fix warnings tied to having `using` directives present in multiple files.

```bash ignore
scala-cli fix . --power
```

Added by [@MaciejG604](https://github.com/MaciejG604) in [#2309](https://github.com/VirtusLab/scala-cli/pull/2309)

### Build static & shared libraries with Scala Native (experimental)

You can now use the `--native-target` option to build Scala Native projects as static or shared libraries.

```bash ignore
scala-cli package . --power --native-target static
scala-cli package . --power --native-target dynamic
```

Added by [@keynmol](https://github.com/keynmol) in [#2196](https://github.com/VirtusLab/scala-cli/pull/2196)

### Print platform version

Platform version is now always logged during compilation.

```bash ignore
scala-cli compile .
# Compiling project (Scala 3.3.1, JVM (17))
# Compiled project (Scala 3.3.1, JVM (17))
scala-cli compile . --js
# Compiling project (Scala 3.3.1, Scala.js 1.13.2)
# Compiled project (Scala 3.3.1, Scala.js 1.13.2)
scala-cli compile . --native
# Compiling project (Scala 3.3.1, Scala Native 0.4.16)
# Compiled project (Scala 3.3.1, Scala Native 0.4.16)
```

Added by [@Gedochao](https://github.com/Gedochao) in [#2465](https://github.com/VirtusLab/scala-cli/pull/2465)

## Other changes

### Enhancements
* Accumulate exp warnings with logger by [@MaciejG604](https://github.com/MaciejG604) in [#2376](https://github.com/VirtusLab/scala-cli/pull/2376)
* Remove ComputeVersion.Command, make ComputeVersion classes positioned by [@MaciejG604](https://github.com/MaciejG604) in [#2350](https://github.com/VirtusLab/scala-cli/pull/2350)
* Add more configuration for publish by [@MaciejG604](https://github.com/MaciejG604) in [#2435](https://github.com/VirtusLab/scala-cli/pull/2435)
* Warn about transitive using file directive by [@MaciejG604](https://github.com/MaciejG604) in [#2432](https://github.com/VirtusLab/scala-cli/pull/2432)
* Support Scala Native 0.5.x changes in publishing artifacts by [@WojciechMazur](https://github.com/WojciechMazur) in [#2460](https://github.com/VirtusLab/scala-cli/pull/2460)

### Fixes
* Fix - set es version into scala-js-cli by [@lwronski](https://github.com/lwronski) in [#2351](https://github.com/VirtusLab/scala-cli/pull/2351)
* Modify the format of StrictDirective.toString by [@MaciejG604](https://github.com/MaciejG604) in [#2355](https://github.com/VirtusLab/scala-cli/pull/2355)
* Make explicitly passed scala version use the latest release, not the default one by [@MaciejG604](https://github.com/MaciejG604) in [#2411](https://github.com/VirtusLab/scala-cli/pull/2411)
* Release flag by [@lwronski](https://github.com/lwronski) in [#2413](https://github.com/VirtusLab/scala-cli/pull/2413)
* Ensure build resolution is kept when packaging assemblies with provided dependencies by [@Gedochao](https://github.com/Gedochao) in [#2457](https://github.com/VirtusLab/scala-cli/pull/2457)
* Fix `fmt` sub-command exit code to mirror `scalafmt` by [@Gedochao](https://github.com/Gedochao) in [#2463](https://github.com/VirtusLab/scala-cli/pull/2463)
* Fix 'JVM too old' as bsp by [@MaciejG604](https://github.com/MaciejG604) in [#2445](https://github.com/VirtusLab/scala-cli/pull/2445)
* Read java props from env vars by [@MaciejG604](https://github.com/MaciejG604) in [#2356](https://github.com/VirtusLab/scala-cli/pull/2356)
* Make script wrapper satisfy compiler checks by [@MaciejG604](https://github.com/MaciejG604) in [#2414](https://github.com/VirtusLab/scala-cli/pull/2414)
* Load local ivy path from ivy.home and user.home system properties by [@JD557](https://github.com/JD557) in [#2484](https://github.com/VirtusLab/scala-cli/pull/2484)

### Documentation changes
* Fix typo in buildInfo directive docs by [@izzyreal](https://github.com/izzyreal) in [#2357](https://github.com/VirtusLab/scala-cli/pull/2357)
* configuration.md examples "using dep" to current versions by [@SunKing2](https://github.com/SunKing2) in [#2398](https://github.com/VirtusLab/scala-cli/pull/2398)
* Documentation updates by [@MaciejG604](https://github.com/MaciejG604) in [#2375](https://github.com/VirtusLab/scala-cli/pull/2375)
* Fix publish directives usage displayed in one line, unify directive docs by [@MaciejG604](https://github.com/MaciejG604) in [#2381](https://github.com/VirtusLab/scala-cli/pull/2381)
* Backport of docs change (#2391) by [@MaciejG604](https://github.com/MaciejG604) in [#2403](https://github.com/VirtusLab/scala-cli/pull/2403)
* Add internal docs for scalajs-cli by [@lwronski](https://github.com/lwronski) in [#2434](https://github.com/VirtusLab/scala-cli/pull/2434)
* Add docs for fix command by [@MaciejG604](https://github.com/MaciejG604) in [#2437](https://github.com/VirtusLab/scala-cli/pull/2437)
* Add docs for offline mode by [@MaciejG604](https://github.com/MaciejG604) in [#2475](https://github.com/VirtusLab/scala-cli/pull/2475)
* Update dependencies.md to mention jitpack by [@doofin](https://github.com/doofin) in [#2458](https://github.com/VirtusLab/scala-cli/pull/2458)
* Update the list of external repositories Scala CLI depends on by [@Gedochao](https://github.com/Gedochao) in [#2476](https://github.com/VirtusLab/scala-cli/pull/2476)
* Update the docs to no longer treat --power as a launcher-only option by [@Gedochao](https://github.com/Gedochao) in [#2478](https://github.com/VirtusLab/scala-cli/pull/2478)

### Build and internal changes
* Add test for actionable diagnostics from compiler by [@MaciejG604](https://github.com/MaciejG604) in [#2327](https://github.com/VirtusLab/scala-cli/pull/2327)
* Pin the versions of Github CI runners by [@MaciejG604](https://github.com/MaciejG604) in [#2370](https://github.com/VirtusLab/scala-cli/pull/2370)
* Remove bloop timeouts in tests by [@MaciejG604](https://github.com/MaciejG604) in [#2407](https://github.com/VirtusLab/scala-cli/pull/2407)
* Add post-update hook for reference doc generation by [@MaciejG604](https://github.com/MaciejG604) in [#2406](https://github.com/VirtusLab/scala-cli/pull/2406)
* Add tests which check availability of scalafmt native launcher for de… by [@lwronski](https://github.com/lwronski) in [#2418](https://github.com/VirtusLab/scala-cli/pull/2418)
* Default to a Scala version for REPL if there are no Scala artifacts. by [@trilleplay](https://github.com/trilleplay) in [#2431](https://github.com/VirtusLab/scala-cli/pull/2431)
* Remove unused snippet checker by [@lwronski](https://github.com/lwronski) in [#2423](https://github.com/VirtusLab/scala-cli/pull/2423)
* Allow to override internal & user default Scala versions for `mill` builds by [@Gedochao](https://github.com/Gedochao) in [#2461](https://github.com/VirtusLab/scala-cli/pull/2461)
* NIT: Refactor: Rely on global --power option where able in cli commands by [@Gedochao](https://github.com/Gedochao) in [#2480](https://github.com/VirtusLab/scala-cli/pull/2480)

### Updates and maintenance
* Update scala-cli.sh launcher for 1.0.4 by [@github-actions](https://github.com/features/actions) in [#2344](https://github.com/VirtusLab/scala-cli/pull/2344)
* Update bloop-rifle_2.13 to 1.5.9-sc-2 by [@lwronski](https://github.com/lwronski) in [#2345](https://github.com/VirtusLab/scala-cli/pull/2345)
* Update core_2.13 to 3.9.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2346](https://github.com/VirtusLab/scala-cli/pull/2346)
* Update sbt to 1.9.3 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2349](https://github.com/VirtusLab/scala-cli/pull/2349)
* Bump VirtusLab/scala-cli-setup from 1.0.2 to 1.0.4 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#2348](https://github.com/VirtusLab/scala-cli/pull/2348)
* Update coursier-jvm_2.13, ... to 2.1.6 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2360](https://github.com/VirtusLab/scala-cli/pull/2360)
* Update trees_2.13 to 4.8.9 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2369](https://github.com/VirtusLab/scala-cli/pull/2369)
* Update scalafmt-cli_2.13, scalafmt-core to 3.7.13 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2368](https://github.com/VirtusLab/scala-cli/pull/2368)
* Update bloop-rifle_2.13 to 1.5.11-sc-1 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2383](https://github.com/VirtusLab/scala-cli/pull/2383)
* Update org.eclipse.jgit to 6.6.1.202309021850-r by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2384](https://github.com/VirtusLab/scala-cli/pull/2384)
* Update trees_2.13 to 4.8.10 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2387](https://github.com/VirtusLab/scala-cli/pull/2387)
* Update coursier-jvm_2.13, ... to 2.1.7 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2393](https://github.com/VirtusLab/scala-cli/pull/2393)
* Bump docker/login-action from 2 to 3 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#2400](https://github.com/VirtusLab/scala-cli/pull/2400)
* Update org.eclipse.jgit to 6.7.0.202309050840-r by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2395](https://github.com/VirtusLab/scala-cli/pull/2395)
* Update scala3-library to 3.3.1 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2392](https://github.com/VirtusLab/scala-cli/pull/2392)
* Update slf4j-nop to 2.0.9 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2388](https://github.com/VirtusLab/scala-cli/pull/2388)
* Update file-tree-views to 2.1.11 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2410](https://github.com/VirtusLab/scala-cli/pull/2410)
* Update test-runner, tools to 0.4.15 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2385](https://github.com/VirtusLab/scala-cli/pull/2385)
* Update scala-library to 2.13.12 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2396](https://github.com/VirtusLab/scala-cli/pull/2396)
* Update scalafmt-cli_2.13, scalafmt-core to 3.7.14 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2386](https://github.com/VirtusLab/scala-cli/pull/2386)
* Update file-tree-views to 2.1.12 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2419](https://github.com/VirtusLab/scala-cli/pull/2419)
* Update bsp4j to 2.1.0-M6 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2401](https://github.com/VirtusLab/scala-cli/pull/2401)
* Update trees_2.13 to 4.8.11 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2429](https://github.com/VirtusLab/scala-cli/pull/2429)
* Update asm to 9.6 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2442](https://github.com/VirtusLab/scala-cli/pull/2442)
* Update bsp4j to 2.1.0-M7 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2438](https://github.com/VirtusLab/scala-cli/pull/2438)
* Update metaconfig-typesafe-config to 0.12.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2439](https://github.com/VirtusLab/scala-cli/pull/2439)
* Update ammonite to 3.0.0-M0-56-1bcbe7f6 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2440](https://github.com/VirtusLab/scala-cli/pull/2440)
* Bump Scala Native to 0.4.16 & log platform version by [@Gedochao](https://github.com/Gedochao) in [#2465](https://github.com/VirtusLab/scala-cli/pull/2465)
* Update guava to 32.1.3-jre by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2467](https://github.com/VirtusLab/scala-cli/pull/2467)
* Update trees_2.13 to 4.8.12 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2468](https://github.com/VirtusLab/scala-cli/pull/2468)
* Bump actions/checkout from 3 to 4 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#2378](https://github.com/VirtusLab/scala-cli/pull/2378)
* Bump coursier/setup-action from 1.3.3 to 1.3.4 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#2424](https://github.com/VirtusLab/scala-cli/pull/2424)
* Bump coursier-publish from 0.1.4 to 0.1.5 by [@MaciejG604](https://github.com/MaciejG604) in [#2433](https://github.com/VirtusLab/scala-cli/pull/2433)
* Bump scalajs-cli to 1.14.0 by [@MaciejG604](https://github.com/MaciejG604) in [#2491](https://github.com/VirtusLab/scala-cli/pull/2491)
* Bump scala-cli-signing to 0.2.3 by [@Gedochao](https://github.com/Gedochao) in [#2486](https://github.com/VirtusLab/scala-cli/pull/2486)
* Bump gcbenchmark dependencies by [@Gedochao](https://github.com/Gedochao) in [#2481](https://github.com/VirtusLab/scala-cli/pull/2481)

## New Contributors
* [@SunKing2](https://github.com/SunKing2) made their first contribution in [#2398](https://github.com/VirtusLab/scala-cli/pull/2398)
* [@trilleplay](https://github.com/trilleplay) made their first contribution in [#2431](https://github.com/VirtusLab/scala-cli/pull/2431)
* [@WojciechMazur](https://github.com/WojciechMazur) made their first contribution in [#2460](https://github.com/VirtusLab/scala-cli/pull/2460)
* [@JD557](https://github.com/JD557) made their first contribution in [#2484](https://github.com/VirtusLab/scala-cli/pull/2484)
* [@doofin](https://github.com/doofin) made their first contribution in [#2458](https://github.com/VirtusLab/scala-cli/pull/2458)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.0.4...v1.0.5

## [v1.0.4](https://github.com/VirtusLab/scala-cli/releases/tag/v1.0.4)

### Hotfix for buildTarget/jvmRunEnvironment in BSP

We've addressed a bug that surfaced when opening your ScalaCLI projects in Metals or IntelliJ. If you encountered the following log:

```
2023.08.09 15:48:34 INFO  BSP server: Caused by: java.lang.IllegalArgumentException: Type ch.epfl.scala.bsp4j.JvmMainClass is instantiated reflectively but was never registered. Register the type by adding "unsafeAllocated" for the type in reflect-config.json.
2023.08.09 15:48:34 INFO  BSP server: 	at com.oracle.svm.core.graal.snippets.SubstrateAllocationSnippets.instanceHubErrorStub(SubstrateAllocationSnippets.java:309)
2023.08.09 15:48:34 INFO  BSP server: 	at jdk.unsupported@17.0.6/sun.misc.Unsafe.allocateInstance(Unsafe.java:864)
2023.08.09 15:48:34 INFO  BSP server: 	... 36 more
```

those logs should no longer appear.

Thanks to [@lwronski](https://github.com/lwronski) for providing the fix in [#2342](https://github.com/VirtusLab/scala-cli/pull/2342).


## [v1.0.3](https://github.com/VirtusLab/scala-cli/releases/tag/v1.0.3)

## What's new

### Access project configuration with the new `BuildInfo`

`BuildInfo` access your project's build configuration within your Scala code. This feature automatically gathers and generates build information about your project, making project details instantly accessible at runtime.

To generate BuildInfo, either use the `--build-info` command line option or include the `//> using buildInfo` directive in your code.

Upon activation, a `BuildInfo` object becomes accessible on your project's classpath. To use it, simply add the following import into your code:

```
import scala.cli.build.BuildInfo
```

This `BuildInfo` object encapsulates information such as the Scala version used, target platform, main class, scalac options, dependencies, and much more for both Main and Test scopes. The generation ensures up-to-date configuration data from both the console options and using directives in your project's sources.

Added by [@MaciejG604](https://github.com/MaciejG604) in [#2249](https://github.com/VirtusLab/scala-cli/pull/2249).

### CompileOnly Dependencies

Now, users can declare dependencies that are exclusively included at the compile time. These dependencies are added to the classpath during compilation, but won't be included when the application is run, keeping your runtime environment lightweight.

To declare such a dependency:

1.  Via the using directive:
```
//> using compileOnly.dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros:2.23.2"
```
2. Via the command line:
```
scala-cli Hello.scala --compile-dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros:2.23.2"
```

Added by @alexarchambault and [@lwronski](https://github.com/lwronski) in [#2299](https://github.com/VirtusLab/scala-cli/pull/2299), Thanks!


### Set globally Java properties

Scala CLI allows users to globally set Java properties for its launcher using the `config` command. This will simplify the JVM properties management process, eliminating the need to pass these properties with each `scala-cli` execution.

To set global Java properties execute the following command:

```
scala-cli config java.properties Djavax.net.ssl.trustStore=cacerts Dfoo=bar2
```

When modifying Java properties, remember that you must redefine all of them. It's not possible to update just a single property. Essentially, each time you use the `config` command for Java properties, you replace the entire list of properties.

Whenever overwriting existing Java properties Scala CLI will let you know what was the previous value and in interactive mode ensure that you are ok with replacing them.

Added by [@lwronski](https://github.com/lwronski) in [#2317](https://github.com/VirtusLab/scala-cli/pull/2317), Thanks!

### Rename parameter for `publish` command

We've updated the `--version` parameter for the publish command. Now, when specifying the project version, use `--project-version` instead.

```bash ignore 
scala-cli publish --project-version 1.0.3 ...
```

## Other changes
* Add custom exception and throw it when node not found in the path by [@lwronski](https://github.com/lwronski) in [#2323](https://github.com/VirtusLab/scala-cli/pull/2323)
* Skip reading ide-options-v2.json if doesn't exist to avoid throwing a… by [@lwronski](https://github.com/lwronski) in [#2333](https://github.com/VirtusLab/scala-cli/pull/2333)
* Skip setting release flag when user pass directly -release or -java-o… by [@lwronski](https://github.com/lwronski) in [#2321](https://github.com/VirtusLab/scala-cli/pull/2321)
* Prevent downloading Java 17 when running a REPL without sources by [@lwronski](https://github.com/lwronski) in [#2305](https://github.com/VirtusLab/scala-cli/pull/2305)
* Extract JAVA_HOME from /usr/libexec/java_home for Mac by [@lwronski](https://github.com/lwronski) in [#2304](https://github.com/VirtusLab/scala-cli/pull/2304)
* Bump case-app, add names limit to HelpFormat, move some name aliases, add test by [@MaciejG604](https://github.com/MaciejG604) in [#2280](https://github.com/VirtusLab/scala-cli/pull/2280)
* Build info with compute version [@MaciejG604](https://github.com/MaciejG604) in [#2310](https://github.com/VirtusLab/scala-cli/pull/2310)


### Fixes
* Fix - install ps, which is necessary for starting Bloop by [@lwronski](https://github.com/lwronski) in [#2332](https://github.com/VirtusLab/scala-cli/pull/2332)
* Load virtual data as byte arrays without encoding using UTF-8 by [@lwronski](https://github.com/lwronski) in [#2313](https://github.com/VirtusLab/scala-cli/pull/2313)
* Accept directive packageType native when using native platform by [@lwronski](https://github.com/lwronski) in [#2311](https://github.com/VirtusLab/scala-cli/pull/2311)
* Ignore url query params [@MaciejG604](https://github.com/MaciejG604) in [#2334](https://github.com/VirtusLab/scala-cli/pull/2334)

### Documentation changes
* Update runner specification by [@MaciejG604](https://github.com/MaciejG604) in [#2301](https://github.com/VirtusLab/scala-cli/pull/2301)
* Add WinGet to Windows installation methods by [@lwronski](https://github.com/lwronski) in [#2283](https://github.com/VirtusLab/scala-cli/pull/2283)
* Add missing caution to Password options and fix displaying command in… by [@lwronski](https://github.com/lwronski) in [#2286](https://github.com/VirtusLab/scala-cli/pull/2286)
* Document BuildInfo [@MaciejG604](https://github.com/MaciejG604) in [#2325](https://github.com/VirtusLab/scala-cli/pull/2325)

### Build and internal changes
* Add timeout for resolving semanticDbVersion by [@lwronski](https://github.com/lwronski) in [#2322](https://github.com/VirtusLab/scala-cli/pull/2322)
* Resolve semanticDB for older scala version by [@lwronski](https://github.com/lwronski) in [#2318](https://github.com/VirtusLab/scala-cli/pull/2318)
* feat: use the new ScalaAction from BSP4J by [@ckipp01](https://github.com/ckipp01)  in [#2284](https://github.com/VirtusLab/scala-cli/pull/2284)


### Updates and maintenance
* Update scalafmt-cli_2.13, scalafmt-core to 3.7.12 by [@lwronski](https://github.com/lwronski) in [#2335](https://github.com/VirtusLab/scala-cli/pull/2335)
* Update trees_2.13 to 4.8.7 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2329](https://github.com/VirtusLab/scala-cli/pull/2329)
* Update guava to 32.1.2-jre by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2324](https://github.com/VirtusLab/scala-cli/pull/2324)
* Update bloop-rifle_2.13 to 1.5.9-sc-1 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2314](https://github.com/VirtusLab/scala-cli/pull/2314)
* Update scalafmt-cli_2.13, scalafmt-core to 3.7.11 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2315](https://github.com/VirtusLab/scala-cli/pull/2315)
* Update scalajs-sbt-test-adapter_2.13 to 1.13.2 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2240](https://github.com/VirtusLab/scala-cli/pull/2240)
* Bump VirtusLab/scala-cli-setup from 1.0.1 to 1.0.2 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#2300](https://github.com/VirtusLab/scala-cli/pull/2300)
* Update mill 0.11.1 by [@lwronski](https://github.com/lwronski) in [#2297](https://github.com/VirtusLab/scala-cli/pull/2297)
* deps: update mill-scalafix to 0.3.1 by [@ckipp01](https://github.com/ckipp01)  in [#2285](https://github.com/VirtusLab/scala-cli/pull/2285)
* Update scalafmt-cli_2.13, scalafmt-core to 3.7.10 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2295](https://github.com/VirtusLab/scala-cli/pull/2295)
* Update sbt to 1.9.2 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2288](https://github.com/VirtusLab/scala-cli/pull/2288)
* Update trees_2.13 to 4.8.4 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2290](https://github.com/VirtusLab/scala-cli/pull/2290)
* Update scala-cli.sh launcher for 1.0.2 by [@github-actions](https://github.com/features/actions) in [#2281](https://github.com/VirtusLab/scala-cli/pull/2281)
* Update trees_2.13 to 4.8.3 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2279](https://github.com/VirtusLab/scala-cli/pull/2279)
* Bump semver from 5.7.1 to 5.7.2 in /website by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#2276](https://github.com/VirtusLab/scala-cli/pull/2276)


**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.0.2...v1.0.3

## [v1.0.2](https://github.com/VirtusLab/scala-cli/releases/tag/v1.0.2)

## What's new

This release brings enhancements to Scala CLI:
- WinGet installation for Windows users
- better navigation with improved build target names
- introducing `new` command for Giter8 project generation
- easier JVM properties management with `.scalaopts` file support.

The release also includes numerous bug fixes, updates, and new contributors.

### Installation via WinGet on Windows

Scala CLI can now be installed via [WinGet](https://learn.microsoft.com/en-gb/windows/package-manager/) on Windows, with
a command such as

```bat
winget install virtuslab.scalacli
```

Added by [@mimoguz](https://github.com/mimoguz) in [#2239](https://github.com/VirtusLab/scala-cli/pull/2239), Thanks!

### Enhanced build target names

Now, the build target name will be derived from the workspace directory that contains it, making it easier for users to
navigate between different projects within a multi-root workspace. Instead of a build target named as `project_XYZ-XYZ`,
you will now see the name like `workspace_XYZ-XYZ`, where `workspace` refers to the name of the workspace directory.

```
.
├── scripts
│   ├── .scala-build
│   │   └── scripts_59f2159dd5
│   └── one.sc
├── skan
│   ├── .scala-build
│   │   └── skan_88b44a2858
│   └── main.scala
└── skan.code-workspace
```

Added by [@MaciejG604](https://github.com/MaciejG604) in [#2201](https://github.com/VirtusLab/scala-cli/pull/2201)

### Introducing 'new' command for Giter8 project generation

Giter8 is a project templating tool for Scala, and its integration within Scala CLI offers efficient way to set up new
projects. By using the `new` command, users can generate new projects based on predefined or custom templates.

For example:

```bash
scala-cli --power new VirtusLab/scala-cli.g8
```

Added by [@zetashift](https://github.com/zetashift) in [#2202](https://github.com/VirtusLab/scala-cli/pull/2202), Thanks!

### Loading Java Properties from `.scalaopts` into ScalaCLI launcher

ScalaCLI allows to load Java properties into `scala-cli` launcher directly from a `.scalaopts` file located in your
current working directory. This will simplify the JVM properties management process, eliminating the need to pass these
properties with each scala-cli execution.

For instance, if `-Djavax.net.ssl.trustStore=cacerts` and `-Dfoo2=bar2` are defined within your `.scalaopts` file, these
values will be loaded into `scala-cli` launcher:

```bash ignore
$ cat .scalaopts
-Djavax.net.ssl.trustStore=cacerts
-Dfoo2=bar2
$ scala-cli run ...
```

Added by [@lwronski](https://github.com/lwronski) in  [#2267](https://github.com/VirtusLab/scala-cli/pull/2267)

Please be aware that ScalaCLI will only process Java properties that it recognizes from the `.scalaopts` file. Other JVM
options, such as` -Xms1024m`, will be ignored as they can't be used within native image, and users will be alerted with 
a warning message when such non-compliant options are passed.

## Other changes

* Add publish.doc directive by [@lwronski](https://github.com/lwronski)
  in [#2245](https://github.com/VirtusLab/scala-cli/pull/2245)
* Fix pgp create with no java 17 by [@MaciejG604](https://github.com/MaciejG604)
  in [#2189](https://github.com/VirtusLab/scala-cli/pull/2189)
* Support for running standalone launcher of scala-cli with JVM 8 by [@lwronski](https://github.com/lwronski)
  in [#2253](https://github.com/VirtusLab/scala-cli/pull/2253)

### Fixes

* Make dependencies keep their positions when fetching by [@MaciejG604](https://github.com/MaciejG604)
  in [#2266](https://github.com/VirtusLab/scala-cli/pull/2266)
* Fix empty position in DependencyFormatErrors by [@MaciejG604](https://github.com/MaciejG604)
  in [#2261](https://github.com/VirtusLab/scala-cli/pull/2261)
* Script wrapper verification by [@MaciejG604](https://github.com/MaciejG604)
  in [#2227](https://github.com/VirtusLab/scala-cli/pull/2227)
* Fix - include test.resourceDir into sources for test scope by [@lwronski](https://github.com/lwronski)
  in [#2235](https://github.com/VirtusLab/scala-cli/pull/2235)
* Fix markdown - allow running .md files that start with a number by [@lwronski](https://github.com/lwronski)
  in [#2225](https://github.com/VirtusLab/scala-cli/pull/2225)
* Fix dep update error by [@MaciejG604](https://github.com/MaciejG604)
  in [#2211](https://github.com/VirtusLab/scala-cli/pull/2211)
* Add new mechanism for resolving scoped BuildOptions by [@MaciejG604](https://github.com/MaciejG604)
  in [#2274](https://github.com/VirtusLab/scala-cli/pull/2274)
* Fix - download cs from coursier-m1 as an archive by [@lwronski](https://github.com/lwronski)
  in [#2193](https://github.com/VirtusLab/scala-cli/pull/2193)
* Fix - Truncate file length to 0 when override content by [@lwronski](https://github.com/lwronski)
  in [#2188](https://github.com/VirtusLab/scala-cli/pull/2188)

### Documentation changes

* Add mentions that using target directives are experimental by [@MaciejG604](https://github.com/MaciejG604)
  in [#2262](https://github.com/VirtusLab/scala-cli/pull/2262)
* Fix inline code in directives docs by [@izzyreal](https://github.com/izzyreal)
  in [#2233](https://github.com/VirtusLab/scala-cli/pull/2233)
* Update docs - dependency parameters by [@lwronski](https://github.com/lwronski)
  in [#2224](https://github.com/VirtusLab/scala-cli/pull/2224)
* Update directive docs for Platform by [@lwronski](https://github.com/lwronski)
  in [#2213](https://github.com/VirtusLab/scala-cli/pull/2213)

### Build and internal changes

* Build changes by [@lwronski](https://github.com/lwronski) in [#2263](https://github.com/VirtusLab/scala-cli/pull/2263)
* Remove file change portion of test by [@MaciejG604](https://github.com/MaciejG604)
  in [#2251](https://github.com/VirtusLab/scala-cli/pull/2251)
* Add logging to 'watch with interactive' test by [@MaciejG604](https://github.com/MaciejG604)
  in [#2229](https://github.com/VirtusLab/scala-cli/pull/2229)
* Add support for parsing cancel params in native launcher of Scala CLI by [@lwronski](https://github.com/lwronski)
  in [#2195](https://github.com/VirtusLab/scala-cli/pull/2195)

### Updates and maintenance

* Update scalafmt-cli_2.13, scalafmt-core to 3.7.7
  by [@scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#2271](https://github.com/VirtusLab/scala-cli/pull/2271)
* Update trees_2.13 to 4.8.2 by [@scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#2272](https://github.com/VirtusLab/scala-cli/pull/2272)
* Update core_2.13 to 3.8.16 by [@scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#2270](https://github.com/VirtusLab/scala-cli/pull/2270)
* Update jimfs to 1.3.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#2269](https://github.com/VirtusLab/scala-cli/pull/2269)
* Update scalafmt-cli_2.13, scalafmt-core to 3.7.6
  by [@scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#2264](https://github.com/VirtusLab/scala-cli/pull/2264)
* Update trees_2.13 to 4.8.1 by [@scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#2265](https://github.com/VirtusLab/scala-cli/pull/2265)
* Update scalafmt-cli_2.13, scalafmt-core to 3.7.5
  by [@scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#2256](https://github.com/VirtusLab/scala-cli/pull/2256)
* Update trees_2.13 to 4.8.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#2257](https://github.com/VirtusLab/scala-cli/pull/2257)
* Update guava to 32.1.1-jre by [@scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#2259](https://github.com/VirtusLab/scala-cli/pull/2259)
* Update coursier-jvm_2.13, ... to 2.1.5 by [@scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#2232](https://github.com/VirtusLab/scala-cli/pull/2232)
* Update sbt to 1.9.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#2222](https://github.com/VirtusLab/scala-cli/pull/2222)
* Update dependency to 0.2.3 by [@scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#2219](https://github.com/VirtusLab/scala-cli/pull/2219)
* Update org.eclipse.jgit to 6.6.0.202305301015-r
  by [@scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#2220](https://github.com/VirtusLab/scala-cli/pull/2220)
* Updates - `amm` (`2.5.9`), `scala-library` (`2.12.18`, `2.13.11`) by [@lwronski](https://github.com/lwronski)
  in [#2223](https://github.com/VirtusLab/scala-cli/pull/2223)
* Update bsp4j to 2.1.0-M5 by [@scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#2216](https://github.com/VirtusLab/scala-cli/pull/2216)
* Update jsoniter-scala-core, ... to 2.23.2 by [@scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#2217](https://github.com/VirtusLab/scala-cli/pull/2217)
* Update scala-collection-compat to 2.11.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#2221](https://github.com/VirtusLab/scala-cli/pull/2221)
* Update test-runner, tools to 0.4.14 by [@scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#2192](https://github.com/VirtusLab/scala-cli/pull/2192)
* Bump VirtusLab/scala-cli-setup from 1.0.0 to 1.0.1
  by [@dependabot](https://docs.github.com/en/code-security/dependabot)
  in [#2207](https://github.com/VirtusLab/scala-cli/pull/2207)
* Update guava to 32.0.1-jre by [@scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#2197](https://github.com/VirtusLab/scala-cli/pull/2197)
* Update scala-cli.sh launcher for 1.0.1 by [@github-actions](https://github.com/features/actions) in [#2194](https://github.com/VirtusLab/scala-cli/pull/2194)
* Upgrade scripts to latest coursier by [@mkurz](https://github.com/mkurz)
  in [#1728](https://github.com/VirtusLab/scala-cli/pull/1728)

## New Contributors

* [@zetashift](https://github.com/zetashift) made their first contribution
  in [#2202](https://github.com/VirtusLab/scala-cli/pull/2202)
* [@izzyreal](https://github.com/izzyreal) made their first contribution
  in [#2233](https://github.com/VirtusLab/scala-cli/pull/2233)
* [@mimoguz](https://github.com/mimoguz) made their first contribution
  in [#2239](https://github.com/VirtusLab/scala-cli/pull/2239)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.0.1...v1.0.2

## [v1.0.1](https://github.com/VirtusLab/scala-cli/releases/tag/v1.0.1)

## What's new
This release only contains bug fixes and minor internal improvements.

### Fixes
* Fix - add test to output from name of script example by [@lwronski](https://github.com/lwronski) in [#2153](https://github.com/VirtusLab/scala-cli/pull/2153)
* Fix publishing with implicit `publish.version` coming from a `git` tag by [@Gedochao](https://github.com/Gedochao) in [#2154](https://github.com/VirtusLab/scala-cli/pull/2154)
* Fix conflicts when watch and interactive try to read StdIn by [@MaciejG604](https://github.com/MaciejG604) in [#2168](https://github.com/VirtusLab/scala-cli/pull/2168)
* Bsp wrapper fixes by [@MaciejG604](https://github.com/MaciejG604) in [#2171](https://github.com/VirtusLab/scala-cli/pull/2171)
* Add the .exe suffix to output provided by user for graalvm-native-image by [@lwronski](https://github.com/lwronski) in [#2182](https://github.com/VirtusLab/scala-cli/pull/2182)

### Build and internal changes
* refactor: Remove JavaInterface, which causes compilation issues with Bloop by [@tgodzik](https://github.com/tgodzik) in [#2174](https://github.com/VirtusLab/scala-cli/pull/2174)
* Enforce to use jvm 17 on linux aarch64 by [@lwronski](https://github.com/lwronski) in [#2180](https://github.com/VirtusLab/scala-cli/pull/2180)

### Updates and maintenance
* Update scala-cli.sh launcher for 1.0.0 by [@github-actions](https://github.com/features/actions) in [#2149](https://github.com/VirtusLab/scala-cli/pull/2149)
* Back port of documentation changes to main by [@github-actions](https://github.com/features/actions) in [#2155](https://github.com/VirtusLab/scala-cli/pull/2155)
* Update jsoniter-scala-core, ... to 2.23.1 by [@scala-steward](https://github.com/scala-steward) in [#2160](https://github.com/VirtusLab/scala-cli/pull/2160)
* Update guava to 32.0.0-jre by [@scala-steward](https://github.com/scala-steward) in [#2161](https://github.com/VirtusLab/scala-cli/pull/2161)
* Update coursier-jvm_2.13, ... to 2.1.4 by [@scala-steward](https://github.com/scala-steward) in [#2162](https://github.com/VirtusLab/scala-cli/pull/2162)
* Update sbt to 1.8.3 by [@scala-steward](https://github.com/scala-steward) in [#2164](https://github.com/VirtusLab/scala-cli/pull/2164)
* Bump `mill` scripts by [@Gedochao](https://github.com/Gedochao) in [#2167](https://github.com/VirtusLab/scala-cli/pull/2167)
* Bump VirtusLab/scala-cli-setup from 0.2.1 to 1.0.0 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#2169](https://github.com/VirtusLab/scala-cli/pull/2169)
* Bump `scala-cli-signing` to `0.2.2` by [@Gedochao](https://github.com/Gedochao) in [#2173](https://github.com/VirtusLab/scala-cli/pull/2173)
* Update scalafmt-cli_2.13, scalafmt-core to 3.7.4 by [@scala-steward](https://github.com/scala-steward) in [#2175](https://github.com/VirtusLab/scala-cli/pull/2175)
* Update trees_2.13 to 4.7.8 by [@scala-steward](https://github.com/scala-steward) in [#2176](https://github.com/VirtusLab/scala-cli/pull/2176)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.0.0...v1.0.1

## [v1.0.0](https://github.com/VirtusLab/scala-cli/releases/tag/v1.0.0)

## The official `scala` runner release

Scala CLI has reached the highly anticipated `1.0.0` milestone!
Having addressed all the [SIP-46](https://github.com/scala/improvement-proposals/pull/46) requirements,
this version is going to become the official `scala` runner, replacing the old `scala` command.

For a deeper understanding of Scala CLI as the new `scala` runner and to explore its benefits and features,
we encourage you to check out our [blogpost](https://virtuslab.com/blog/scala-cli-the-new-scala-runner/).

Also be sure to get familiar with all the differences introduced by this change in our [migration guide](guides/introduction/old-runner-migration.md).

## What's Changed

### New default Scala version - 3.3.0

Scala 3.3.0 is now the default version for Scala CLI projects.
It's the first LTS (Long Term Support) release of Scala 3 to be used by Scala CLI.
Right on time for 1.0.0!

Added by [@lwronski](https://github.com/lwronski) in [#2140](https://github.com/VirtusLab/scala-cli/pull/2140)

### Toolkit-test

By incorporating the [Scala Toolkit](https://github.com/scala/toolkit) into your project, you gain the advantage of two additional
dependencies seamlessly integrated into your classpath:
- `org.scala-lang:toolkit:<version>` is added to the main scope, allowing its utilization throughout your project.
- `org.scala-lang:toolkit-test:<version>` is included in the test scope, making it available exclusively for testing purposes.

Scala CLI now supports the following features for the toolkit:
* including e.g. `//> using toolkit latest` in any main scope file will automatically add the `toolkit` dependency to the main scope and the `toolkit-test` dependency to the test scope
* if you place e.g. `//> using toolkit latest` within a test scope file, both `toolkit` and `toolkit-test` will be limited to the test scope only
* inserting e.g. `//> using test.toolkit latest` anywhere in the project will add both `toolkit` and `toolkit-test` to the test scope only

This convention is encouraged for other toolkit-like libraries as well.

Added by [@Gedochao](https://github.com/Gedochao) in [#2127](https://github.com/VirtusLab/scala-cli/pull/2127) and [#2137](https://github.com/VirtusLab/scala-cli/pull/2137)

### Forcing an object wrapper for scripts

Scala CLI now supports the `//> using objectWrapper` directive, along with the corresponding `--object-wrapper` option,
which allows to force wrapping script code in an object body instead of a class.

Using object wrappers should be avoided for scripts relying on multi-threading (as it may cause deadlocks), but may prove to be the only option in some cases.

Added by [@MaciejG604](https://github.com/MaciejG604) in [#2136](https://github.com/VirtusLab/scala-cli/pull/2136)

## Other changes
* Add alias for snapshots repository in Maven by [@lwronski](https://github.com/lwronski) in [#2125](https://github.com/VirtusLab/scala-cli/pull/2125)
* Bump typelevel-toolkit to 0.0.11, configure toolkit-test by [@armanbilge](https://github.com/armanbilge) in [#2135](https://github.com/VirtusLab/scala-cli/pull/2135)
* Fix updating toolkit dependencies by [@Gedochao](https://github.com/Gedochao) in [#2138](https://github.com/VirtusLab/scala-cli/pull/2138)
* Improve directive parsing errors & special-case `toolkit` directive version parsing by [@Gedochao](https://github.com/Gedochao) in [#2133](https://github.com/VirtusLab/scala-cli/pull/2133)
* Fix determining position for value in directive without quotes by [@lwronski](https://github.com/lwronski) in [#2141](https://github.com/VirtusLab/scala-cli/pull/2141)

### Fixes
* Fix line conversion logic by simplifying topWrapperLen to line count of top wrapper by [@MaciejG604](https://github.com/MaciejG604) in [#2101](https://github.com/VirtusLab/scala-cli/pull/2101)
* Fix test watch infinite loop by [@MaciejG604](https://github.com/MaciejG604) in [#2113](https://github.com/VirtusLab/scala-cli/pull/2113)
* Fix flaky completions for `zsh` by [@Jasper-M](https://github.com/Jasper-M) in [#2118](https://github.com/VirtusLab/scala-cli/pull/2118)
* Fix - install certificates for java by [@lwronski](https://github.com/lwronski) in [#2123](https://github.com/VirtusLab/scala-cli/pull/2123)
* Fix the `--source-jar` option & add corresponding `using` directives by [@Gedochao](https://github.com/Gedochao) in [#2120](https://github.com/VirtusLab/scala-cli/pull/2120)

### Documentation changes
* Add docs for bootstrapped standalone fat JAR by [@lwronski](https://github.com/lwronski) in [#2122](https://github.com/VirtusLab/scala-cli/pull/2122)
* Add developer docs on modifying `reflect-config.json` by [@Gedochao](https://github.com/Gedochao) in [#2114](https://github.com/VirtusLab/scala-cli/pull/2114)

### Build and internal changes
* Update release procedure - update also v1 tag by [@lwronski](https://github.com/lwronski) in [#2107](https://github.com/VirtusLab/scala-cli/pull/2107)
* NIT Refactor test scope directives by [@Gedochao](https://github.com/Gedochao) in [#2083](https://github.com/VirtusLab/scala-cli/pull/2083)
* Add main class to jar manifest in assembly by [@romanowski](https://github.com/romanowski) in [#2124](https://github.com/VirtusLab/scala-cli/pull/2124)

### Updates and maintenance
* Update scala-cli.sh launcher for 1.0.0-RC2 by [@github-actions](https://github.com/features/actions) in [#2105](https://github.com/VirtusLab/scala-cli/pull/2105)
* Update org.eclipse.jgit to 6.5.0.202303070854-r by [@scala-steward](https://github.com/scala-steward) in [#2090](https://github.com/VirtusLab/scala-cli/pull/2090)

## New Contributors
* [@Jasper-M](https://github.com/Jasper-M) made their first contribution in [#2118](https://github.com/VirtusLab/scala-cli/pull/2118)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.0.0-RC2...v1.0.0

## [v1.0.0-RC2](https://github.com/VirtusLab/scala-cli/releases/tag/v1.0.0-RC2)

## What's Changed

### Exclude

To exclude specific source files or entire directories from a Scala CLI project, you can now use the `//> using exclude` directive in your `project.scala` file.
Alternatively, you can do the same from the command line with the `--exclude` option.
- absolute path: `/root/path/to/your/project/Main.scala`
- relative path: `src/main/scala/Main.scala`
- glob pattern: `*.sc`

For example, to exclude all files in the `example/scala` directory, add the following directive to your `project.scala` file:
```scala
//> using exclude "example/scala"
```

Added by [@lwronski](https://github.com/lwronski) in [#2053](https://github.com/VirtusLab/scala-cli/pull/2053).

### Directives with a Test Scope equivalent

Some directives now have a test scope equivalent, such as `using dep` and its test scope counterpart `using test.dep`. This allows you to declare dependencies that are only used in tests outside of test-specific sources.

For example, you can declare a dependency on `munit` in your `project.scala` file like this:
```scala
//> using test.dep "org.scalameta::munit::0.7.29"
```
The dependency will only be available in test sources.

Here's a list of directives with a test scope equivalent with example values:

```scala
 //> using test.dep "org.scalameta::munit::0.7.29"
 //> using test.jar "path/to/jar"
 //> using test.javaOpt "-Dfoo=bar"
 //> using test.javacOpt "source", "1.8", "target", "1.8"
 //> using test.javaProp "foo1=bar1"
 //> using test.option "-Xfatal-warnings"
 //> using test.resourceDir "testResources"
 //> using test.toolkit "latest"
```

Added by [@Gedochao](https://github.com/Gedochao) in  [#2046](https://github.com/VirtusLab/scala-cli/pull/2046)

### Changes to using-directives syntax

We've made several updates to simplify the using directives syntax in this release:

- allowed omitting commas in lists of values.
- disallowed multiline comments.
- removed multiline strings.
- removed `require` and `@require` syntax support.
- allowed values without quotes.
- removed `@using`.

For example, the following using directives are now valid without the need for commas and quotes:

```scala
//> using scala 3.2.2
//> using javacOpt -source 1.8 -target 1.8
```

Added by [@tgodzik](https://github.com/tgodzik) in [#2076](https://github.com/VirtusLab/scala-cli/pull/2076)

### Bootstrapped standalone fat JAR.

The Scala CLI launcher is available as a standalone fat JAR. You can download the stable version of the Scala CLI fat JAR from Maven and try it now:

```bash ignore
cs launch org.virtuslab.scala-cli:cliBootstrapped:1.0.0-RC2 -M scala.cli.ScalaCli
```

Added by [@romanowski](https://github.com/romanowski) in [#2005](https://github.com/VirtusLab/scala-cli/pull/2005).

### Access the path of the script being run from its code

With the special `scriptPath` function, you can now easily access the path of the script being run from the script code itself.
Here's an example of how to use the `scriptPath` value:

```scala title=scripts/hello.sc
#!/usr/bin/env -S scala-cli shebang

println(scriptPath)
```

<ChainedSnippets>

```bash
chmod +x scripts/hello.sc
./scripts/hello.sc
```

```text
./scripts/hello.sc
```

</ChainedSnippets>

Added by [@lwronski](https://github.com/lwronski) in [#1990](https://github.com/VirtusLab/scala-cli/pull/1990)

### Explicit Handling of Paths in using-directives

The `${.}` pattern in directive values can now be replaced by the parent directory of the file containing the directive. This makes it possible to generate coverage output files relative to the source file location, for example:

```scala
//> using options "-coverage-out:${.}"
```

Added by [@lwronski](https://github.com/lwronski) in [#2040](https://github.com/VirtusLab/scala-cli/pull/2040)

### Fix deadlocks in Script Wrappers

We have resolved an issue that caused deadlocks when threads were run from the static initializer of the wrapper object 
([#532](https://github.com/VirtusLab/scala-cli/pull/532) and [#1933](https://github.com/VirtusLab/scala-cli/pull/1933)).
Based on the feedback from the community (Thanks [@dacr](https://github.com/dacr)), we found that encapsulating the script code
into a class wrapper fixes the issue. The wrapper is generated by the Scala CLI and is not visible to the user.

This change alters the behavior of scripts that use the `@main` annotation. The `@main` annotation is no longer supported in `.sc` files.

```scala title=script.sc
@main def main(args: String*): Unit = println("Hello")
```

<ChainedSnippets>

```bash run-fail
scala-cli script.sc
```

```text
[warn]  Annotation @main in .sc scripts is not supported, use .scala format instead
Compiling project (Scala 3.2.2, JVM)
[error] ./script.sc:1:1
[error] method main cannot be a main method since it cannot be accessed statically
[error] @main def main(args: String*): Unit = println("Hello")
[error] ^^^^^
Error compiling project (Scala 3.2.2, JVM)
Compilation failed
```

</ChainedSnippets>

Fixed by [@MaciejG604](https://github.com/MaciejG604) in [#2033](https://github.com/VirtusLab/scala-cli/pull/2033)

## Other changes

* Add first-class support for Typelevel and other toolkits by [@armanbilge](https://github.com/armanbilge) in [#2025](https://github.com/VirtusLab/scala-cli/pull/2025)
* Make shebang run not check dependency updates by [@MaciejG604](https://github.com/MaciejG604) in [#2022](https://github.com/VirtusLab/scala-cli/pull/2022)
* Make 'export --json' print to stdout by default by [@MaciejG604](https://github.com/MaciejG604) in [#2008](https://github.com/VirtusLab/scala-cli/pull/2008)
* Don't print the spread directives warning if there's only a single file per scope by [@Gedochao](https://github.com/Gedochao) in [#1988](https://github.com/VirtusLab/scala-cli/pull/1988)
* Add --as-jar option by [@alexarchambault](https://github.com/alexarchambault) in [#2028](https://github.com/VirtusLab/scala-cli/pull/2028)
* add newline to topWrapper by [@bishabosha](https://github.com/bishabosha) in [#1998](https://github.com/VirtusLab/scala-cli/pull/1998)


### Publishing changes
* React to secret key decryption error by [@MaciejG604](https://github.com/MaciejG604) in [#1993](https://github.com/VirtusLab/scala-cli/pull/1993)
* Use ASCII armored secret key by [@MaciejG604](https://github.com/MaciejG604) in [#1991](https://github.com/VirtusLab/scala-cli/pull/1991)
* Properly handle pgp keychains generated by Scala CLI by [@MaciejG604](https://github.com/MaciejG604) in [#1987](https://github.com/VirtusLab/scala-cli/pull/1987)

#### Fixes

* Fix `ExcludeTests` by [@Gedochao](https://github.com/Gedochao) in [#2082](https://github.com/VirtusLab/scala-cli/pull/2082)
* bugfix: Properly show unsupported binary version by [@tgodzik](https://github.com/tgodzik) in [#2081](https://github.com/VirtusLab/scala-cli/pull/2081)
* Allow BSP to start successfully even with unrecognised `using` directives by [@Gedochao](https://github.com/Gedochao) in [#2072](https://github.com/VirtusLab/scala-cli/pull/2072)
* Fix invalid `scala-cli-signing` artifact downloads by [@Gedochao](https://github.com/Gedochao) in [#2054](https://github.com/VirtusLab/scala-cli/pull/2054)
* Fix - package js without main method by [@lwronski](https://github.com/lwronski) in [#2038](https://github.com/VirtusLab/scala-cli/pull/2038)
* Fix completions by [@Gedochao](https://github.com/Gedochao) in [#2004](https://github.com/VirtusLab/scala-cli/pull/2004)
* Fix export failing on input duplicates [@Gedochao](https://github.com/Gedochao) in [#2098](https://github.com/VirtusLab/scala-cli/pull/2098)
* Clean up parsing repositories for publishing [@romanowski](https://github.com/romanowski) in [#2084](https://github.com/VirtusLab/scala-cli/pull/2084)

### Documentation changes
* Docs: Update build output folder in Internal docs by [@amaalali](https://github.com/amaalali) in [#2071](https://github.com/VirtusLab/scala-cli/pull/2071)
* Add docs for test scope directives by [@Gedochao](https://github.com/Gedochao) in [#2058](https://github.com/VirtusLab/scala-cli/pull/2058)
* Improve error messages for malformed `config` values by [@Gedochao](https://github.com/Gedochao) in [#2014](https://github.com/VirtusLab/scala-cli/pull/2014)
* Update export documentation by [@MaciejG604](https://github.com/MaciejG604) in [#2023](https://github.com/VirtusLab/scala-cli/pull/2023)
* Add weaver test framework instruction by [@lenguyenthanh](https://github.com/lenguyenthanh) in [#2021](https://github.com/VirtusLab/scala-cli/pull/2021)

### Build and internal changes
* Download cs for aarch64 from coursier-m1 repo by [@lwronski](https://github.com/lwronski) in [#2085](https://github.com/VirtusLab/scala-cli/pull/2085)
* Pass `invokeData` all the way to pre-processing to give more meaningful error/warning messages by [@Gedochao](https://github.com/Gedochao) in [#2073](https://github.com/VirtusLab/scala-cli/pull/2073)
* Refactor `using` directives processing by [@Gedochao](https://github.com/Gedochao) in [#2066](https://github.com/VirtusLab/scala-cli/pull/2066)
* Remove the `examples` directory to fix `scala-steward` runs by [@Gedochao](https://github.com/Gedochao) in [#2067](https://github.com/VirtusLab/scala-cli/pull/2067)
* Remove some dead code in build by [@alexarchambault](https://github.com/alexarchambault) in [#2069](https://github.com/VirtusLab/scala-cli/pull/2069)
* NIT Remove dead `BuildDeps` by [@Gedochao](https://github.com/Gedochao) in [#2065](https://github.com/VirtusLab/scala-cli/pull/2065)
* Clean up build by [@romanowski](https://github.com/romanowski) in [#2017](https://github.com/VirtusLab/scala-cli/pull/2017)
* Developers reflect 5 active developers in the repo. by [@romanowski](https://github.com/romanowski) in [#2006](https://github.com/VirtusLab/scala-cli/pull/2006)
* Increase maximum memory allocation for JVM by [@lwronski](https://github.com/lwronski) in [#2012](https://github.com/VirtusLab/scala-cli/pull/2012)
* Use bloop-rifle module from scala-cli/bloop-core repo by [@alexarchambault](https://github.com/alexarchambault) in [#1989](https://github.com/VirtusLab/scala-cli/pull/1989)
* Add missing modules for which unit tests are now executed by [@lwronski](https://github.com/lwronski) in [#1992](https://github.com/VirtusLab/scala-cli/pull/1992)
* Remove dead code for ordering PreprocessedSources by [@MaciejG604](https://github.com/MaciejG604) in [#2103](https://github.com/VirtusLab/scala-cli/pull/2103)

### Updates and maintenance
* Downgrade GraalVM to `22.3.1` to fix M1 by [@Gedochao](https://github.com/Gedochao) in [#2099](https://github.com/VirtusLab/scala-cli/pull/2099)
* Update slf4j-nop to 2.0.7 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2095](https://github.com/VirtusLab/scala-cli/pull/2095)
* Update sbt to 1.6.2 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2093](https://github.com/VirtusLab/scala-cli/pull/2093)
* Update bsp4j to 2.1.0-M4 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2086](https://github.com/VirtusLab/scala-cli/pull/2086)
* Bump `coursier` to `2.1.3` by [@Gedochao](https://github.com/Gedochao) in [#2077](https://github.com/VirtusLab/scala-cli/pull/2077)
* Update core_2.13 to 3.8.15 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2087](https://github.com/VirtusLab/scala-cli/pull/2087)
* Update file-tree-views to 2.1.10 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2088](https://github.com/VirtusLab/scala-cli/pull/2088)
* Bump `graalvm` to `22.3.2` by [@Gedochao](https://github.com/Gedochao) in [#2078](https://github.com/VirtusLab/scala-cli/pull/2078)
* Update asm to 9.5 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2092](https://github.com/VirtusLab/scala-cli/pull/2092)
* Bump coursier/setup-action from 1.3.2 to 1.3.3 by [@dependabot](https://docs.github.com/en/code-security/dependabot)  in [#2070](https://github.com/VirtusLab/scala-cli/pull/2070)
* Bump `jsoniter`, `scalameta`, `os-lib` and `scala-collection-compat` by [@Gedochao](https://github.com/Gedochao) in [#2064](https://github.com/VirtusLab/scala-cli/pull/2064)
* Bump `coursier` to `2.1.2` by [@Gedochao](https://github.com/Gedochao) in [#2063](https://github.com/VirtusLab/scala-cli/pull/2063)
* Bump `ammonite` to `2.5.8` by [@Gedochao](https://github.com/Gedochao) in [#2057](https://github.com/VirtusLab/scala-cli/pull/2057)
* Bump Scala.js to `1.13.1` by [@Gedochao](https://github.com/Gedochao) in [#2062](https://github.com/VirtusLab/scala-cli/pull/2062)
* Bump coursier/setup-action from 1.3.1 to 1.3.2 by [@dependabot](https://docs.github.com/en/code-security/dependabot)  in [#2055](https://github.com/VirtusLab/scala-cli/pull/2055)
* Bump coursier/setup-action from 1.3.0 to 1.3.1 by [@dependabot](https://docs.github.com/en/code-security/dependabot)  in [#2042](https://github.com/VirtusLab/scala-cli/pull/2042)
* Dump bloop core to 1.5.6-sc-8 by [@lwronski](https://github.com/lwronski) in [#2013](https://github.com/VirtusLab/scala-cli/pull/2013)
* Fix snapshot versions calculation when the current version ends with `-RC.` by [@Gedochao](https://github.com/Gedochao) in [#2002](https://github.com/VirtusLab/scala-cli/pull/2002)
* Update scala-cli.sh launcher for 1.0.0-RC1 by [@github-actions](https://github.com/features/actions) in [#1995](https://github.com/VirtusLab/scala-cli/pull/1995)
* Update scalafmt-cli_2.13, scalafmt-core to 3.7.3 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#2094](https://github.com/VirtusLab/scala-cli/pull/2094)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v1.0.0-RC1...v1.0.0-RC2

## [v1.0.0-RC1](https://github.com/VirtusLab/scala-cli/releases/tag/v1.0.0-RC1)

### Official `scala` runner release candidate

`v1.0.0-RC1` is the first release candidate version of Scala CLI.

Either this or a future release candidate is meant to become the new official `scala` runner to accompany
the Scala compiler (`scalac`) and other scripts, replacing the old `scala` command. 

To learn more about Scala CLI as the new `scala` runner, check out our recent blogpost: 
https://virtuslab.com/blog/scala-cli-the-new-scala-runner/

### Scala CLI should now have better performance

With a number of newly added performance tweaks, you can expect Scala CLI to run considerably faster.
Added by  [@lwronski](https://github.com/lwronski) in [#1939](https://github.com/VirtusLab/scala-cli/pull/1939)

### Print appropriate warnings when experimental features are used

Using experimental features will now cause Scala CLI to print an appropriate warning.

```bash
scala-cli --power -e '//> using publish.name "my-library"'
# The '//> publish.name "my-library"' directive is an experimental feature.
# Please bear in mind that non-ideal user experience should be expected.
# If you encounter any bugs or have feedback to share, make sure to reach out to the maintenance team at https://github.com/VirtusLab/scala-cli
```

The warning can be suppressed with the `--suppress-experimental-warning` option, or alternatively with the
`suppress-warning.experimental-features` global config key.
```bash
scala-cli config suppress-warning.experimental-features true
```

Added by  [@Gedochao](https://github.com/Gedochao) in [#1920](https://github.com/VirtusLab/scala-cli/pull/1920)

### Experimental and restricted configuration keys will now require to be accessed in `--power` mode

Some configuration keys available with the `config` sub-command have been tagged as experimental or restricted and will 
only be available in `--power` mode.

```bash ignore
scala-cli config httpProxy.address
# The 'httpProxy.address' configuration key is restricted.
# You can run it with the '--power' flag or turn power mode on globally by running:
#   scala-cli config power true
```

Added by  [@Gedochao](https://github.com/Gedochao) in [#1953](https://github.com/VirtusLab/scala-cli/pull/1953)


### Dropped deprecated `using` directive syntax 

The following syntax for `using` directives have been dropped:
- skipping `//>`
- multiline directives
- directives in `/*> ... */` comments
- directives in plain `//` comments
- `@using`

Added by  [@tgodzik](https://github.com/tgodzik) in [#1932](https://github.com/VirtusLab/scala-cli/pull/1932)

### Added support for packaging native images from Docker

It is now possible to package a GraalVM native image with Scala CLI from docker.

```bash ignore
docker run -v $(pwd)/Hello.scala:/Hello.scala virtuslab/scala-cli package --native-image /Hello.scala
```

Added by  [@lwronski](https://github.com/lwronski) in [#1961](https://github.com/VirtusLab/scala-cli/pull/1961)

### Added support for Scala Native's `LTO`
It is now possible to set the Link Time Optimization (LTO) when using Scala CLI with Scala Native.
The available options are "thin", "full" and "none".
You can do it with the `--native-lto` option from the command line:

```bash
scala-cli -e 'println("Hello")' --native --native-lto full
```

Or with a `using` directive:

```scala compile
//> using platform "scala-native"
//> using nativeLto "full"
@main def main(): Unit = println("Hello")
```

Added by [@lwronski](https://github.com/lwronski) in [#1964](https://github.com/VirtusLab/scala-cli/pull/1964)

### Other changes

#### Publishing changes
* Make credential entries respect the --password-value option by [@MaciejG604](https://github.com/MaciejG604)
  in [#1949](https://github.com/VirtusLab/scala-cli/pull/1949)
* Write PGP keys to publish-conf when doing publish setup by [@MaciejG604](https://github.com/MaciejG604)
  in [#1940](https://github.com/VirtusLab/scala-cli/pull/1940)
* Comply with optional password in `scala-cli-signing` by [@MaciejG604](https://github.com/MaciejG604)
  in [#1982](https://github.com/VirtusLab/scala-cli/pull/1982)
* Support ssh in GitHub repo org&name extraction by [@KuceraMartin](https://github.com/KuceraMartin) 
  in [#1938](https://github.com/VirtusLab/scala-cli/pull/1938)

#### Fixes
* Print an informative error if the project workspace path contains `File.pathSeparator` by [@Gedochao](https://github.com/Gedochao)
  in [#1985](https://github.com/VirtusLab/scala-cli/pull/1985)
* Enable to pass custom docker-cmd to execute application in docker by [@lwronski](https://github.com/lwronski) 
  in [#1980](https://github.com/VirtusLab/scala-cli/pull/1980)
* Fix - uses show cli.nativeImage command to generate native image by [@lwronski](https://github.com/lwronski)
  in [#1975](https://github.com/VirtusLab/scala-cli/pull/1975)
* Vcs.parse fix by [@KuceraMartin](https://github.com/KuceraMartin)
  in [#1963](https://github.com/VirtusLab/scala-cli/pull/1963)
* move args definition to the top of the script  by [@bishabosha](https://github.com/bishabosha)
  in [#1983](https://github.com/VirtusLab/scala-cli/pull/1983)

#### Documentation changes

* Back port of documentation changes to main by [@github-actions](https://github.com/features/actions) 
  in [#1935](https://github.com/VirtusLab/scala-cli/pull/1935)
* Remove ChainedSnippets by  [@MaciejG604](https://github.com/MaciejG604) 
  in [#1928](https://github.com/VirtusLab/scala-cli/pull/1928)
* Further document publish command by  [@MaciejG604](https://github.com/MaciejG604) 
  in [#1914](https://github.com/VirtusLab/scala-cli/pull/1914)
* Add a verbosity guide by  [@Gedochao](https://github.com/Gedochao) 
  in [#1936](https://github.com/VirtusLab/scala-cli/pull/1936)
* Docs - how to run unit tests in Scala CLI by [@lwronski](https://github.com/lwronski)
  in [#1977](https://github.com/VirtusLab/scala-cli/pull/1977)

#### Build and internal changes

* Use locally build jvm launcher of scala-cli in gifs generator by  [@lwronski](https://github.com/lwronski)
  in [#1921](https://github.com/VirtusLab/scala-cli/pull/1921)
* Clean up after ammonite imports removal by  [@MaciejG604](https://github.com/MaciejG604) 
  in [#1934](https://github.com/VirtusLab/scala-cli/pull/1934)
* Temporarily disable `PublishTests.secret keys in config` on Windows by  [@Gedochao](https://github.com/Gedochao)
  in [#1948](https://github.com/VirtusLab/scala-cli/pull/1948)
* Move toolkit to scalalang org by [@szymon-rd](https://github.com/szymon-rd) 
  in [#1930](https://github.com/VirtusLab/scala-cli/pull/1930)

#### Updates and maintenance

* Update scala-cli.sh launcher for 0.2.1 by [@github-actions](https://github.com/features/actions) 
  in [#1931](https://github.com/VirtusLab/scala-cli/pull/1931)
* Bump VirtusLab/scala-cli-setup from 0.2.0 to 0.2.1 by [@dependabot](https://docs.github.com/en/code-security/dependabot) 
  in [#1947](https://github.com/VirtusLab/scala-cli/pull/1947)
* Bump coursier/publish version to 0.1.4 by  [@MaciejG604](https://github.com/MaciejG604) 
  in [#1950](https://github.com/VirtusLab/scala-cli/pull/1950)
* Bump to the latest weaver & remove expecty by [@lenguyenthanh](https://github.com/lenguyenthanh) 
  in [#1955](https://github.com/VirtusLab/scala-cli/pull/1955)
* Bump webfactory/ssh-agent from 0.7.0 to 0.8.0 by [@dependabot](https://docs.github.com/en/code-security/dependabot) 
  in [#1967](https://github.com/VirtusLab/scala-cli/pull/1967)
* chore(dep): bump mill from 0.10.10 to 0.10.12 by [@ckipp01](https://github.com/ckipp01) 
  in [#1970](https://github.com/VirtusLab/scala-cli/pull/1970)
* Bump Bleep to `1.5.6-sc-4`by [@Gedochao](https://github.com/Gedochao) 
  in [#1973](https://github.com/VirtusLab/scala-cli/pull/1973)

### New Contributors

* [@KuceraMartin](https://github.com/KuceraMartin) made their first contribution 
  in [#1938](https://github.com/VirtusLab/scala-cli/pull/1938)
* [@lenguyenthanh](https://github.com/lenguyenthanh) made their first contribution 
  in [#1955](https://github.com/VirtusLab/scala-cli/pull/1955)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v0.2.1...v1.0.0-RC1

## [v0.2.1](https://github.com/VirtusLab/scala-cli/releases/tag/v0.2.1)

### Add a guide for migrating from the old `scala` runner to Scala CLI

As of [SIP-46](https://github.com/scala/improvement-proposals/pull/46), Scala CLI has been accepted as the new `scala`
command. To make the transition smooth we added a [guide](docs/guides/introduction/old-runner-migration.md) highlighting
the differences between the two runners.

Added by [@Gedochao](https://github.com/Gedochao) in [#1900](https://github.com/VirtusLab/scala-cli/pull/1900)

### Improve the `publish` and `publish setup` sub-commands' user experience

We're currently focusing on improving the experimental `publish` feature of Scala CLI and making `publish setup` + `publish`
more stable and user-friendly.

Using pgp keys created by `config --create-pgp-key` subcommand is now supported as a default option,
no additional user input is needed.

Addressed by [@alexarchambault](https://github.com/alexarchambault) in [#1432](https://github.com/VirtusLab/scala-cli/pull/1432)
and by [@MaciejG604](https://github.com/MaciejG604) in [#1898](https://github.com/VirtusLab/scala-cli/pull/1898)

### Remove unsupported kebab-case style in using directives

All using directives names are now using camelCase, kebab-case is no longer available.

Added by [@lwronski](https://github.com/lwronski) in [#1878](https://github.com/VirtusLab/scala-cli/pull/1878)

### Add a reference for available config keys in help & docs

You can now view the available config keys using `config --help`:
```bash
scala-cli config -h
# Usage: scala-cli config [options]
# Configure global settings for Scala CLI.
# 
# Available keys:
#   actions                                        Globally enables actionable diagnostics. Enabled by default.
#   interactive                                    Globally enables interactive mode (the '--interactive' flag).
#   power                                          Globally enables power mode (the '--power' launcher flag).
#   suppress-warning.directives-in-multiple-files  Globally suppresses warnings about directives declared in multiple source files.
#   suppress-warning.outdated-dependencies-files   Globally suppresses warnings about outdated dependencies.
# 
# You are currently viewing the basic help for the config sub-command. You can view the full help by running: 
#    scala-cli config --help-full
# For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/misc/config
# 
# Config options:
#   --unset, --remove  Remove an entry from config
```
Also, `config --full-help` will show the list of all keys.

Added by [@Gedochao](https://github.com/Gedochao) in [#1910](https://github.com/VirtusLab/scala-cli/pull/1910)

### Pass user arguments to JS runner

It's now possible to pass user arguments to a JS application:

```scala title=ScalaJsArgs.sc
import scala.scalajs.js
import scala.scalajs.js.Dynamic.global

val process = global.require("process")
val argv = Option(process.argv)
  .filterNot(js.isUndefined)
  .map(_.asInstanceOf[js.Array[String]].drop(2).toSeq)
  .getOrElse(Nil)
val console = global.console
console.log(argv.mkString(" "))
```

```bash
scala-cli ScalaJsArgs.sc --js -- Hello World
```
```text
Hello World
```

Added by [@alexarchambault](https://github.com/alexarchambault) in [#1826](https://github.com/VirtusLab/scala-cli/pull/1826)


### Other changes
* Tweak error messages for running scripts without file extensions by [@Gedochao](https://github.com/Gedochao) in [#1886](https://github.com/VirtusLab/scala-cli/pull/1886)
* Exit with Bloop command return code if it's non-zero by [@alexarchambault](https://github.com/alexarchambault) in [#1837](https://github.com/VirtusLab/scala-cli/pull/1837)
* bloop-rifle: increase timeout values by [@Flowdalic](https://github.com/Flowdalic) in [#1865](https://github.com/VirtusLab/scala-cli/pull/1865)
* Suggest users to clean working directory when Nailgun server failed by [@lwronski](https://github.com/lwronski) in [#1916](https://github.com/VirtusLab/scala-cli/pull/1916)
* fix: encode videos in yuv420p to support Firefox by [@danielleontiev](https://github.com/danielleontiev) in [#1904](https://github.com/VirtusLab/scala-cli/pull/1904)
* Fix reading passwords from commands by [@alexarchambault](https://github.com/alexarchambault) in [#1775](https://github.com/VirtusLab/scala-cli/pull/1775)
* Add extra class path to generated bootstrap launcher by [@lwronski](https://github.com/lwronski) in [#1897](https://github.com/VirtusLab/scala-cli/pull/1897)


#### SIP-related changes
* Add 'dependency' and 'dependencies' alias for using directive by [@MaciejG604](https://github.com/MaciejG604) in [#1903](https://github.com/VirtusLab/scala-cli/pull/1903)


#### Documentation updates
* Ensure no console-syntax in reference docs and no `md` fenced blocks in `--help` by [@Gedochao](https://github.com/Gedochao) in [#1874](https://github.com/VirtusLab/scala-cli/pull/1874)
* Document export subcommand by [@MaciejG604](https://github.com/MaciejG604) in [#1875](https://github.com/VirtusLab/scala-cli/pull/1875)
* Tweak guides' and cookbooks' pages by [@Gedochao](https://github.com/Gedochao) in [#1894](https://github.com/VirtusLab/scala-cli/pull/1894)
* Fix pgp creation option name by [@MaciejG604](https://github.com/MaciejG604) in [#1909](https://github.com/VirtusLab/scala-cli/pull/1909)
* Fix using directive docs by [@lwronski](https://github.com/lwronski) in [#1901](https://github.com/VirtusLab/scala-cli/pull/1901)
* Add docs to classifiers and exclude dependency by [@lwronski](https://github.com/lwronski) in [#1892](https://github.com/VirtusLab/scala-cli/pull/1892)


#### Internal changes
* Fix handling for `experimental` features by [@Gedochao](https://github.com/Gedochao) in [#1915](https://github.com/VirtusLab/scala-cli/pull/1915)
* Change default home directory for tests integration and docs-test modules to avoid overriding global user config by [@lwronski](https://github.com/lwronski) in [#1917](https://github.com/VirtusLab/scala-cli/pull/1917)
* NIT Use enums for help groups and help command groups by [@Gedochao](https://github.com/Gedochao) in [#1880](https://github.com/VirtusLab/scala-cli/pull/1880)


#### Updates & maintenance
* Bump dns-packet from 5.3.1 to 5.4.0 in /website by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#1906](https://github.com/VirtusLab/scala-cli/pull/1906)
* Bump VirtusLab/scala-cli-setup from 0.1.20 to 0.2.0 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#1890](https://github.com/VirtusLab/scala-cli/pull/1890)
* Dump docusaurus to 2.3.1 and other docs deps by [@lwronski](https://github.com/lwronski) in [#1907](https://github.com/VirtusLab/scala-cli/pull/1907)
* Update scala-cli.sh launcher for 0.2.0 by [@github-actions](https://github.com/features/actions) in [#1881](https://github.com/VirtusLab/scala-cli/pull/1881)
* Back port of documentation changes to main by [@github-actions](https://github.com/features/actions) in [#1911](https://github.com/VirtusLab/scala-cli/pull/1911)


## New Contributors
* [@danielleontiev](https://github.com/danielleontiev) made their first contribution in [#1904](https://github.com/VirtusLab/scala-cli/pull/1904)

## [v0.2.0](https://github.com/VirtusLab/scala-cli/releases/tag/v0.2.0)

### Require the `--power` option for restricted features by default

Until now, Scala CLI has been limiting some of its functionalities in its `scala` distribution.
Starting with `v0.2.0`, those limitation will be applied to all distributions, including `scala-cli`.

This was done in order to make the behaviour consistent with Scala CLI acting as the Scala runner.

Restricted features can be accessed by using the `--power` launcher flag. Do note that launcher flags have to be passed **before** the sub-command.
```bash ignore
scala-cli --power package .
```
Alternatively, the `power` mode can be turned on globally by running:

```bash ignore
scala-cli config power true 
```

Please note that this change may affect your existing scripts or workflows that rely on the limited commands from ScalaCLI (such as `package`, `publish`). You can still use those commands with `power` mode enabled.

When you try to use a limited command in restricted mode, you will now see a warning message with suggestions on how to enable this command:


```bash ignore
$ scala-cli package Hello.scala
# This command is restricted and requires setting the `--power` option to be used.
# You can pass it explicitly or set it globally by running:
#    scala-cli config power true
$ scala-cli config power true
$ scala-cli package Hello.scala
# Wrote Hello, run it with
#   ./Hello
```

Added by [@lwronski](https://github.com/lwronski) in [#1835](https://github.com/VirtusLab/scala-cli/pull/1835) and [#1849](https://github.com/VirtusLab/scala-cli/pull/1849)

### Allow executable Scala scripts without a file extension

As of this release Scala scripts without the `*.sc` file extension will be supported for execution when using the `shebang` command.

```scala title=hello
#!/usr/bin/env -S scala-cli shebang -S 3

println(args.size)
println(args.headOption)
```
```bash
chmod +x hello
./hello Hello World
#2
#Some(Hello)
 ```
Note that files with no extension are always run as scripts even though they may contain e.g. a valid `.scala` program.

Also, do note that this feature has only been added for `shebang` - the `run` sub-command (which is the default way of running inputs when a sub-command is not specified explicitly) will not support this.

Added by [@MaciejG604](https://github.com/MaciejG604) in [#1802](https://github.com/VirtusLab/scala-cli/pull/1802)

### Export Project configuration to Json

It is now possible to export configuration from Scala CLI project to Json format with the `export` sub-command.

```bash ignore
scala-cli --power export --json .
```

It is currently exporting basic information about the project and includes, for example, the following fields:
 
- ScalaVersion 
- Platform
- Sources
- Dependencies
- Resolvers


Example of generated Json output:
```json
{
  "scalaVersion": "3.2.2",
  "platform": "JVM",
  "scopes": {
    "main": {
      "sources": [
        "Hello.scala"
      ],
      "dependencies": [
        {
          "groupId": "com.lihaoyi",
          "artifactId": {
            "name": "pprint",
            "fullName": "pprint_3"
          },
          "version": "0.6.6"
        }
      ],
      ...
    }
  }
}
```

Added by [@MaciejG604](https://github.com/MaciejG604) in [#1840](https://github.com/VirtusLab/scala-cli/pull/1840)

### Rename `using lib` to `using dep`

To be more consistent with dependency command line options `--dep`, the dependency using directive is now passed by `using dep`.
Please note that we have kept the alias of the old directive (`lib`, `libs`) for backwards compatibility.

```scala compile
 //> using dep "org.scalameta::munit:0.7.29"
```
Renamed by [@lwronski](https://github.com/lwronski) in [#1827](https://github.com/VirtusLab/scala-cli/pull/1827)

### Other breaking changes

#### Remove ammonite imports support
The support for `$ivy` and `$dep` ammonite imports has been removed. 
To easily convert existing `$ivy` and `$dep` imports into the `using dep` directive in your sources, you can use the provided actionable diagnostic.

![convert_ivy_to_using_dep](/img/gifs/convert_ivy_to_using.gif)

Removed by [@MaciejG604](https://github.com/MaciejG604) in [#1787](https://github.com/VirtusLab/scala-cli/pull/1787)

#### Drop the `metabrowse` sub-command

With this release, support for Metabrowse has been removed from Scala CLI. This change was made in order to limit the number of features that we need to support, especially since the `Metabrowse` project is no longer being actively worked on.

Remove by [@lwronski](https://github.com/lwronski) in [#1867](https://github.com/VirtusLab/scala-cli/pull/1867)

### Other changes

* Add cross-platform toolkit dependency by [@bishabosha](https://github.com/bishabosha) in [#1810](https://github.com/VirtusLab/scala-cli/pull/1810)
* Show explain message when is enabled by [@lwronski](https://github.com/lwronski) in [#1830](https://github.com/VirtusLab/scala-cli/pull/1830)
* Read home directory from env variable instead of option from command line by [@lwronski](https://github.com/lwronski) in [#1842](https://github.com/VirtusLab/scala-cli/pull/1842)
* Add build/taskStart and taskFinish to the exception reporting BSP mechanism by [@MaciejG604](https://github.com/MaciejG604) in [#1821](https://github.com/VirtusLab/scala-cli/pull/1821)
* blooprifle: report exit code in exception by [@Flowdalic](https://github.com/flowdalic) in [#1844](https://github.com/VirtusLab/scala-cli/pull/1844)
* Suppress lib update warning by [@MaciejG604](https://github.com/MaciejG604) in [#1848](https://github.com/VirtusLab/scala-cli/pull/1848)
* Invalid subcommand arg by [@MaciejG604](https://github.com/MaciejG604) in [#1811](https://github.com/VirtusLab/scala-cli/pull/1811)


#### SIP-related changes
* Add a warning for the `-run` option of the legacy `scala` runner, instead of failing by [@Gedochao](https://github.com/Gedochao) in [#1801](https://github.com/VirtusLab/scala-cli/pull/1801)
* Add warnings for the deprecated `-Yscriptrunner` legacy `scala` runner option instead of passing it to `scalac` by [@Gedochao](https://github.com/Gedochao) in [#1804](https://github.com/VirtusLab/scala-cli/pull/1804)
* Filter out `restricted` & `experimental` options from `SIP` mode help by [@Gedochao](https://github.com/Gedochao) in [#1812](https://github.com/VirtusLab/scala-cli/pull/1812)
* Warn in sip mode when using restricted command by [@lwronski](https://github.com/lwronski) in [#1862](https://github.com/VirtusLab/scala-cli/pull/1862)
* Add more detail for sub-commands' help messages by [@Gedochao](https://github.com/Gedochao) in [#1852](https://github.com/VirtusLab/scala-cli/pull/1852)
* Fix printing not supported option in restricted mode by [@lwronski](https://github.com/lwronski) in [#1861](https://github.com/VirtusLab/scala-cli/pull/1861)
* Shorter options help by [@Gedochao](https://github.com/Gedochao) in [#1872](https://github.com/VirtusLab/scala-cli/pull/1872)

#### Fixes

* Fix warning about using directives in multiple files when two java files are present by [@MaciejG604](https://github.com/MaciejG604) in [#1796](https://github.com/VirtusLab/scala-cli/pull/1796)
* Quit flag not suppresses compilation errors by [@lwronski](https://github.com/lwronski) in [#1792](https://github.com/VirtusLab/scala-cli/pull/1792)
* Dont warn about target directives by [@MaciejG604](https://github.com/MaciejG604) in [#1803](https://github.com/VirtusLab/scala-cli/pull/1803)
* Fix - actionable actions not suggest update to previous version by [@lwronski](https://github.com/lwronski) in [#1813](https://github.com/VirtusLab/scala-cli/pull/1813)
* Fix actionable action when uses latest sytanx version in lib by [@lwronski](https://github.com/lwronski) in [#1817](https://github.com/VirtusLab/scala-cli/pull/1817)
* Prevent NPE from being thrown by the `export` sub-command if `testFramework` isn't defined by [@Gedochao](https://github.com/Gedochao) in [#1814](https://github.com/VirtusLab/scala-cli/pull/1814)
* Fix message checking in test by [@MaciejG604](https://github.com/MaciejG604) in [#1847](https://github.com/VirtusLab/scala-cli/pull/1847)
* blooprifle: add -XX:+IgnoreUnrecognizedVMOptions to hardCodedDefaultJavaOpts by [@Flowdalic](https://github.com/flowdalic) in [#1845](https://github.com/VirtusLab/scala-cli/pull/1845)
* Trim passwords obtained as command result by [@MaciejG604](https://github.com/MaciejG604) in [#1871](https://github.com/VirtusLab/scala-cli/pull/1871)

#### Build and internal changes
* Ignore Bloop server early exit if it signals an already running server by [@alexarchambault](https://github.com/alexarchambault) in [#1799](https://github.com/VirtusLab/scala-cli/pull/1799)
* Build aarch64 linux launcher using m1 by [@lwronski](https://github.com/lwronski) in [#1805](https://github.com/VirtusLab/scala-cli/pull/1805)
* Remove latest supported scala version mechanism by [@lwronski](https://github.com/lwronski) in [#1816](https://github.com/VirtusLab/scala-cli/pull/1816)
* Switch `scala-cli-signing` to `org.virtuslab` and bump to `0.1.15` by [@Gedochao](https://github.com/Gedochao) in [#1853](https://github.com/VirtusLab/scala-cli/pull/1853)
* Add clang to scala-cli docker image by [@lwronski](https://github.com/lwronski) in [#1846](https://github.com/VirtusLab/scala-cli/pull/1846)
* bloop-file: show timeout value in error message by [@Flowdalic](https://github.com/flowdalic) in [#1855](https://github.com/VirtusLab/scala-cli/pull/1855)
* Back port of documentation changes to main by [@github-actions](https://github.com/features/actions) in [#1860](https://github.com/VirtusLab/scala-cli/pull/1860)
* Run generate reference doc as non sip by [@lwronski](https://github.com/lwronski) in [#1866](https://github.com/VirtusLab/scala-cli/pull/1866)
* Bump `case-app` to `2.1.0-M23` by [@lwronski](https://github.com/lwronski) in [#1868](https://github.com/VirtusLab/scala-cli/pull/1868)

#### Documentation updates
* Update docker example command by [@MaciejG604](https://github.com/MaciejG604) in [#1798](https://github.com/VirtusLab/scala-cli/pull/1798)
* Tweak `--watch`/`--restart` disambiguation in the help messages & docs by [@Gedochao](https://github.com/Gedochao) in [#1819](https://github.com/VirtusLab/scala-cli/pull/1819)
* Release notes - msi malware analysis by [@lwronski](https://github.com/lwronski) in [#1832](https://github.com/VirtusLab/scala-cli/pull/1832)
* Improve 'shebang' help message wrt program *arguments* by [@Flowdalic](https://github.com/flowdalic) in [#1829](https://github.com/VirtusLab/scala-cli/pull/1829)
* docs: Fix Yum manual installation step by [@tgodzik](https://github.com/tgodzik) in [#1850](https://github.com/VirtusLab/scala-cli/pull/1850)

#### Updates & maintenance
* Update scala-cli.sh launcher for 0.1.20 by [@github-actions](https://github.com/features/actions) in [#1790](https://github.com/VirtusLab/scala-cli/pull/1790)
* Bump VirtusLab/scala-cli-setup from 0.1.19 to 0.1.20 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#1806](https://github.com/VirtusLab/scala-cli/pull/1806)

## New Contributors
* [@Flowdalic](https://github.com/flowdalic) made their first contribution in [#1829](https://github.com/VirtusLab/scala-cli/pull/1829)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v0.1.20...v0.2.0

## [v0.1.20](https://github.com/VirtusLab/scala-cli/releases/tag/v0.1.20)

### Add support for Scala Toolkit
Scala CLI now has support for [Scala Toolkit](https://virtuslab.com/blog/scala-toolkit-makes-scala-powerful-straight-out-of-the-box/).

Scala Toolkit is an ongoing effort by [Scala Center](https://scala.epfl.ch/) and [VirtusLab](https://www.virtuslab.com/) 
to compose a set of approachable libraries to solve everyday problems.

It is currently in its pre-release phase and includes the following libraries:
- [MUnit](https://github.com/scalameta/munit) for testing;
- [Sttp](https://github.com/softwaremill/sttp) for HTTP client;
- [UPickle/UJson](https://github.com/com-lihaoyi/upickle) for reading, writing and operating on JSONs;
- [OS-Lib](https://github.com/com-lihaoyi/os-lib) for operating on files and the operating system.

You can add it to your Scala CLI build from the command line with the `--with-toolkit` option.

```bash ignore
scala-cli . --with-toolkit latest
```

There's also an appropriate `using` directive.

```scala compile
//> using toolkit "0.1.6"
```

Added by [@lwronski](https://github.com/lwronski) in [#1768](https://github.com/VirtusLab/scala-cli/pull/1768)

### Scala CLI is built with Scala `3.2.2`
We now rely on Scala `3.2.2` as the default internal Scala version used to build the project.

Added by [@lwronski](https://github.com/lwronski) and [@Gedochao](https://github.com/Gedochao) in [#1772](https://github.com/VirtusLab/scala-cli/pull/1772)

### Removal of the `about` and `doctor` sub-commands
The `about` command has been removed, its features merged back to the `version` command.
As a result, the `version` command will now check if your locally installed Scala CLI is up-to-date.
It is possible to skip the check with the `--offline` option, or when printing raw CLI or default Scala
versions with `--cli-version` and `--scala-version`, respectively.

```bash
scala-cli version --offline                     
# Scala CLI version: 0.1.20
# Scala version (default): 3.2.2
```

Similarly, the `doctor` sub-command has been removed, with its past and previously planned functionalities to be delivered
in a more interactive manner in the future.

Added by [@Gedochao](https://github.com/Gedochao) in [#1744](https://github.com/VirtusLab/scala-cli/pull/1744)

### The Scala CLI `aarch64/arm64` binary is now available via `sdkman`
You can now get the platform-appropriate Scala CLI binary on `aarch64/arm64` architecture via `sdkman`.

Added by [@mkurz](https://github.com/mkurz) in [#1748](https://github.com/VirtusLab/scala-cli/pull/1748)

### `aarch64/arm64` artifact with the launcher script
The `scala-cli.sh` launcher script now correctly downloads the `aarch64/arm64` artifact on the appropriate architecture.

Added by [@mkurz](https://github.com/mkurz) in [#1745](https://github.com/VirtusLab/scala-cli/pull/1745)

### Run a `.jar` file as a regular input
JARs can now be run just like any other input, without the need of passing the `-cp` option.
```bash ignore
scala-cli Hello.jar
# Hello
```
Added by [@lwronski](https://github.com/lwronski) in [#1738](https://github.com/VirtusLab/scala-cli/pull/1738)

### Java properties without the need for `--java-prop`
The `--java-prop` option can be skipped when passing Java properties to Scala CLI now.
```bash ignore
scala-cli Hello.scala -Dfoo=bar
```
Added by [@lwronski](https://github.com/lwronski) in [#1739](https://github.com/VirtusLab/scala-cli/pull/1739)

### Docker packaging with `using` directives
It is now possible to configure packaging into a docker image via `using` directives.
```scala compile power
//> using packaging.dockerFrom "openjdk:11"
//> using packaging.dockerImageTag "1.0.0"
//> using packaging.dockerImageRegistry "virtuslab"
//> using packaging.dockerImageRepository "scala-cli"
```
Added by [@lwronski](https://github.com/lwronski) in [#1753](https://github.com/VirtusLab/scala-cli/pull/1753)

### Pass GraalVM args via a `using` directive
It is now possible to pass args to GraalVM via the following `using` directive:
```scala compile power
//> using packaging.graalvmArgs "--no-fallback", "--enable-url-protocols=http,https"
```

Added by [@lwronski](https://github.com/lwronski) in [#1767](https://github.com/VirtusLab/scala-cli/pull/1767)

### Other changes

#### SIP-related changes
* Remove irrelevant options from `version` help message by [@lwronski](https://github.com/lwronski) in [#1737](https://github.com/VirtusLab/scala-cli/pull/1737)
* Include launcher options in the help for the default and `help` sub-commands by [@Gedochao](https://github.com/Gedochao) in [#1725](https://github.com/VirtusLab/scala-cli/pull/1725)
* Remove suffix `.aux` from progName when installed by cs by [@lwronski](https://github.com/lwronski) in [#1736](https://github.com/VirtusLab/scala-cli/pull/1736)
* Don't fail in case of connection errors in the version sub-command by [@Gedochao](https://github.com/Gedochao) in [#1760](https://github.com/VirtusLab/scala-cli/pull/1760)
* Set workspace dir to `os.tmp.dir` for virtual sources by [@lwronski](https://github.com/lwronski) in [#1771](https://github.com/VirtusLab/scala-cli/pull/1771)
* Add support for deprecated Scala `2.13.x`-specific `scala` runner options by [@Gedochao](https://github.com/Gedochao) in [#1774](https://github.com/VirtusLab/scala-cli/pull/1774)
* Add support for the `-with-compiler` runner option by [@Gedochao](https://github.com/Gedochao) in [#1780](https://github.com/VirtusLab/scala-cli/pull/1780)

#### Fixes
* Take into account interactively picked options when caching binaries by [@alexarchambault](https://github.com/alexarchambault) in [#1701](https://github.com/VirtusLab/scala-cli/pull/1701)
* Erase things in working dir in publish by [@alexarchambault](https://github.com/alexarchambault) in [#1715](https://github.com/VirtusLab/scala-cli/pull/1715)
* Improve formatting of generated Mill project by [@lolgab](https://github.com/lolgab) in [#1677](https://github.com/VirtusLab/scala-cli/pull/1677)
* Restart Bloop server if it exited by [@alexarchambault](https://github.com/alexarchambault) in [#1716](https://github.com/VirtusLab/scala-cli/pull/1716)
* Add a global configuration for suppressing the warning about directives in multiple files by [@MaciejG604](https://github.com/MaciejG604) in [#1779](https://github.com/VirtusLab/scala-cli/pull/1779)
* Add CLI option for suppressing the warning about directives in multiple files by [@MaciejG604](https://github.com/MaciejG604) in [#1754](https://github.com/VirtusLab/scala-cli/pull/1754)
* Set page size for aarch64 Linux binaries to 64k by [@mkurz](https://github.com/mkurz) in [#1726](https://github.com/VirtusLab/scala-cli/pull/1726)

#### Build and internal changes
* Tweaking by [@alexarchambault](https://github.com/alexarchambault) in [#1711](https://github.com/VirtusLab/scala-cli/pull/1711)
* Address some native-image warnings by [@alexarchambault](https://github.com/alexarchambault) in [#1719](https://github.com/VirtusLab/scala-cli/pull/1719)
* Do not generate Linux aarch64 binaries from PRs by [@alexarchambault](https://github.com/alexarchambault) in [#1720](https://github.com/VirtusLab/scala-cli/pull/1720)
* Derive using directives handlers from case classes by [@alexarchambault](https://github.com/alexarchambault) in [#1637](https://github.com/VirtusLab/scala-cli/pull/1637)
* Don't run commands upon HelpTests instantiation by [@alexarchambault](https://github.com/alexarchambault) in [#1762](https://github.com/VirtusLab/scala-cli/pull/1762)

#### Documentation updates
* Add test-only cookbook by [@lwronski](https://github.com/lwronski) in [#1718](https://github.com/VirtusLab/scala-cli/pull/1718)
* Fixing minor typos and some wordsmithing. by [@medale](https://github.com/medale) in [#1731](https://github.com/VirtusLab/scala-cli/pull/1731)
* Back port of documentation changes to main by [@github-actions](https://github.com/features/actions) in [#1735](https://github.com/VirtusLab/scala-cli/pull/1735)
* Explain the differences in using shebang vs scala-cli directly in script by [@lwronski](https://github.com/lwronski) in [#1740](https://github.com/VirtusLab/scala-cli/pull/1740)
* Add instruction for Intellij JVM version by [@MaciejG604](https://github.com/MaciejG604) in [#1773](https://github.com/VirtusLab/scala-cli/pull/1773)
* Fix a broken link by [@xerial](https://github.com/xerial) and [@lwronski](https://github.com/lwronski) in [#1777](https://github.com/VirtusLab/scala-cli/pull/1777)

#### Updates & maintenance
* Update svm to 22.3.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1689](https://github.com/VirtusLab/scala-cli/pull/1689)
* Update scala-cli.sh launcher for 0.1.19 by [@github-actions](https://github.com/features/actions) in [#1707](https://github.com/VirtusLab/scala-cli/pull/1707)
* Bump VirtusLab/scala-cli-setup from 0.1.18 to 0.1.19 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#1709](https://github.com/VirtusLab/scala-cli/pull/1709)
* Update Bloop to 1.5.6-sc-1 by [@lwronski](https://github.com/lwronski) in [#1704](https://github.com/VirtusLab/scala-cli/pull/1704)
* Update trees_2.13 to 4.7.1 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1717](https://github.com/VirtusLab/scala-cli/pull/1717)
* Update coursier-jvm_2.13, ... to 2.1.0-RC4 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1723](https://github.com/VirtusLab/scala-cli/pull/1723)
* Bump uraimo/run-on-arch-action from 2.3.0 to 2.5.0 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#1734](https://github.com/VirtusLab/scala-cli/pull/1734)
* Update jsoniter-scala-core_2.13, ... to 2.20.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1732](https://github.com/VirtusLab/scala-cli/pull/1732)
* Update jsoniter-scala-core_2.13, ... to 2.20.1 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1741](https://github.com/VirtusLab/scala-cli/pull/1741)
* Update scalafmt-cli_2.13, scalafmt-core to 3.6.1 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1742](https://github.com/VirtusLab/scala-cli/pull/1742)
* Update core_2.13 to 3.8.6 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1746](https://github.com/VirtusLab/scala-cli/pull/1746)
* Update libdaemon to 0.0.11 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1759](https://github.com/VirtusLab/scala-cli/pull/1759)
* Update jsoniter-scala-core_2.13, ... to 2.20.2 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1757](https://github.com/VirtusLab/scala-cli/pull/1757)
* Update core_2.13 to 3.8.7 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1758](https://github.com/VirtusLab/scala-cli/pull/1758)
* Update bloop core to 1.5.6-sc-2 by [@lwronski](https://github.com/lwronski) in [#1761](https://github.com/VirtusLab/scala-cli/pull/1761)
* Update core_2.13 to 3.8.8 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1770](https://github.com/VirtusLab/scala-cli/pull/1770)
* Update ammonite to 2.5.6 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1769](https://github.com/VirtusLab/scala-cli/pull/1769)
* Update jsoniter-scala-core_2.13, ... to 2.20.3 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1776](https://github.com/VirtusLab/scala-cli/pull/1776)
* Update amm to 2.5.6-1-f8bff243 by [@lwronski](https://github.com/lwronski) in [#1778](https://github.com/VirtusLab/scala-cli/pull/1778)

### New Contributors
* [@mkurz](https://github.com/mkurz) made their first contribution in [#1726](https://github.com/VirtusLab/scala-cli/pull/1726)
* [@medale](https://github.com/medale) made their first contribution in [#1731](https://github.com/VirtusLab/scala-cli/pull/1731)
* [@MaciejG604](https://github.com/MaciejG604) made their first contribution in [#1773](https://github.com/VirtusLab/scala-cli/pull/1773)
* [@xerial](https://github.com/xerial) made their first contribution in [#1777](https://github.com/VirtusLab/scala-cli/pull/1777)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v0.1.19...v0.1.20

## [v0.1.19](https://github.com/VirtusLab/scala-cli/releases/tag/v0.1.19)

### The Linux `aarch64` native launcher is here! (experimental)

We are happy to announce that there is a new dedicated launcher for the Linux Aarch64. You can find it [here](https://github.com/VirtusLab/scala-cli/releases/download/v0.1.19/scala-cli-aarch64-pc-linux.gz).

Added in [#1703](https://github.com/VirtusLab/scala-cli/pull/1703) by [@lwronski](https://github.com/lwronski)

### Fix `workspace/reload` for Intellij IDEA

Dependencies (and other configurations) from `using` directives should now always be picked up after a BSP project reload.

<ReactPlayer playing controls url='https://user-images.githubusercontent.com/18601388/207319736-534f2d8a-862d-4c0a-8c8a-e52d95ac03e6.mov' />

Fixed by [@Gedochao](https://github.com/Gedochao) in [#1681](https://github.com/VirtusLab/scala-cli/pull/1681).

###  `shebang` headers in Markdown

The `shebang` headers in `scala` code blocks inside a markdown input are always ignored.

````markdown
# Scala with `shebang`
A sample code block with the `shebang` header.
```scala
#!/usr/bin/env -S scala-cli shebang
println("Hello world")
```
````

Added by [@Gedochao](https://github.com/Gedochao) in [#1647](https://github.com/VirtusLab/scala-cli/pull/1647)

### Export Scala compiler plugins to Mill projects
It is now possible to export `scalac` compiler plugins from a Scala CLI project to Mill with the `export` sub-command.

Added by [@carlosedp](https://github.com/carlosedp) in [#1626](https://github.com/VirtusLab/scala-cli/pull/1626)

### Other changes

#### SIP Changes
* Fix the order of help command groups for the default help by [@Gedochao](https://github.com/Gedochao) in [#1697](https://github.com/VirtusLab/scala-cli/pull/1697)
* Adjust SIP help output & ensure `ScalaSipTests` are run on Windows by [@Gedochao](https://github.com/Gedochao) in [#1695](https://github.com/VirtusLab/scala-cli/pull/1695)
* Add warnings for `-save` & `-nosave` legacy `scala` runner options instead of failing by [@Gedochao](https://github.com/Gedochao) in [#1679](https://github.com/VirtusLab/scala-cli/pull/1679)

#### Fixes
* Suggest to update only to stable version by [@lwronski](https://github.com/lwronski) in [#1634](https://github.com/VirtusLab/scala-cli/pull/1634)
* Fix - Skip checking file order by [@lwronski](https://github.com/lwronski) in [#1696](https://github.com/VirtusLab/scala-cli/pull/1696)
* fix if else in mill.bat by [@MFujarewicz](https://github.com/MFujarewicz) in [#1661](https://github.com/VirtusLab/scala-cli/pull/1661)
* Add repositories from build options when validating scala versions by [@lwronski](https://github.com/lwronski) in [#1630](https://github.com/VirtusLab/scala-cli/pull/1630)
* Fix using directives not working with the shebang line in `.scala` files by [@Gedochao](https://github.com/Gedochao) in [#1639](https://github.com/VirtusLab/scala-cli/pull/1639)
* Don't clear compilation output dir by [@clutroth](https://github.com/clutroth) in [#1660](https://github.com/VirtusLab/scala-cli/pull/1660)

#### Documentation updates

* Decompose the README & add a contributing guide by [@Gedochao](https://github.com/Gedochao) in [#1650](https://github.com/VirtusLab/scala-cli/pull/1650)
* Improve IDE support docs by [@Gedochao](https://github.com/Gedochao) in [#1684](https://github.com/VirtusLab/scala-cli/pull/1684)


#### Build and internal changes
* Use snapshot repo to download stubs by [@lwronski](https://github.com/lwronski) in [#1693](https://github.com/VirtusLab/scala-cli/pull/1693)
* Temporarily rollback CI to `ubuntu-20.04` by [@Gedochao](https://github.com/Gedochao) in [#1640](https://github.com/VirtusLab/scala-cli/pull/1640)
* Fix - merge extra repos with resolve.repositories by [@lwronski](https://github.com/lwronski) in [#1643](https://github.com/VirtusLab/scala-cli/pull/1643)
* Use Mill directory convention in mill project by [@lolgab](https://github.com/lolgab) in [#1676](https://github.com/VirtusLab/scala-cli/pull/1676)


#### Updates & maintenance
* Update coursier-jvm_2.13, ... to 2.1.0-RC3 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1688](https://github.com/VirtusLab/scala-cli/pull/1688)
* Update coursier-jvm_2.13, ... to 2.1.0-RC3-1 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1702](https://github.com/VirtusLab/scala-cli/pull/1702)
* Update slf4j-nop to 2.0.6 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1691](https://github.com/VirtusLab/scala-cli/pull/1691)
* Ignore `jsoniter` updates for JDK 8 by [@lwronski](https://github.com/lwronski) in [#1694](https://github.com/VirtusLab/scala-cli/pull/1694)
* Update trees_2.13 to 4.7.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1690](https://github.com/VirtusLab/scala-cli/pull/1690)
* Update jsoniter-scala-core_2.13 to 2.19.1 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1674](https://github.com/VirtusLab/scala-cli/pull/1674)
* Update jsoniter-scala-core_2.13 to 2.19.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1672](https://github.com/VirtusLab/scala-cli/pull/1672)
* Update os-lib to 0.9.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1653](https://github.com/VirtusLab/scala-cli/pull/1653)
* Update scala-collection-compat to 2.9.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1657](https://github.com/VirtusLab/scala-cli/pull/1657)
* Update core_2.13 to 3.8.5 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1655](https://github.com/VirtusLab/scala-cli/pull/1655)
* Update pprint to 0.8.1 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1654](https://github.com/VirtusLab/scala-cli/pull/1654)
* Update mill-main to 0.10.10 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1652](https://github.com/VirtusLab/scala-cli/pull/1652)
* Update org.eclipse.jgit to 6.4.0.202211300538-r by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1656](https://github.com/VirtusLab/scala-cli/pull/1656)
* Update jsoniter-scala-core_2.13 to 2.18.1 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1651](https://github.com/VirtusLab/scala-cli/pull/1651)
* Update slf4j-nop to 2.0.5 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1658](https://github.com/VirtusLab/scala-cli/pull/1658)
* Bump VirtusLab/scala-cli-setup from 0.1.17 to 0.1.18 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#1644](https://github.com/VirtusLab/scala-cli/pull/1644)
* Update scala-cli.sh launcher for 0.1.18 by [@github-actions](https://github.com/features/actions) in [#1624](https://github.com/VirtusLab/scala-cli/pull/1624)
* Update using_directives to 0.0.10 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1692](https://github.com/VirtusLab/scala-cli/pull/1692)
* Bumped up com.lihaoyi::os-lib version to 0.9.0 by [@pingu1m](https://github.com/scala-steward-org/pingu1m) in [#1649](https://github.com/VirtusLab/scala-cli/pull/1649)

### New Contributors
* [@pingu1m](https://github.com/pingu1m) made their first contribution in [#1649](https://github.com/VirtusLab/scala-cli/pull/1649)
* [@clutroth](https://github.com/clutroth) made their first contribution in [#1660](https://github.com/VirtusLab/scala-cli/pull/1660)
* [@MFujarewicz](https://github.com/MFujarewicz) made their first contribution in [#1661](https://github.com/VirtusLab/scala-cli/pull/1661)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v0.1.18...v0.1.19

## [v0.1.18](https://github.com/VirtusLab/scala-cli/releases/tag/v0.1.18)

### Filter tests with `--test-only`
It is now possible to filter test suites with the `--test-only` option.

```scala title=BarTests.scala
//> using dep "org.scalameta::munit::1.0.0-M7"
package tests.only
class Tests extends munit.FunSuite {
  test("bar") {
    assert(2 + 2 == 5)
  }
  test("foo") {
    assert(2 + 3 == 5)
  }
  test("foo-again") {
    assert(2 + 3 == 5)
  }
}
```

```scala title=HelloTests.scala
package tests
class HelloTests extends munit.FunSuite {
  test("hello") {
    assert(2 + 2 == 4)
  }
}
```

```bash fail
scala-cli test BarTests.scala HelloTests.scala --test-only 'tests.only*' 
# tests.only.Tests:
# ==> X tests.only.Tests.bar  0.037s munit.FailException: ~/project/src/test/BarTests.scala:5 assertion failed
# 4:  test("bar") {
# 5:    assert(2 + 2 == 5)
# 6:  }
#     at munit.FunSuite.assert(FunSuite.scala:11)
#     at tests.only.Tests.$init$$$anonfun$1(BarTests.scala:5)
#     at tests.only.Tests.$init$$$anonfun$adapted$1(BarTests.scala:6)
#   + foo 0.004s
#   + foo-again 0.001s
```

Filtering particular tests by name requires passing args to the test framework.
For example, with `munit`:

```bash
scala-cli test BarTests.scala HelloTests.scala --test-only 'tests.only*'  -- '*foo*'
# tests.only.Tests:
#   + foo 0.032s
#   + foo-again 0.001s
```

Added by [@lwronski](https://github.com/lwronski) in [#1604](https://github.com/VirtusLab/scala-cli/pull/1604)

### Accept authenticated proxy params via Scala CLI config
If you can only download artifacts through an authenticated proxy, it is now possible to configure it
with the `config` subcommand.

```bash ignore
scala-cli config httpProxy.address https://proxy.company.com
scala-cli config httpProxy.user _encoded_user_
scala-cli config httpProxy.password _encoded_password_
```

Replace `_encoded_user_` and `_encoded_password_` by your actual user and password, following
the [password option format](/docs/reference/password-options.md). They should typically look like
`env:ENV_VAR_NAME`, `file:/path/to/file`, or `command:command to run`.

Added by [@alexarchambault](https://github.com/alexarchambault) in [#1593](https://github.com/VirtusLab/scala-cli/pull/1593)

### Support for running Markdown sources from zipped archives and gists
It is now possible to run `.md` sources inside a `.zip` archive.
Same as with directories,  `.md` sources inside zipped archives are ignored by default, unless
the `--enable-markdown` option is passed.

```bash ignore
scala-cli archive-with-markdown.zip --enable-markdown
```

This also enables running Markdown sources fom GitHub gists, as those are downloaded by Scala CLI as zipped archives.

```bash ignore
scala-cli https://gist.github.com/Gedochao/6415211eeb8ca4d8d6db123f83f0f839 --enable-markdown
```

It is also possible to point Scala CLI to a `.md` file with a direct URL.

```bash ignore
scala-cli https://gist.githubusercontent.com/Gedochao/6415211eeb8ca4d8d6db123f83f0f839/raw/4c5ce7593e19f1390555221e0d076f4b02f4b4fd/example.md
```

Added by [@Gedochao](https://github.com/Gedochao) in [#1581](https://github.com/VirtusLab/scala-cli/pull/1581)

### Support for running piped Markdown sources
Instead of passing paths to your Markdown sources, you can also pipe your code via standard input:

```bash
echo '# Example Snippet
```scala
println("Hello")
```' | scala-cli _.md
```

Added by [@Gedochao](https://github.com/Gedochao) in [#1582](https://github.com/VirtusLab/scala-cli/pull/1582)

### Support for running Markdown snippets
It is now possible to pass Markdown code as a snippet directly from the command line.

````bash
scala-cli run --markdown-snippet '# Markdown snippet
with a code block
```scala
println("Hello")
```'
````

Added by [@Gedochao](https://github.com/Gedochao) in [#1583](https://github.com/VirtusLab/scala-cli/pull/1583)

### Customize exported Mill project name
It is now possible to pass the desired name of your Mill project to the `export` sub-command 
with the `--project` option. 

```bash ignore
scala-cli export . --mill -o mill-proj --project project-name
```

Added by [@carlosedp](https://github.com/carlosedp) in [#1563](https://github.com/VirtusLab/scala-cli/pull/1563)

### Export Scala compiler options to Mill projects
It is now possible to export `scalac` options from a Scala CLI project to Mill with the `export` sub-command.

Added by [@lolgab](https://github.com/lolgab) in [#1562](https://github.com/VirtusLab/scala-cli/pull/1562)

### Other changes

#### Fixes
* Fix overriding settings from tests by [@alexarchambault](https://github.com/alexarchambault) in [#1566](https://github.com/VirtusLab/scala-cli/pull/1566)
* Print compilation failed in watch mode too in test command by [@alexarchambault](https://github.com/alexarchambault) in [#1548](https://github.com/VirtusLab/scala-cli/pull/1548)
* Fix error message when running JVM launcher from Java 8 by [@alexarchambault](https://github.com/alexarchambault) in [#1575](https://github.com/VirtusLab/scala-cli/pull/1575)
* Fix `using` directives for Markdown inputs by [@Gedochao](https://github.com/Gedochao) in [#1598](https://github.com/VirtusLab/scala-cli/pull/1598)
* Fix - clean up only homebrew-scala-experimental directory by [@lwronski](https://github.com/lwronski) in [#1615](https://github.com/VirtusLab/scala-cli/pull/1615)
* Warn users when pushing to Sonatype with missing credentials or params by [@alexarchambault](https://github.com/alexarchambault) in [#1545](https://github.com/VirtusLab/scala-cli/pull/1545)
* Warning for multiple files with using directives by [@wleczny](https://github.com/wleczny) in [#1591](https://github.com/VirtusLab/scala-cli/pull/1591)
* Make package --python work by [@alexarchambault](https://github.com/alexarchambault) in [#1531](https://github.com/VirtusLab/scala-cli/pull/1531)
* Better revolver output by [@alexarchambault](https://github.com/alexarchambault) in [#1614](https://github.com/VirtusLab/scala-cli/pull/1614)
* Make `PackageTestsDefault.reuse run native binary` more robust by [@lwronski](https://github.com/lwronski) in [1621](https://github.com/VirtusLab/scala-cli/pull/1621)

#### Documentation updates
* Add some explanations on implicit sub-commands in `-help` by [@Gedochao](https://github.com/Gedochao) in [#1587](https://github.com/VirtusLab/scala-cli/pull/1587)
* Runner specification by [@romanowski](https://github.com/romanowski) in [#1445](https://github.com/VirtusLab/scala-cli/pull/1445)
* Install documentation update by [@wleczny](https://github.com/wleczny) in [#1595](https://github.com/VirtusLab/scala-cli/pull/1595)
* Document recent features & changes affecting working with Markdown inputs  by [@Gedochao](https://github.com/Gedochao) in [#1606](https://github.com/VirtusLab/scala-cli/pull/1606)
* Improve docs coverage with `sclicheck` by [@Gedochao](https://github.com/Gedochao) in [#1612](https://github.com/VirtusLab/scala-cli/pull/1612)
* Reduce ignore tags in the docs snippets by [@Gedochao](https://github.com/Gedochao) in [#1617](https://github.com/VirtusLab/scala-cli/pull/1617)

#### Build and internal changes
* Remove superfluous annotation by [@alexarchambault](https://github.com/alexarchambault) in [#1567](https://github.com/VirtusLab/scala-cli/pull/1567)
* Decompose & refactor `Inputs` by [@Gedochao](https://github.com/Gedochao) in [#1565](https://github.com/VirtusLab/scala-cli/pull/1565)
* Disable create PGP key test on Windows CI by [@alexarchambault](https://github.com/alexarchambault) in [#1588](https://github.com/VirtusLab/scala-cli/pull/1588)
* Switch to Scala 3-based case-app by [@alexarchambault](https://github.com/alexarchambault) in [#1568](https://github.com/VirtusLab/scala-cli/pull/1568)
* Remove cli-options module by [@alexarchambault](https://github.com/alexarchambault) in [#1552](https://github.com/VirtusLab/scala-cli/pull/1552)
* Enable to force using jvm signing launcher for native launcher of scala-cli by [@lwronski](https://github.com/lwronski) in [#1597](https://github.com/VirtusLab/scala-cli/pull/1597)
* Run warm up test before running default tests by [@lwronski](https://github.com/lwronski) in [#1599](https://github.com/VirtusLab/scala-cli/pull/1599)
* Make DefaultTests more robust by [@alexarchambault](https://github.com/alexarchambault) in [#1613](https://github.com/VirtusLab/scala-cli/pull/1613)

#### Updates & maintenance
* Update scala-cli.sh launcher for 0.1.17 by [@github-actions](https://github.com/features/actions) in [#1564](https://github.com/VirtusLab/scala-cli/pull/1564)
* Update zip-input-stream to 0.1.1 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1573](https://github.com/VirtusLab/scala-cli/pull/1573)
* Update coursier-jvm_2.13, ... to 2.1.0-RC1 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1572](https://github.com/VirtusLab/scala-cli/pull/1572)
* Update mill-main to 0.10.9 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1571](https://github.com/VirtusLab/scala-cli/pull/1571)
* Update test-runner, tools to 0.4.8 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1574](https://github.com/VirtusLab/scala-cli/pull/1574)
* Update case-app_2.13 to 2.1.0-M21 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1570](https://github.com/VirtusLab/scala-cli/pull/1570)
* Bump VirtusLab/scala-cli-setup from 0.1.16 to 0.1.17 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#1579](https://github.com/VirtusLab/scala-cli/pull/1579)
* Bump Ammonite to 2.5.5-17-df243e14 & Scala to 3.2.1 by [@Gedochao](https://github.com/Gedochao) in [#1586](https://github.com/VirtusLab/scala-cli/pull/1586)
* Update scala-cli-signing to 0.1.13 by [@alexarchambault](https://github.com/alexarchambault) in [#1569](https://github.com/VirtusLab/scala-cli/pull/1569)
* Update coursier-jvm_2.13, ... to 2.1.0-RC2 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1590](https://github.com/VirtusLab/scala-cli/pull/1590)
* Update scalajs-sbt-test-adapter_2.13 to 1.11.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1477](https://github.com/VirtusLab/scala-cli/pull/1477)
* Update slf4j-nop to 2.0.4 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1596](https://github.com/VirtusLab/scala-cli/pull/1596)
* Update jsoniter-scala-core_2.13 to 2.18.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1608](https://github.com/VirtusLab/scala-cli/pull/1608)
* Update test-runner, tools to 0.4.9 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1610](https://github.com/VirtusLab/scala-cli/pull/1610)
* Update Bloop to 1.5.4-sc-4 by [@alexarchambault](https://github.com/alexarchambault) in [#1622](https://github.com/VirtusLab/scala-cli/pull/1622)

### New Contributors
* [@carlosedp](https://github.com/carlosedp) made their first contribution in [#1563](https://github.com/VirtusLab/scala-cli/pull/1563)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v0.1.17...v0.1.18

## [v0.1.17](https://github.com/VirtusLab/scala-cli/releases/tag/v0.1.17)

### SDKMAN and Homebrew support installation of Scala CLI for M1

To install Scala CLI via SDKMAN, run the following command from the command line:
```
sdk install scalacli
```

and to install Scala CLI via homebrew:
```
brew install Virtuslab/scala-cli/scala-cli
```

Added by [@wleczny](https://github.com/wleczny) in [#1505](https://github.com/VirtusLab/scala-cli/pull/1505) and [#1497](https://github.com/VirtusLab/scala-cli/pull/1497)


### Specifying the `--jvm` option via using directives

The `--jvm` option can now be added via using directives, like
```scala
//> using jvm "temurin:11"
```

Added by [@lwronski](https://github.com/lwronski) in [#1539](https://github.com/VirtusLab/scala-cli/pull/1539)

### Accept more `scalac` options without escaping

Scala CLI now accepts options such as `-rewrite`, `-new-syntax`, `-old-syntax`, `-source:<target>`, `-indent` and `-no-indent`, without requiring them to be escaped by `-O`.

Fixed by [@Gedochao](https://github.com/Gedochao) in [#1501](https://github.com/VirtusLab/scala-cli/pull/1501)

### Enable `python` support  via using directives

The `--python` option can now be enabled via a using directive, like
```scala
//> using python
```

Added by [@alexarchambault](https://github.com/alexarchambault) in [#1492](https://github.com/VirtusLab/scala-cli/pull/1492)

### Other changes

#### Publish

* Various config command tweaks / fixes by [@alexarchambault](https://github.com/alexarchambault)  in [#1460](https://github.com/VirtusLab/scala-cli/pull/1460)
* Accept email via --email when creating a PGP key in config command by [@alexarchambault](https://github.com/alexarchambault)  in [#1482](https://github.com/VirtusLab/scala-cli/pull/1482)
* Make publish --python work by [@alexarchambault](https://github.com/alexarchambault)  in [#1494](https://github.com/VirtusLab/scala-cli/pull/1494)
* Add repositories.credentials config key by [@alexarchambault](https://github.com/alexarchambault)  in [#1466](https://github.com/VirtusLab/scala-cli/pull/1466)
* Check for missing org and version at the same time in publish by [@alexarchambault](https://github.com/alexarchambault)  in [#1534](https://github.com/VirtusLab/scala-cli/pull/1534)
* Rename some publish config keys by [@alexarchambault](https://github.com/alexarchambault)  in [#1532](https://github.com/VirtusLab/scala-cli/pull/1532)
* Add publish.credentials config key, use it to publish by [@alexarchambault](https://github.com/alexarchambault)  in [#1533](https://github.com/VirtusLab/scala-cli/pull/1533)

#### Spark

* Accept spark-submit arguments on the command-line by [@alexarchambault](https://github.com/alexarchambault)  in [#1455](https://github.com/VirtusLab/scala-cli/pull/1455)

#### Fixes

* Fix generating pkg package for M1 by [@lwronski](https://github.com/lwronski) in [#1461](https://github.com/VirtusLab/scala-cli/pull/1461)
* Return exit code 1 when build fails for test by [@lwronski](https://github.com/lwronski) in [#1518](https://github.com/VirtusLab/scala-cli/pull/1518)
* Fix the `nativeEmbedResources` using directive by [@Gedochao](https://github.com/Gedochao) in [#1525](https://github.com/VirtusLab/scala-cli/pull/1525)

#### Build and internal changes

* Automate deploy of scala-experimental brew formula by [@wleczny](https://github.com/wleczny) in [#1530](https://github.com/VirtusLab/scala-cli/pull/1530)
* Decompose RunTestDefinitions by [@Gedochao](https://github.com/Gedochao) in [#1529](https://github.com/VirtusLab/scala-cli/pull/1529)
* Add some simple tests for running `.md` sources by [@Gedochao](https://github.com/Gedochao) in [#1527](https://github.com/VirtusLab/scala-cli/pull/1527)
* Run doc tests from munit test suites by [@alexarchambault](https://github.com/alexarchambault)  in [#1435](https://github.com/VirtusLab/scala-cli/pull/1435)
* Minor refacto around build options stuff by [@alexarchambault](https://github.com/alexarchambault)  in [#1488](https://github.com/VirtusLab/scala-cli/pull/1488)
* No need to use os.ProcessOutput.ReadLines in test by [@alexarchambault](https://github.com/alexarchambault)  in [#1491](https://github.com/VirtusLab/scala-cli/pull/1491)
* Enforce logging options for all scala commands by [@Gedochao](https://github.com/Gedochao) in [#1499](https://github.com/VirtusLab/scala-cli/pull/1499)
* Tweak documentation verification tests by [@Gedochao](https://github.com/Gedochao) in [#1504](https://github.com/VirtusLab/scala-cli/pull/1504)
* Support `jvmRunEnvironment` and `jvmTestEnvironment` for BSP by [@Gedochao](https://github.com/Gedochao) in [#1519](https://github.com/VirtusLab/scala-cli/pull/1519)
* Downgrade Scala version in 'scala-cli repl --amm' if needed by [@alexarchambault](https://github.com/alexarchambault)  [#1493](https://github.com/VirtusLab/scala-cli/pull/1493)

#### Documentation / help updates
* Tweak / fix publish messages by [@alexarchambault](https://github.com/alexarchambault)  in [#1535](https://github.com/VirtusLab/scala-cli/pull/1535)
* Merge documentation of installing scala-cli on MacOs and MacOs/M1 by [@wleczny](https://github.com/wleczny) in [#1507](https://github.com/VirtusLab/scala-cli/pull/1507)
* Improve the basics doc by [@Gedochao](https://github.com/Gedochao) in [#1513](https://github.com/VirtusLab/scala-cli/pull/1513)
* Fix a typo in the `--server` option reference doc by [@Gedochao](https://github.com/Gedochao) in [#1521](https://github.com/VirtusLab/scala-cli/pull/1521)
* Improve the docs on using Scala compiler options by [@Gedochao](https://github.com/Gedochao) in [#1503](https://github.com/VirtusLab/scala-cli/pull/1503)
* Add help for repl, scalafmt and scaladoc by [@wleczny](https://github.com/wleczny) in [#1487](https://github.com/VirtusLab/scala-cli/pull/1487)
* remove paragraph about bug for coursier install by [@bishabosha](https://github.com/bishabosha) in [#1485](https://github.com/VirtusLab/scala-cli/pull/1485)
* Tell about pressing Enter in watch message by [@alexarchambault](https://github.com/alexarchambault)  in [#1465](https://github.com/VirtusLab/scala-cli/pull/1465)


#### Updates / maintainance
* Update jsoniter-scala-core_2.13 to 2.17.9 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1544](https://github.com/VirtusLab/scala-cli/pull/1544)
* Bump docusaurus to 2.20 and other docs deps by [@lwronski](https://github.com/lwronski) in [#1540](https://github.com/VirtusLab/scala-cli/pull/1540)
* Update jsoniter-scala-core_2.13 to 2.17.8 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1537](https://github.com/VirtusLab/scala-cli/pull/1537)
* Update cli-options_2.13, cli_2.13, ... to 0.1.11 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1538](https://github.com/VirtusLab/scala-cli/pull/1538)
* Update case-app_2.13 to 2.1.0-M19 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1536](https://github.com/VirtusLab/scala-cli/pull/1536)
* Bump coursier/setup-action from 1.2.1 to 1.3.0 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#1496](https://github.com/VirtusLab/scala-cli/pull/1496)
* Update scala-cli.sh launcher for 0.1.16 by [@github-actions](https://github.com/features/actions) in [#1458](https://github.com/VirtusLab/scala-cli/pull/1458)
* Bump VirtusLab/scala-cli-setup from 0.1.15 to 0.1.16 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#1462](https://github.com/VirtusLab/scala-cli/pull/1462)
* Update expecty to 0.16.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1467](https://github.com/VirtusLab/scala-cli/pull/1467)
* Update jsoniter-scala-core_2.13 to 2.17.5 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1470](https://github.com/VirtusLab/scala-cli/pull/1470)
* Update mill-main to 0.10.8 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1472](https://github.com/VirtusLab/scala-cli/pull/1472)
* Update pprint to 0.8.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1473](https://github.com/VirtusLab/scala-cli/pull/1473)
* Update core_2.13 to 3.8.3 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1474](https://github.com/VirtusLab/scala-cli/pull/1474)
* Update publish_2.13 to 0.1.3 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1475](https://github.com/VirtusLab/scala-cli/pull/1475)
* Update trees_2.13 to 4.6.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1478](https://github.com/VirtusLab/scala-cli/pull/1478)
* Update slf4j-nop to 2.0.3 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1479](https://github.com/VirtusLab/scala-cli/pull/1479)
* Update asm to 9.4 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1476](https://github.com/VirtusLab/scala-cli/pull/1476)
* Update using_directives to 0.0.9 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1480](https://github.com/VirtusLab/scala-cli/pull/1480)
* Update fansi to 0.4.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1471](https://github.com/VirtusLab/scala-cli/pull/1471)
* Update case-app_2.13 to 2.1.0-M18 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1468](https://github.com/VirtusLab/scala-cli/pull/1468)
* Bump webfactory/ssh-agent from 0.5.4 to 0.7.0 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#1495](https://github.com/VirtusLab/scala-cli/pull/1495)
* Update jsoniter-scala-core_2.13 to 2.17.6 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1498](https://github.com/VirtusLab/scala-cli/pull/1498)
* Update coursier to 2.1.0-M7-39-gb8f3d7532 by [@alexarchambault](https://github.com/alexarchambault)  in [#1520](https://github.com/VirtusLab/scala-cli/pull/1520)

### New Contributors
* [@bishabosha](https://github.com/bishabosha) made their first contribution in [#1485](https://github.com/VirtusLab/scala-cli/pull/1485)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v0.1.16...v0.1.17

## [v0.1.16](https://github.com/VirtusLab/scala-cli/releases/tag/v0.1.16)

This release consists mainly of updates, fixes, and various enhancements of existing features.

### Specifying javac options via using directives

javac options can now be added via using directives, like
```scala
//> using javacOpt "source", "1.8", "target", "1.8"
```

Added by [@lwronski](https://github.com/lwronski) in [#1438](https://github.com/VirtusLab/scala-cli/pull/1438)

### Pressing enter in watch mode proceeds to run / compile / test / … again

In watch mode (using the `-w` or `--watch` option), pressing Enter when Scala CLI is watching for changes makes it run again what it's supposed to be doing (compiling, running, running tests, or packaging, etc.) This is inspired by Mill's behaviour in watch mode, which supports the same feature.

Added by [@alexarchambault](https://github.com/alexarchambault) in [#1451](https://github.com/VirtusLab/scala-cli/pull/1451)

### Installation via Scoop on Windows

Scala CLI can now be installed via [Scoop](https://scoop.sh) on Windows, with a command such as
```bat
scoop install scala-cli
```

Added by [@nightscape](https://github.com/nightscape) in [#1416](https://github.com/VirtusLab/scala-cli/pull/1416), thanks to him!

### Actionable diagnostics in Metals

Scala CLI should now send text edit suggestions with some of its diagnostics, via BSP, so that editors
can suggest those edits to users. This should work in upcoming versions of Metals in particular.

Added by [@lwronski](https://github.com/lwronski) in [#1448](https://github.com/VirtusLab/scala-cli/pull/1448)

### Other

* Add `--scalapy-version` option by [@alexarchambault](https://github.com/alexarchambault) in [#1397](https://github.com/VirtusLab/scala-cli/pull/1397)

#### Fixes

#### Fixes in Scala Native binaries caching

When running a sequence of commands such as
```bash ignore
$ scala-cli run --native .
$ scala-cli --power package --native . -o my-app
```
Scala CLI should cache a Scala Native binary during the first command, so that the second command can just re-use it, rather than generating a binary again. This also fixes the re-use of compilation artifacts between both commands, so that the Scala CLI project isn't re-compiled during the second command either.

Fixed by [@alexarchambault](https://github.com/alexarchambault) in [#1406](https://github.com/VirtusLab/scala-cli/pull/1406)

##### Accept more scalac options without escaping

Scala CLI now accepts options such as `-release`, `-encoding`, `-color`, `-feature`, `-deprecation` and `-nowarn`, without requiring them to be escaped by `-O`. It also accepts `--scalac-verbose`, which is equivalent to `-O -verbose` (increases scalac verbosity). Lastly, it warns when `-release` and / or `-target:<target>` are inconsistent with `--jvm`.

Fixed by [@Gedochao](https://github.com/Gedochao) in [#1413](https://github.com/VirtusLab/scala-cli/pull/1413)

##### Fix `--java-option` and `--javac-option` handling in `package` sub-command

`--java-option` and `--javac-option` should now be accepted and handled properly in the `package` sub-command.

Fixed by [@lwronski](https://github.com/lwronski) in [#1434](https://github.com/VirtusLab/scala-cli/pull/1434)

##### Fix wrong file name when publising Scala.js artifacts locally

The `publish local` sub-command used to publish Scala.js artifacts with a malformed suffix. This is now fixed.

Fixed by [@lwronski](https://github.com/lwronski) in [#1443](https://github.com/VirtusLab/scala-cli/pull/1443)

##### Fix spurious stack traces in the `publish` and `publish local` sub-commands

The `publish` and `publish local` commands could print spurious stack traces when run with non-default locales, using native Scala CLI binaries. This is now fixed.

Fixed by [@romanowski](https://github.com/romanowski) in [#1423](https://github.com/VirtusLab/scala-cli/pull/1423)

##### Make `run --python --native`  work from Python virtualenv

Using both `--native` and `--python` in the `run` sub-command should work fine from Python virtualenv.

Fixed by [@kiendang](https://github.com/kiendang) in [#1399](https://github.com/VirtusLab/scala-cli/pull/1399)

#### Documentation / help updates
* Dump scala 2 version in docs by [@lwronski](https://github.com/lwronski) in [#1408](https://github.com/VirtusLab/scala-cli/pull/1408)
* Ensure the the `repl` & default sub-commands respect group help options by [@Gedochao](https://github.com/Gedochao) in [#1417](https://github.com/VirtusLab/scala-cli/pull/1417)
* Remove stray `_` typo by [@armanbilge](https://github.com/armanbilge) in [#1385](https://github.com/VirtusLab/scala-cli/pull/1385)
* Add docs on how to install scala-cli for M1 by [@lwronski](https://github.com/lwronski) in [#1431](https://github.com/VirtusLab/scala-cli/pull/1431)
* Debugging cookbook by [@wleczny](https://github.com/wleczny) in [#1441](https://github.com/VirtusLab/scala-cli/pull/1441)

#### Updates / maintainance
* Update scala-cli.sh launcher for 0.1.15 by [@github-actions](https://github.com/features/actions) in [#1401](https://github.com/VirtusLab/scala-cli/pull/1401)
* Revert scalafmt fix by [@lwronski](https://github.com/lwronski) in [#1402](https://github.com/VirtusLab/scala-cli/pull/1402)
* Bump respective Scala versions to `2.12.17` & `2.13.9` and Ammonite to `2.5.4-33-0af04a5b` by [@Gedochao](https://github.com/Gedochao) in [#1405](https://github.com/VirtusLab/scala-cli/pull/1405)
* Turn off running tests in PR for M1 runner by [@lwronski](https://github.com/lwronski) in [#1403](https://github.com/VirtusLab/scala-cli/pull/1403)
* Bump VirtusLab/scala-cli-setup from 0.1.14.1 to 0.1.15 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#1414](https://github.com/VirtusLab/scala-cli/pull/1414)
* Bump coursier/setup-action from f883d08305acbc28e5e5363bf5ec086397627021 to 1.2.1 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#1415](https://github.com/VirtusLab/scala-cli/pull/1415)
* Tweak the release procedure by [@Gedochao](https://github.com/Gedochao) in [#1426](https://github.com/VirtusLab/scala-cli/pull/1426)
* Update case-app_2.13 to 2.1.0-M17 & scala-cli-signing to v0.1.10 by [@lwronski](https://github.com/lwronski) in [#1427](https://github.com/VirtusLab/scala-cli/pull/1427)
* Automate choco package deploy by [@wleczny](https://github.com/wleczny) in [#1412](https://github.com/VirtusLab/scala-cli/pull/1412)
* Generate pkg package for m1 by [@lwronski](https://github.com/lwronski) in [#1410](https://github.com/VirtusLab/scala-cli/pull/1410)
* Re-enable gif tests by [@alexarchambault](https://github.com/alexarchambault) in [#1436](https://github.com/VirtusLab/scala-cli/pull/1436)
* Bump Scala 2.13.x to 2.13.10 & Ammonite to 2.5.5 by [@Gedochao](https://github.com/Gedochao) in [#1437](https://github.com/VirtusLab/scala-cli/pull/1437)
* Remove mill-scala-cli stuff from build by [@alexarchambault](https://github.com/alexarchambault) in [#1433](https://github.com/VirtusLab/scala-cli/pull/1433)
* Add support for BSP's `buildTarget/outputPaths` and update bsp4j to 2… by [@lwronski](https://github.com/lwronski) in [#1439](https://github.com/VirtusLab/scala-cli/pull/1439)
* Update bsp4j to 2.1.0-M3 by [@lwronski](https://github.com/lwronski) in [#1444](https://github.com/VirtusLab/scala-cli/pull/1444)
* Update scala-packager to 0.1.29 and hardcode upgradeCodeGuid by [@lwronski](https://github.com/lwronski) in [#1446](https://github.com/VirtusLab/scala-cli/pull/1446)
* Refactor `ScalaCommand` to enforce respecting help options by [@Gedochao](https://github.com/Gedochao) in [#1440](https://github.com/VirtusLab/scala-cli/pull/1440)
* Address compilation warnings by [@alexarchambault](https://github.com/alexarchambault) in [#1452](https://github.com/VirtusLab/scala-cli/pull/1452)
* Update coursier to 2.1.0-M7 by [@alexarchambault](https://github.com/alexarchambault) in [#1447](https://github.com/VirtusLab/scala-cli/pull/1447)
* Update bloop to 1.5.4-sc-3 by [@alexarchambault](https://github.com/alexarchambault) in [#1454](https://github.com/VirtusLab/scala-cli/pull/1454)

### New Contributors
* [@nightscape](https://github.com/nightscape) made their first contribution in [#1416](https://github.com/VirtusLab/scala-cli/pull/1416)
* [@kiendang](https://github.com/kiendang) made their first contribution in [#1399](https://github.com/VirtusLab/scala-cli/pull/1399)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v0.1.15...v0.1.16

## [v0.1.15](https://github.com/VirtusLab/scala-cli/releases/tag/v0.1.15)

### The M1 native launcher is here! (experimental)

We are happy to announce that there is a new dedicated launcher for M1 users. You can find it [here](https://github.com/VirtusLab/scala-cli/releases/download/v0.1.15/scala-cli-aarch64-apple-darwin.gz).

Please note that the `package` sub-command is unstable for this launcher.

Added in [#1396](https://github.com/VirtusLab/scala-cli/pull/1396) by [@lwronski](https://github.com/lwronski)

### `--python` option for `repl` sub-command (experimental)

Passing the `--python` option allows using `ScalaPy` with the `repl` sub-command:

```
▶ scala-cli --python
Welcome to Scala 3.2.0 (17.0.2, Java OpenJDK 64-Bit Server VM).
Type in expressions for evaluation. Or try :help.

scala> import me.shadaj.scalapy.py

scala> py.Dynamic.global.range(1, 4)
val res0: me.shadaj.scalapy.py.Dynamic = range(1, 4)
```

Added in [#1336](https://github.com/VirtusLab/scala-cli/pull/1336) by [@alexarchambault](https://github.com/alexarchambault)

### `-d`, `-classpath` and `compile` sub-command's `--output` options changes

To be backward compatible with the `scala` command, some changes have been made to the following options:
* The `compile` sub-command's  `--output` option has been renamed to `--compilation-output`. This option is now also available from the `run` and `package` sub-commands.

```
▶ scala-cli compile Hello.scala --compilation-output out
▶ scala-cli --main-class Hello -classpath out
Hello
```

* The `-d` option is no longer an alias for `--dependency`, but for `--compilation-output`.
  * `-O -d -O path/to/compilation/output` now defaults to `-d path/to/compilation/output`.

```
▶ scala-cli compile Hello.scala -d out
▶ scala-cli --main-class Hello -classpath out
Hello
```

* The old `--classpath` option has been renamed to `--print-classpath`.
  *  `--classpath`, `--class-path` and `-classpath` options are now aliases for the `--extra jars` option.
  * `-O -classpath -O path/to/classpath` now defaults to `--extra-jars path/to/classpath`.

```
▶ scala-cli compile --print-classpath Hello.scala
# ~/Projects/debug-test/.scala-build/project_103be31561_103be31561-7a1ed8dde0/classes/main:~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala3-library_3/3.2.0/scala3-library_3-3.2.0.jar:~/Library/Caches/ScalaCli/local-repo/v0.1.15/org.virtuslab.scala-cli/runner_3/0.1.15/jars/runner_3.jar:~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.8/scala-library-2.13.8.jar
```

Added in [#1340](https://github.com/VirtusLab/scala-cli/pull/1340) by [@Gedochao](https://github.com/Gedochao)

### Make inputs optional when `-classpath` and `--main-class` are passed

The following changes have been made to improve backward compatibility with the `scala` command:
* Passing the `--main-class` option along with `-classpath` to the default command now defaults to `run` instead of `repl`:

```
▶ scala-cli --main-class Hello -classpath out
Hello
```

* If the `run` sub-command is passed explicitly, it's sufficient to have a main class on the classpath (inputs aren't necessary then):

```
▶ scala-cli compile Hello.scala -d out
▶ scala-cli run -classpath out
Hello
```

Added in [#1369](https://github.com/VirtusLab/scala-cli/pull/1369) by [@Gedochao](https://github.com/Gedochao)

### Debugging with the `run` and `test` sub-commands

It is now possible to debug code ran by `run` and `test` sub-commands:

```
▶ scala-cli Main.scala --debug
Listening for transport dt_socket at address: 5005
Hello
```

This addresses [#1212](https://github.com/VirtusLab/scala-cli/issues/1212)

Added in [#1389](https://github.com/VirtusLab/scala-cli/pull/1389) by [@wleczny](https://github.com/wleczny)

## `--platform` option

This option can be used to choose the platform, which should be used to compile and run the application.

```
▶ scala-cli Main.scala --platform js
Hello
```

Note that `--platform js` is an alias for `--js` and `--platform native` is an alias for `--native`.

This addresses [#1214](https://github.com/VirtusLab/scala-cli/issues/1214)

Added in [#1347](https://github.com/VirtusLab/scala-cli/pull/1347) by [@wleczny](https://github.com/wleczny)

### Other changes

#### Fixes

* Ensure directories are created recursively when the `package` sub-command is called by [@Gedochao](https://github.com/Gedochao) in [#1371](https://github.com/VirtusLab/scala-cli/pull/1371)
* Fix calculation of Scala version and turn off the `-release` flag for 2.12.x \< 2.12.5 by [@Gedochao](https://github.com/Gedochao) in [#1377](https://github.com/VirtusLab/scala-cli/pull/1377)
* Fix finding main classes in external jars by [@Gedochao](https://github.com/Gedochao) in [#1380](https://github.com/VirtusLab/scala-cli/pull/1380)
* Fix Js split style SmallModulesFor in pure JVM by [@lwronski](https://github.com/lwronski) in [#1394](https://github.com/VirtusLab/scala-cli/pull/1394)

#### Build and internal changes

* Remove mill-scalafix customization by [@alexarchambault](https://github.com/alexarchambault) in [#1360](https://github.com/VirtusLab/scala-cli/pull/1360)
* Split config db stuff to a separate config module by [@alexarchambault](https://github.com/alexarchambault) in [#1367](https://github.com/VirtusLab/scala-cli/pull/1367)
* Detect sip when installed by coursier by [@lwronski](https://github.com/lwronski) in [#1368](https://github.com/VirtusLab/scala-cli/pull/1368)
* Create empty class to enforce resolving ivy deps by mill for dummy modules by [@lwronski](https://github.com/lwronski) in [#1374](https://github.com/VirtusLab/scala-cli/pull/1374)
* Use millw launcher instead of running mill by cs by [@lwronski](https://github.com/lwronski) in [#1375](https://github.com/VirtusLab/scala-cli/pull/1375)
* Add --debug option for integration tests by [@wleczny](https://github.com/wleczny) in [#1378](https://github.com/VirtusLab/scala-cli/pull/1378)
* NIT ScalaVersionUtil refactor by [@Gedochao](https://github.com/Gedochao) in [#1384](https://github.com/VirtusLab/scala-cli/pull/1384)
* Make config module compatible with Java 8 by [@alexarchambault](https://github.com/alexarchambault) in [#1387](https://github.com/VirtusLab/scala-cli/pull/1387)
* Add HTTP proxy-related keys in config module by [@alexarchambault](https://github.com/alexarchambault) in [#1388](https://github.com/VirtusLab/scala-cli/pull/1388)
* Add repositories-related keys in config module by [@alexarchambault](https://github.com/alexarchambault) in [#1395](https://github.com/VirtusLab/scala-cli/pull/1395)

#### Updates

* Update scala-cli.sh launcher for 0.1.14 by [@github-actions](https://github.com/features/actions) in [#1362](https://github.com/VirtusLab/scala-cli/pull/1362)
* Update jsoniter-scala-core_2.13 to 2.17.3 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1364](https://github.com/VirtusLab/scala-cli/pull/1364)
* Update core_2.13 to 3.8.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1365](https://github.com/VirtusLab/scala-cli/pull/1365)
* Bump VirtusLab/scala-cli-setup from 0.1.13 to 0.1.14.1 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#1376](https://github.com/VirtusLab/scala-cli/pull/1376)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v0.1.14...v0.1.15

## [v0.1.14](https://github.com/VirtusLab/scala-cli/releases/tag/v0.1.14)

### Hotfix printing stacktraces from Scala CLI runner for Scala 3.x \< 3.2.0
We fixed a nasty bug breaking any Scala CLI run using any Scala 3 version earlier than 3.2.0 on printing stacktraces.
Only Scala CLI 0.1.13 was affected.
```
$ scala-cli about
Scala CLI version: 0.1.13
Scala version (default): 3.2.0
$ scala-cli -S 3.1.3 -e 'throw Exception("Broken")'
Compiling project (Scala 3.1.3, JVM)
Compiled project (Scala 3.1.3, JVM)
Exception in thread "main" java.lang.NoSuchMethodError: 'long scala.runtime.LazyVals$.getOffsetStatic(java.lang.reflect.Field)'
        at scala.cli.runner.StackTracePrinter.<clinit>(StackTracePrinter.scala:101)
        at scala.cli.runner.StackTracePrinter$.coloredStackTraces(StackTracePrinter.scala:104)
        at scala.cli.runner.StackTracePrinter$.$lessinit$greater$default$4(StackTracePrinter.scala:11)
        at scala.cli.runner.Runner$.main(Runner.scala:18)
        at scala.cli.runner.Runner.main(Runner.scala)
```
Added in [#1358](https://github.com/VirtusLab/scala-cli/pull/1358) by [@romanowski](https://github.com/romanowski)

### Build and internal changes
* Disable mill-scala-cli for now by [@alexarchambault](https://github.com/alexarchambault) in [#1335](https://github.com/VirtusLab/scala-cli/pull/1335)
* Update scala-cli.sh launcher for 0.1.13 by [@github-actions](https://github.com/features/actions) in [#1351](https://github.com/VirtusLab/scala-cli/pull/1351)
* Remove backslash which skip execution of `mv` command by [@lwronski](https://github.com/lwronski) in [#1353](https://github.com/VirtusLab/scala-cli/pull/1353)
* Fix import ordering by [@alexarchambault](https://github.com/alexarchambault) in [#1359](https://github.com/VirtusLab/scala-cli/pull/1359)

### Updates
* Update scalafix stuff… by [@alexarchambault](https://github.com/alexarchambault) in [#1333](https://github.com/VirtusLab/scala-cli/pull/1333)
* Bump VirtusLab/scala-cli-setup from 0.1.12 to 0.1.13 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#1354](https://github.com/VirtusLab/scala-cli/pull/1354)


**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v0.1.13...v0.1.14


## [v0.1.13](https://github.com/VirtusLab/scala-cli/releases/tag/v0.1.13)

### Change the default sub-command to `repl` when no args are passed

We no longer default to the `help` sub-command when no arguments are passed. Starting with `0.1.13` running  Scala CLI with no args will launch the `repl`.

```
$ scala-cli -S 3
Welcome to Scala 3.1.3 (17.0.3, Java OpenJDK 64-Bit Server VM).
Type in expressions for evaluation. Or try :help.

scala>
```

When inputs are provided, Scala CLI defaults to the `run` sub-command, as before.

```
$ cat hello.sc
println("Hello World")
$ scala-cli hello.sc
Hello World
```

This change was added by [@Gedochao](https://github.com/Gedochao) in [#1268](https://github.com/VirtusLab/scala-cli/pull/1268)

### Marking the project's workspace root with the `project.settings.scala` file

Scala CLI now supports marking the workspace root directory with an optional configuration file: `project.settings.scala`.  The workspace root determines where the `.bsp` and `.scala-build` directories will be saved (which mostly affects what path should be opened in your IDE to import the Scala CLI project through BSP).

The settings file is also the recommended input for your project's `using directives`. Otherwise, it functions similarly to other `.scala` sources.

```
$ cat project.settings.scala
//> using scala "2.13.4"
$ cat hello.sc
println(util.Properties.versionString)
$ scala-cli hello.sc .
version 2.13.4
```

To see how exactly is the root directory resolved, see [this document](https://github.com/VirtusLab/scala-cli/blob/932c942b78bc35fc0906f2f9e2f6a0c56bef712b/website/docs/reference/root-dir.md)

Added in [#1260](https://github.com/VirtusLab/scala-cli/pull/1260) by [@wleczny](https://github.com/wleczny)

### Scala CLI is now built with Scala 3.2.0

We now rely on Scala `3.2.0` as the default internal Scala version used to build the project.

This change was added by [@lwronski](https://github.com/lwronski) in [#1314](https://github.com/VirtusLab/scala-cli/pull/1314)

### Add resources support for Scala Native

Scala CLI now allows embedding resources (by default) in a Scala Native binary with the `--native` flag.

```
$ cat resources/scala-native/foo.c
int foo(int i) {
  return i + 42;
}
$ cat hello.scala
//> using platform "native"
//> using resourceDir "resources"

import scalanative.unsafe.*

@extern
def foo(int: CInt): CInt = extern

@main def main =
  println(foo(3))
$ scala-cli hello.scala --native
45
```

Added in [#812](https://github.com/VirtusLab/scala-cli/pull/812) by [@jchyb](https://github.com/jchyb)

###  Default to the `run` sub-command instead of `repl` when the `-e`, `--execute-script`, `--execute-scala` or `--execute-java` options are passed.

Even though we default to the `repl` sub-command when no arguments are passed to Scala CLI, an exception to that rule is when a snippet is passed with one of the following options: `-e`, `--execute-script`, `--execute-scala` or `--execute-java`. In that case, the passed snippets are treated as inputs to be executed and switch the default to the `run` sub-command.
```
$ scala-cli -e 'println("Hello")'
Hello
```

If you still want to pass a snippet to the `repl`, you can either pass the `repl` sub-command explicitly or use one of the following options, as before: `--script-snippet`, `--scala-snippet` or `--java-snippet`.
```
$ scala-cli --script-snippet 'println("Hello")'
Welcome to Scala 3.1.3 (17.0.2, Java OpenJDK 64-Bit Server VM).
Type in expressions for evaluation. Or try :help.

scala> snippet_sc.main(Array.empty)
Hello
```
This change was introduced to make the `-e` option backwards compatible with the `scala` command.

Added in [#1313](https://github.com/VirtusLab/scala-cli/pull/1313) by [@Gedochao](https://github.com/Gedochao)

### Work in progress

#### Support for Markdown (experimental)

Scala CLI can now accept `.md` inputs and run/compile a snippet of Scala code inside the markdown. Markdown sources are ignored by default unless passed explicitly as inputs. You can also enable including non-explicit `.md` inputs by passing the `--enable-markdown` option.

Plain `scala` snippets are treated similarly to `.sc` scripts which can be run by `scala-cli`:

````markdown
$ cat Example.md
This is a simple example of an `.md` file with a Scala snippet.

```scala
val message = "Hello from Markdown"
println(message)
```
````

```
scala-cli Example.md
Hello from Markdown
```

See [this document](https://github.com/VirtusLab/scala-cli/blob/5f15ada41fbdcce9b9efd93bd63d513e3476a69a/website/docs/guides/markdown.md) for more details about the experimental Markdown support.

Added in [#1268](https://github.com/VirtusLab/scala-cli/pull/1268) by [@Gedochao](https://github.com/Gedochao)

#### Add `--python` option for the `run` sub-command (experimental)

The `run` sub-command can now run ScalaPy when the `--python` option is passed.

```
$ cat helloscalapy.sc
import py.SeqConverters
val len = py.Dynamic.global.len(List(0, 2, 3).toPythonProxy)
println(s"Length is $len")
$ scala-cli helloscalapy.sc --python -S 2.13
Length is 3
```

Added in [#1295](https://github.com/VirtusLab/scala-cli/pull/1295) by [@alexarchambault](https://github.com/alexarchambault)

### Other changes

#### Documentation

* Correct using directives on configuration.md by [@megri](https://github.com/megri) in [#1278](https://github.com/VirtusLab/scala-cli/pull/1278)
* Improve dependencies doc by [@Gedochao](https://github.com/Gedochao) in [#1287](https://github.com/VirtusLab/scala-cli/pull/1287)

#### Fixes

* Fix path to sourceMappingURL by [@lwronski](https://github.com/lwronski) in [#1286](https://github.com/VirtusLab/scala-cli/pull/1286)

#### Build and internal changes

* Improve the error message for when a build's main class is ambiguous by [@Gedochao](https://github.com/Gedochao) in [#1323](https://github.com/VirtusLab/scala-cli/pull/1323)
* Improve the error message for unsupported Scala version with Ammonite by [@Gedochao](https://github.com/Gedochao) in [#1327](https://github.com/VirtusLab/scala-cli/pull/1327)
* Detect ARM64 macs when downloading coursier launcher by [@keynmol](https://github.com/keynmol) in  [#1282](https://github.com/VirtusLab/scala-cli/pull/1282)
* Make test("...".only) work again in RunTestDefinitions by [alexarchambault](https://github.com/alexarchambault) in [#1294](https://github.com/VirtusLab/scala-cli/pull/1294)
* Use os-lib short-hand method trim when possible by [alexarchambault](https://github.com/alexarchambault) in [#1334](https://github.com/VirtusLab/scala-cli/pull/1334)
* Add missing repl tests by [alexarchambault](https://github.com/alexarchambault) in [#1332](https://github.com/VirtusLab/scala-cli/pull/1332)
* Scala CLI deb package - Priority and Section flag by [@lwronski](https://github.com/lwronski) in [#1338](https://github.com/VirtusLab/scala-cli/pull/1338)

#### Updates

* Update ammonite to 2.5.4-16-7317286d by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1283](https://github.com/VirtusLab/scala-cli/pull/1283)
* Update mill-main to 0.10.7 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1284](https://github.com/VirtusLab/scala-cli/pull/1284)
* Update scalajs-env-nodejs_2.13 to 1.4.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1303](https://github.com/VirtusLab/scala-cli/pull/1303)
* Update jsoniter-scala-core_2.13 to 2.16.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1302](https://github.com/VirtusLab/scala-cli/pull/1302)
* Update core_2.13 to 3.7.6 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1299](https://github.com/VirtusLab/scala-cli/pull/1299)
* Update ammonite to 2.5.4-19-cd76521f by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1298](https://github.com/VirtusLab/scala-cli/pull/1298)
* Update bsp4j to 2.1.0-M1 by [@lwronski](https://github.com/lwronski) in [#1277](https://github.com/VirtusLab/scala-cli/pull/1277)
* Bump VirtusLab/scala-cli-setup from 0.1.11 to 0.1.12 by [@dependabot](https://docs.github.com/en/code-security/dependabot) in [#1306](https://github.com/VirtusLab/scala-cli/pull/1306)
* Update jsoniter-scala-core_2.13 to 2.17.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1311](https://github.com/VirtusLab/scala-cli/pull/1311)
* Update test-runner, tools to 0.4.7 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1317](https://github.com/VirtusLab/scala-cli/pull/1317)
* Update jsoniter-scala-core_2.13 to 2.17.1 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1320](https://github.com/VirtusLab/scala-cli/pull/1320)
* Update ammonite_3.1.3 to 2.5.4-22-4a9e6989 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1329](https://github.com/VirtusLab/scala-cli/pull/1329)
* Update jsoniter-scala-core_2.13 to 2.17.2 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1343](https://github.com/VirtusLab/scala-cli/pull/1343)
* Update python-native-libs to 0.2.4 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1341](https://github.com/VirtusLab/scala-cli/pull/1341)
* Update org.eclipse.jgit to 6.3.0.202209071007-r by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1344](https://github.com/VirtusLab/scala-cli/pull/1344)

### New Contributors
* [@megri](https://github.com/megri) made their first contribution in [#1278](https://github.com/VirtusLab/scala-cli/pull/1278)
* [@keynmol](https://github.com/keynmol) made their first contribution in [#1282](https://github.com/VirtusLab/scala-cli/pull/1282)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v0.1.12...v0.1.13


## [v0.1.12](https://github.com/VirtusLab/scala-cli/releases/tag/v0.1.12)

### Add `--spark`, `--spark-standalone` and `--hadoop` options for the `run` sub-command
The `run` sub-command can now run Spark jobs when the `--spark` option is passed.
```text
$ scala-cli run --spark SparkJob.scala
```
Similarly, it's possible to run Hadoop jobs by passing the `--hadoop` option.
```text
scala-cli run --hadoop HadoopJob.java
```
It's also possible to run Spark jobs without a Spark distribution by passing the `--spark-standalone` option.
```text
$ scala-cli run --spark-standalone SparkJob.scala
```

Added in [#1129](https://github.com/VirtusLab/scala-cli/pull/1129) by [alexarchambault](https://github.com/alexarchambault)

### Add the default Scala version to the output of the `version` sub-command
The `version` sub-command now includes both the Scala CLI version and the default Scala version.
```text
$ scala-cli --version
Scala CLI version 0.1.12
Default Scala version: 3.1.3
$ scala-cli -version
Scala CLI version 0.1.12
Default Scala version: 3.1.3
$ scala-cli version
Scala CLI version 0.1.12
Default Scala version: 3.1.3
```
You can also pass the `--cli-version` option to only get the Scala CLI version or the `--scala-version` option
to only get the default Scala version.
```text
$ scala-cli version --cli-version
0.1.12
$ scala-cli version --scala-version
3.1.3
```
This is potentially a breaking change if your automation relies on the output of the `version` sub-command.

Added in [#1262](https://github.com/VirtusLab/scala-cli/pull/1262) by [lwronski](https://github.com/lwronski)

### Enable passing the `scalafmt` configuration with `--scalafmt-conf` and `--scalafmt-conf-str`
It is now possible to pass a custom location of the `scalafmt` configuration with the `--scalafmt-conf` option for the
`fmt` sub-command.
```text
$ scala-cli fmt --scalafmt-conf path/to/the/conf/.scalafmt.conf
```
You can also pass the configuration straight from the terminal with `--scalafmt-conf-str`.
```text
$ scala-cli fmt --scalafmt-conf-str  "version=3.5.5
runner.dialect=scala213"
```
Added in [#1227](https://github.com/VirtusLab/scala-cli/pull/1227) by [wleczny](https://github.com/wleczny)

### Enable turning the `--interactive` mode on permanently
It is now possible to set the `--interactive` mode on by default, so that passing it explicitly isn't necessary.

The next time when you run a command with the `--interactive` option set to on, Scala CLI will suggest to turn it on
permanently.

This is recommended for environments where `scala-cli` is used by a human user only (and not by any automation).

```text
$ scala-cli . --interactive
You have run the current scala-cli command with the --interactive mode turned on.
Would you like to leave it on permanently?
[0] Yes
[1] No
0
--interactive is now set permanently. All future scala-cli commands will run with the flag set to true.
If you want to turn this setting off at any point, just run `scala-cli config interactive false`.
Found several main classes. Which would you like to run?
[0] ScalaMainClass2
[1] ScalaMainClass1
[2] scripts.ScalaScript_sc
```

You can also configure it manually with the `config` sub-command, by setting the `interactive` property to `true`.
```text
$ scala-cli config interactive true
```
Added in [#1238](https://github.com/VirtusLab/scala-cli/pull/1238) by [Gedochao](https://github.com/Gedochao)

### Other changes

#### Work in progress
* Actionable diagnostics by [lwronski](https://github.com/lwronski) in [#1229](https://github.com/VirtusLab/scala-cli/pull/1229)

#### [SIP-46](https://github.com/scala/improvement-proposals/pull/46)-related
* Restrict directives based on the command used by [romanowski](https://github.com/romanowski) in [#1259](https://github.com/VirtusLab/scala-cli/pull/1259)

#### Documentation
* NIT Improve some website docs by [BlackAnubis7](https://github.com/BlackAnubis7) in [#1243](https://github.com/VirtusLab/scala-cli/pull/1243)

#### Build and internal changes
* Add 0.1.11 release notes to release_notes.md by [BlackAnubis7](https://github.com/BlackAnubis7) in [#1228](https://github.com/VirtusLab/scala-cli/pull/1228)
* Temporary disable test gif by [lwronski](https://github.com/lwronski) in [#1261](https://github.com/VirtusLab/scala-cli/pull/1261)
* aarch64 fixes by [alexarchambault](https://github.com/alexarchambault) in [#1180](https://github.com/VirtusLab/scala-cli/pull/1180)

#### Updates
* Update mill launcher by [alexarchambault](https://github.com/alexarchambault) in [#1269](https://github.com/VirtusLab/scala-cli/pull/1269)
* Update scala-cli.sh launcher for 0.1.11 by [github-actions](https://github.com/features/actions) in [#1230](https://github.com/VirtusLab/scala-cli/pull/1230)
* Update jsoniter-scala-core_2.13 to 2.13.39 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1239](https://github.com/VirtusLab/scala-cli/pull/1239)
* Update trees_2.13 to 4.5.12 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1242](https://github.com/VirtusLab/scala-cli/pull/1242)
* Update jsoniter-scala-core_2.13 to 2.14.2 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1241](https://github.com/VirtusLab/scala-cli/pull/1241)
* Update org name to VirtusLab for downloading scalafmt-native-image by [lwronski](https://github.com/lwronski) in [#1253](https://github.com/VirtusLab/scala-cli/pull/1253)
* Update core_2.13 to 3.7.4 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1247](https://github.com/VirtusLab/scala-cli/pull/1247)
* Update case-app_2.13 to 2.1.0-M15 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1245](https://github.com/VirtusLab/scala-cli/pull/1245)
* Update jsoniter-scala-core_2.13 to 2.15.0 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1246](https://github.com/VirtusLab/scala-cli/pull/1246)
* Update cli-options_2.13, cli_2.13, ... to 0.1.8 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1248](https://github.com/VirtusLab/scala-cli/pull/1248)
* Update metaconfig-typesafe-config to 0.11.1 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1254](https://github.com/VirtusLab/scala-cli/pull/1254)
* Update ammonite to 2.5.4-14-dc4c47bc by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1255](https://github.com/VirtusLab/scala-cli/pull/1255)
* Update coursier-jvm_2.13, ... to 2.1.0-M6-53-gb4f448130 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1256](https://github.com/VirtusLab/scala-cli/pull/1256)
* Update scala-packager-cli_2.13, ... to 0.1.27 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1258](https://github.com/VirtusLab/scala-cli/pull/1258)
* Update bloop-config_2.13 to 1.5.3-sc-1 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1257](https://github.com/VirtusLab/scala-cli/pull/1257)
* Update ammonite to 2.5.4-15-f4a8969b by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1264](https://github.com/VirtusLab/scala-cli/pull/1264)
* Update trees_2.13 to 4.5.13 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1265](https://github.com/VirtusLab/scala-cli/pull/1265)
* Update slf4j-nop to 2.0.0 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1273](https://github.com/VirtusLab/scala-cli/pull/1273)
* Update cli-options_2.13, cli_2.13, ... to 0.1.9 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1272](https://github.com/VirtusLab/scala-cli/pull/1272)
* Bump VirtusLab/scala-cli-setup from 0.1.5 to 0.1.11 by [dependabot](https://docs.github.com/en/code-security/dependabot) in [#1274](https://github.com/VirtusLab/scala-cli/pull/1274)

### New Contributors
* [BlackAnubis7](https://github.com/BlackAnubis7) made their first contribution in [#1228](https://github.com/VirtusLab/scala-cli/pull/1228)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v0.1.11...v0.1.12

## [v0.1.11](https://github.com/VirtusLab/scala-cli/releases/tag/v0.1.11)

### Make `.scalafmt.conf` optional when running the `fmt` command
Scala CLI can now run the `fmt` command without a `.scalafmt.conf` file present. Previously, if such a file was absent, a `Scalafmt requires explicitly specified version.` error was raised while using the `fmt` command.

The Scala CLI `fmt` command now supports passing the `scalafmt` version and dialect directly from the command line, using the `--scalafmt-dialect` and `--scalafmt-version` options respectively:
```
scala-cli fmt --scalafmt-dialect scala3 --scalafmt-version 3.5.8
```
Either of those (or both) can be skipped, which will make Scala CLI infer a default value.

The configuration used can be saved in the workspace by passing the `--save-scalafmt-conf` option.

Added in [#1192](https://github.com/VirtusLab/scala-cli/pull/1192) by [wleczny](https://github.com/wleczny)

### Define `output` option for `package` command with using directives
It is now possible to pass the `output` option of the `package` command with [using directives](guides/introduction/using-directives) instead of passing it directly from bash.

Added in [#1213](https://github.com/VirtusLab/scala-cli/pull/1213) by [wleczny](https://github.com/wleczny)

### Add support for running multiple snippets of the same kind
Scala CLI now allows to pass multiple snippets of the same kind.

It was previously possible to mix different kinds (so to pass a Java snippet alongside a Scala one), but not for example 2 separate Scala snippets. That limitation no longer applies.

When passed this way, each snippet is then treated as a separate input by Scala CLI.

```text
$ scala-cli --scala-snippet '@main def main() = println(Messages.hello)' --scala-snippet 'object Messages { def hello = "Hello" }'
Hello
```
Added in [#1182](https://github.com/VirtusLab/scala-cli/pull/1182) by [Gedochao](https://github.com/Gedochao)

### Add bloop sub-command
Scala CLI now has a (hidden for now) bloop sub-command, that runs a command using the Scala CLI Bloop server (while the mainline Bloop bloop CLI uses its default Bloop server). This is handy when debugging things on Scala CLI for example, allowing one to manually run scala-cli bloop projects or scala-cli bloop compile.

Added in [#1199](https://github.com/VirtusLab/scala-cli/pull/1199) by [alexarchambault](https://github.com/alexarchambault)

### Make main class optional in preamble-less assemblies
It is now allowed to generate an assembly, even for code that has no main class, when `--preamble=false` is passed. This can be useful for libraries, if users want to pass the assembly to tools such as proguard. This also accepts a (hidden) `--main-class-in-manifest=false` option if users want not only no preamble, but also no mention of main class in the assembly manifest (`META-INF/MANIFEST.MF` in the assembly JAR). The latter option is useful for tools, such as the hadoop jar command, that behave differently depending on the presence or not of a main class in the manifest.

Added in [#1200](https://github.com/VirtusLab/scala-cli/pull/1200) by [alexarchambault](https://github.com/alexarchambault)

### Important fixes & enhancements
#### Prevent erroneous using directives from blocking the initial run of BSP
Up till now, running the `setup-ide` sub-command on sources containing `using directives` with syntax errors or pointing to dependencies which could not be fetched would create a `BSP` setup which could not be imported correctly by IDEs. This is no longer the case and such a `BSP` connection should now import correctly, so that it's possible to fix the faulty code within the comfort of one's IDE of choice.

This fixes [#1097](https://github.com/VirtusLab/scala-cli/issues/1097)

Added in [#1195](https://github.com/VirtusLab/scala-cli/pull/1195) by [Gedochao](https://github.com/Gedochao)

### Work in progress
#### Allow to globally turn actionable diagnostics on or off
It is now possible to globally enable or disable actionable diagnostics using the `config` sub-command.

The relevant configuration is under the `actions` key.
```text
$ scala-cli config actions true
```

Added in [#1193](https://github.com/VirtusLab/scala-cli/pull/1193) by [lwronski](https://github.com/lwronski)

#### Publishing-related features
* Add "publish setup" command by [alexarchambault](https://github.com/alexarchambault) in [#926](https://github.com/VirtusLab/scala-cli/pull/926)

### Other changes
#### Documentation
* Put the release notes doc on the website by [Gedochao](https://github.com/Gedochao) in [#1196](https://github.com/VirtusLab/scala-cli/pull/1196)
* Fix typo in Spark docs by [alexarchambault](https://github.com/alexarchambault) in [#1183](https://github.com/VirtusLab/scala-cli/pull/1183)
* Tweak issue templates & the release procedure by [Gedochao](https://github.com/Gedochao) in [#1188](https://github.com/VirtusLab/scala-cli/pull/1188)
* Add install and uninstall completions documentation by [wleczny](https://github.com/wleczny) in [#1201](https://github.com/VirtusLab/scala-cli/pull/1201)

#### Build and internal changes
* ignore *.semanticdb files by [mtk](https://github.com/mtk) in [#1187](https://github.com/VirtusLab/scala-cli/pull/1187)
* Update scala-cli.sh launcher for 0.1.10 by [github-actions](https://github.com/features/actions) in [#1185](https://github.com/VirtusLab/scala-cli/pull/1185)
* Force push updating scala-cli in scala-cli-setup by [lwronski](https://github.com/lwronski) in [#1189](https://github.com/VirtusLab/scala-cli/pull/1189)
* Fix running scala check in scala native by [lwronski](https://github.com/lwronski) in [#1190](https://github.com/VirtusLab/scala-cli/pull/1190)
* Use manifest JARs in "run" command if needed by [alexarchambault](https://github.com/alexarchambault) in [#1198](https://github.com/VirtusLab/scala-cli/pull/1198)
* Use more lightweight Spark distribs in spark tests by [alexarchambault](https://github.com/alexarchambault) in [#1207](https://github.com/VirtusLab/scala-cli/pull/1207)
* Update GraalVM to 22.2.0 by [alexarchambault](https://github.com/alexarchambault) in [#1208](https://github.com/VirtusLab/scala-cli/pull/1208)
* Split integration tests by [alexarchambault](https://github.com/alexarchambault) in [#1202](https://github.com/VirtusLab/scala-cli/pull/1202)
* Debug macOS CI issue on CI by [alexarchambault](https://github.com/alexarchambault) in [#1215](https://github.com/VirtusLab/scala-cli/pull/1215)
* Update docusaurus to 2.0.0-rc.1 by [lwronski](https://github.com/lwronski) in [#1224](https://github.com/VirtusLab/scala-cli/pull/1224)

#### Updates
* Update core_2.13 to 3.7.0 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1186](https://github.com/VirtusLab/scala-cli/pull/1186)
* Update core_2.13 to 3.7.1 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1194](https://github.com/VirtusLab/scala-cli/pull/1194)
* Update jsoniter-scala-core_2.13 to 2.13.37 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1197](https://github.com/VirtusLab/scala-cli/pull/1197)
* Update jsoniter-scala-core_2.13 to 2.13.38 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1217](https://github.com/VirtusLab/scala-cli/pull/1217)
* Update ammonite to 2.5.4-13-1ebd00a6 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1218](https://github.com/VirtusLab/scala-cli/pull/1218)
* Update core_2.13 to 3.7.2 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1219](https://github.com/VirtusLab/scala-cli/pull/1219)
* Update scala-collection-compat to 2.8.1 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1221](https://github.com/VirtusLab/scala-cli/pull/1221)
* Update trees_2.13 to 4.5.11 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1222](https://github.com/VirtusLab/scala-cli/pull/1222)
* Update coursier-jvm_2.13, ... to 2.1.0-M6-49-gff26f8e39 by [scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1223](https://github.com/VirtusLab/scala-cli/pull/1223)

**Full Changelog**: [https://github.com/VirtusLab/scala-cli/compare/v0.1.10...v0.1.11](https://github.com/VirtusLab/scala-cli/compare/v0.1.10...v0.1.11)


## [v0.1.10](https://github.com/VirtusLab/scala-cli/releases/tag/v0.1.10)

### Initial support for importing other sources via `using` directives

It is now possible to add sources to a Scala CLI project from a source file, with `using file` directives:

```scala
//> using file "Other.scala"
//> using file "extra/"
```

Note that several sources can be specified in a single directive

```scala
//> using file "Other.scala" "extra/"
```

Added in [#1157](https://github.com/VirtusLab/scala-cli/pull/1157) by [lwronski](https://github.com/lwronski).

### Add `dependency update` sub-command

Scala CLI can now update dependencies in user projects, using the `dependency-update` sub-command, like

```text
scala-cli dependency-update --all .
```

When updates are available, this sub-command asks whether to update each of those, right where these dependencies are
defined.

Added in [#1055](https://github.com/VirtusLab/scala-cli/pull/1055) by [lwronski](https://github.com/lwronski).

### Running snippets passed as arguments

Scala CLI can now run Scala or Java code passed on the command-line, via `-e` / `--script-snippet` / `--scala-snippet`
/ `--java-snippet`:

```text
$ scala-cli -e 'println("Hello")'
Hello

$ scala-cli --script-snippet 'println("Hello")'
Hello

$ scala-cli --scala-snippet '@main def run() = println("Hello")'
Hello

$ scala-cli --java-snippet 'public class Main { public static void main(String[] args) { System.out.println("Hello"); } }'
Hello
```

These options are meant to be substitutes to the `-e` option of the `scala` script that ships in scalac archives.

Added in [#1166](https://github.com/VirtusLab/scala-cli/pull/1166) by [Gedochao](https://github.com/Gedochao).

### Uninstall instructions and `uninstall` sub-command

Uninstalling Scala CLI is now documented in the main installation page, right after the installation instructions. In
particular, when installed via
the [installation script](https://github.com/VirtusLab/scala-cli-packages/blob/main/scala-setup.sh), Scala CLI can be
uninstalled via a newly added `uninstall` sub-command.

Added in [#1122](https://github.com/VirtusLab/scala-cli/pull/1122) and #1152 by [wleczny](https://github.com/wleczny).

### Important fixes & enhancements

#### ES modules

Scala CLI now supports the ES Scala.js module kind, that can be enabled via a `//> using jsModuleKind "esmodule"`
directive, allowing to import other ES modules in particular.

Added in [#1142](https://github.com/VirtusLab/scala-cli/pull/1142)
by [hugo-vrijswijk](https://github.com/hugo-vrijswijk).

#### Putting Java options in assemblies, launchers, and docker images, in `package` sub-command

Passing `--java-opt` and `--java-prop` options to the `package` sub-command is now allowed. The passed options are
hard-coded in the generated assemblies or launchers, and in docker images.

Added in [#1167](https://github.com/VirtusLab/scala-cli/pull/1167) by [wleczny](https://github.com/wleczny).

#### `--command` and `--scratch-dir` options in `run` sub-command

The `run` sub-command can now print the command it would have run, rather than running it. This can be useful for
debugging purposes, or if users want to manually tweak commands right before they are run. Pass `--command` to run to
enable it. This prints one argument per line, for easier automated processing:

```text
$ scala-cli run --command -e 'println("Hello")' --runner=false
~/Library/Caches/Coursier/arc/https/github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.2%252B8/OpenJDK17U-jdk_x64_mac_hotspot_17.0.2_8.tar.gz/jdk-17.0.2+8/Contents/Home/bin/java
-cp
~/Library/Caches/ScalaCli/virtual-projects/ee/project-3c6fdea1/.scala-build/project_ed4bea6d06_ed4bea6d06/classes/main:~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala3-library_3/3.1.3/scala3-library_3-3.1.3.jar:~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.8/scala-library-2.13.8.jar
snippet_sc
```

When `run` relies on temporary files (when Scala.js is used for example), one can pass a temporary directory
via `--scratch-dir`, so that temporary files are kept even when `scala-cli` doesn't run anymore:

```text
$ scala-cli run --command -e 'println("Hello")' --js --runner=false --scratch-dir ./tmp
node
./tmp/main1690571004533525773.js
```

Added in [#1163](https://github.com/VirtusLab/scala-cli/pull/1163) by
by [alexarchambault](https://github.com/alexarchambault).

#### Don't put Scala CLI internal modules in packages

Scala CLI doesn't put anymore its stubs module and its "runner" module in generated packages, in the `package`
sub-command.

Fixed in [#1161](https://github.com/VirtusLab/scala-cli/pull/1161)
by [alexarchambault](https://github.com/alexarchambault).

#### Don't write preambles in generated assemblies in the `package` sub-command

Passing `--preamble=false` to `scala-cli --power package --assembly` makes it generate assemblies without a shell preamble. As a
consequence, these assemblies cannot be made executable, but these look more like "standard" JARs, which is required in
some contexts.

Fixed in [#1161](https://github.com/VirtusLab/scala-cli/pull/1161)
by [alexarchambault](https://github.com/alexarchambault).

#### Don't put some dependencies in generated assemblies in the `package` sub-command

Some dependencies, alongside all their transitive dependencies, can be excluded from the generated assemblies.
Pass `--provided org:name` to `scala-cli --power package --assembly` to remove a dependency, like

```text
$ scala-cli --power package SparkJob.scala --assembly --provided org.apache.spark::spark-sql
```

Note that unlike "provided" dependencies in sbt, and compile-time dependencies in Mill, all transitive dependencies are
excluded from the assembly. In the Spark example above, for example, as `spark-sql` depends on `scala-library` (the
Scala standard library), the latter gets excluded from the assembly too (which works fine in the context of Spark jobs).

Fixed in [#1161](https://github.com/VirtusLab/scala-cli/pull/1161)
by [alexarchambault](https://github.com/alexarchambault).

### In progress

#### Experimental Spark capabilities

The `package` sub-command now accepts a `--spark` option, to generate assemblies for Spark jobs, ready to be passed
to `spark-submit`. This option is hidden (not printed in `scala-cli --power package --help`, only in `--help-full`), and should
be considered experimental.

See [this document](https://github.com/VirtusLab/scala-cli/blob/410f54c01ac5d9cb046461dce07beb5aa008231e/website/src/pages/spark.md)
for more details about these experimental Spark features.

Added in [#1086](https://github.com/VirtusLab/scala-cli/pull/1086)
by [alexarchambault](https://github.com/alexarchambault).

### Other changes

#### Documentation

* Add cookbooks for working with Scala CLI in IDEA IntelliJ by [Gedochao](https://github.com/Gedochao)
  in [#1149](https://github.com/VirtusLab/scala-cli/pull/1149)
* Fix VL branding by [lwronski](https://github.com/lwronski)
  in [#1151](https://github.com/VirtusLab/scala-cli/pull/1151)
* Back port of documentation changes to main by [github-actions](https://github.com/features/actions)
  in [#1154](https://github.com/VirtusLab/scala-cli/pull/1154)
* Update using directive syntax in scenarios by [lwronski](https://github.com/lwronski)
  in [#1159](https://github.com/VirtusLab/scala-cli/pull/1159)
* Back port of documentation changes to main by [github-actions](https://github.com/features/actions)
  in [#1165](https://github.com/VirtusLab/scala-cli/pull/1165)
* Add docs depedency-update by [lwronski](https://github.com/lwronski)
  in [#1178](https://github.com/VirtusLab/scala-cli/pull/1178)
* Add docs how to install scala-cli via choco by [lwronski](https://github.com/lwronski)
  in [#1179](https://github.com/VirtusLab/scala-cli/pull/1179)

#### Build and internal changes

* Update scala-cli.sh launcher for 0.1.9 by [github-actions](https://github.com/features/actions)
  in [#1144](https://github.com/VirtusLab/scala-cli/pull/1144)
* Update release procedure by [wleczny](https://github.com/wleczny)
  in [#1156](https://github.com/VirtusLab/scala-cli/pull/1156)
* chore(ci): add in mill-github-dependency-graph by [ckipp01](https://github.com/ckipp01)
  in [#1164](https://github.com/VirtusLab/scala-cli/pull/1164)
* chore(ci): bump version of mill-github-dependency-graph by [ckipp01](https://github.com/ckipp01)
  in [#1171](https://github.com/VirtusLab/scala-cli/pull/1171)
* Use Scala CLI 0.1.9 in build by [alexarchambault](https://github.com/alexarchambault)
  in [#1173](https://github.com/VirtusLab/scala-cli/pull/1173)
* Stop compiling most stuff with Scala 2 by [alexarchambault](https://github.com/alexarchambault)
  in [#1113](https://github.com/VirtusLab/scala-cli/pull/1113)
* Turn the sip mode also for `scala-cli-sip` binary by [romanowski](https://github.com/romanowski)
  in [#1168](https://github.com/VirtusLab/scala-cli/pull/1168)
* chore(ci): use mill-dependency-submission action by [ckipp01](https://github.com/ckipp01)
  in [#1174](https://github.com/VirtusLab/scala-cli/pull/1174)
* Fix snippet tests for Windows by [Gedochao](https://github.com/Gedochao)
  in [#1172](https://github.com/VirtusLab/scala-cli/pull/1172)

#### Updates

* Update mill-main to 0.10.5 by [scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#1148](https://github.com/VirtusLab/scala-cli/pull/1148)
* Update snailgun-core, snailgun-core_2.13 to 0.4.1-sc2
  by [scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#1155](https://github.com/VirtusLab/scala-cli/pull/1155)
* Update jsoniter-scala-core_2.13 to 2.13.35 by [scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#1169](https://github.com/VirtusLab/scala-cli/pull/1169)
* Update scala-collection-compat to 2.8.0 by [scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#1170](https://github.com/VirtusLab/scala-cli/pull/1170)
* Update jsoniter-scala-core_2.13 to 2.13.36 by [scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#1175](https://github.com/VirtusLab/scala-cli/pull/1175)

### New Contributors

* [hugo-vrijswijk](https://github.com/hugo-vrijswijk) made their first contribution
  in [#1142](https://github.com/VirtusLab/scala-cli/pull/1142)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v0.1.9...v0.1.10

## [v0.1.9](https://github.com/VirtusLab/scala-cli/releases/tag/v0.1.9)

### `--list-main-classes` for `publish` & `package`

`publish` and `package` sub-commands now support the `--list-main-classes` option, which allows to list all the
available main classes. Previously it was only available in the `run` command.

Added in [#1118](https://github.com/VirtusLab/scala-cli/pull/1118) by [Gedochao](https://github.com/Gedochao)

### Important fixes & enhancements

#### `fmt` options improvement

Added missing documentation on how to pass native `scalafmt` options in the `fmt` sub-command with the `-F` option.

```
$ scala-cli fmt -F --version
scalafmt 3.5.2
```

Additionally, a couple of `scalafmt`'s native options received aliases in Scala CLI:

`--respect-project-filters` is an alias for `-F --respect-project-filters`. Because of the way sources are passed by
Scala CLI to `scalafmt` under the hood, we now turn it on by default to respect any `project.excludePaths` settings in
the user's `.scalafmt.conf`.
It can be disabled by passing `--respect-project-filters=false` to revert to previous behaviour.
This addresses [#1121](https://github.com/VirtusLab/scala-cli/issues/1121)

`--scalafmt-help` is an alias for `-F --help`. It shows the `--help` output from `scalafmt`, which might prove as
helpful reference when in need of using native `scalafmt` options with `-F`.

Added in [#1135](https://github.com/VirtusLab/scala-cli/pull/1135) by [Gedochao](https://github.com/Gedochao)

#### Include `libsodium.dll` on Windows

Static linking of libsodium in Windows launcher has been fixed.
This addresses [#1114](https://github.com/VirtusLab/scala-cli/issues/1114)

Added in [#1115](https://github.com/VirtusLab/scala-cli/pull/1115)
by [alexarchambault](https://github.com/alexarchambault)

#### Force interactive mode for `update` command

Interactive mode for `update` sub-command is now enabled by default.

Added in [#1100](https://github.com/VirtusLab/scala-cli/pull/1100) by [lwronski](https://github.com/lwronski)

### In progress

#### Publishing-related features

* Publish tweaks + documentation by [alexarchambault](https://github.com/alexarchambault)
  in [#1107](https://github.com/VirtusLab/scala-cli/pull/1107)

#### Better BSP support for Scala scripts

* Add scala-sc language to BSP supported languages by [alexarchambault](https://github.com/alexarchambault)
  in [#1140](https://github.com/VirtusLab/scala-cli/pull/1140)

### Other changes

#### Documentation PRs

* Update scala 2.12 to 2.12.16 in docs by [lwronski](https://github.com/lwronski)
  in [#1108](https://github.com/VirtusLab/scala-cli/pull/1108)
* Back port of documentation changes to main by [github-actions](https://github.com/features/actions)
  in [#1111](https://github.com/VirtusLab/scala-cli/pull/1111)
* Tweak release procedure by [Gedochao](https://github.com/Gedochao)
  in [#1112](https://github.com/VirtusLab/scala-cli/pull/1112)

#### Build and internal changes

* Add choco configuration files by [lwronski](https://github.com/lwronski)
  in [#998](https://github.com/VirtusLab/scala-cli/pull/998)
* Tweaking by [alexarchambault](https://github.com/alexarchambault)
  in [#1105](https://github.com/VirtusLab/scala-cli/pull/1105)
* Add scala-cli-setup deploy key to ssh-agent by [lwronski](https://github.com/lwronski)
  in [#1117](https://github.com/VirtusLab/scala-cli/pull/1117)

#### Updates

* Update scala-cli.sh launcher for 0.1.8 by [github-actions](https://github.com/features/actions)
  in [#1106](https://github.com/VirtusLab/scala-cli/pull/1106)
* Update case-app to 2.1.0-M14 by [alexarchambault](https://github.com/alexarchambault)
  in [#1120](https://github.com/VirtusLab/scala-cli/pull/1120)
* Update Scala to 3.1.3 by [alexarchambault](https://github.com/alexarchambault)
  in [#1124](https://github.com/VirtusLab/scala-cli/pull/1124)
* Update jsoniter-scala-core_2.13 to 2.13.32 by [scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#1125](https://github.com/VirtusLab/scala-cli/pull/1125)
* Update coursier-jvm_2.13, ... to 2.1.0-M6-28-gbad85693f
  by [scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#1126](https://github.com/VirtusLab/scala-cli/pull/1126)
* Update libsodiumjni to 0.0.3 by [scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#1127](https://github.com/VirtusLab/scala-cli/pull/1127)
* Update org.eclipse.jgit to 6.2.0.202206071550-r by [scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#1128](https://github.com/VirtusLab/scala-cli/pull/1128)
* Update Scala.js to 1.10.1 by [scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#1130](https://github.com/VirtusLab/scala-cli/pull/1130)
* Update Scala Native to 0.4.5 by [alexarchambault](https://github.com/alexarchambault)
  in [#1133](https://github.com/VirtusLab/scala-cli/pull/1133)
* Update scala-js-cli to 1.1.1-sc5 by [alexarchambault](https://github.com/alexarchambault)
  in [#1134](https://github.com/VirtusLab/scala-cli/pull/1134)
* Update jsoniter-scala-core_2.13 to 2.13.33 by [scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#1136](https://github.com/VirtusLab/scala-cli/pull/1136)
* Update `scalafmt`  to 3.5.8 by [Gedochao](https://github.com/Gedochao)
  in [#1137](https://github.com/VirtusLab/scala-cli/pull/1137)
* Update cli-options_2.13, cli_2.13, ... to 0.1.7 by [scala-steward](https://github.com/scala-steward-org/scala-steward)
  in [#1138](https://github.com/VirtusLab/scala-cli/pull/1138)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v0.1.8...v0.1.9

## [v0.1.8](https://github.com/VirtusLab/scala-cli/releases/tag/v0.1.8)

### `--list-main-classes` option for the `run` command

You can pass the option `--list-main-classes` to the `run` command to list all the available main classes, including
scripts.

```
$ scala-cli . --list-main-classes
Hello scripts.AnotherScript_sc scripts.Script_sc
```

Added in [#1095](https://github.com/VirtusLab/scala-cli/pull/1095) by [Gedochao](https://github.com/Gedochao)

### Add `config` command

The `config` sub-command allows to get and set various configuration values, intended for use by
other Scala CLI sub-commands.

This feature has been added in preparation for the `publish` command, stay tuned for future announcements.

Added in [#1056](https://github.com/VirtusLab/scala-cli/pull/1056)
by [alexarchambault](https://github.com/alexarchambault)

### Prioritise non-script main classes

When trying to run a directory containing scripts and just a single non-script main class, the non-script main class
will now be prioritised and run by default.

```
$ scala-cli .
Running Hello. Also detected script main classes: scripts.AnotherScript_sc, scripts.Script_sc
You can run any one of them by passing option --main-class, i.e. --main-class scripts.AnotherScript_sc
All available main classes can always be listed by passing option --list-main-classes
Hello world
```

Changed in [#1095](https://github.com/VirtusLab/scala-cli/pull/1095) by [Gedochao](https://github.com/Gedochao)

### Important bugfixes

#### Accept latest Scala versions despite stale Scala version listings in cache

Scala CLI uses version listings from Maven Central to check if a Scala version is valid. When new Scala versions are
released, users could sometimes have stale version listings in their Coursier cache for a short period of time (the
Coursier cache TTL, which is 24 hours by default). This prevented these users to use new Scala versions during that
time.
To work around that, Scala CLI now tries to re-download version listings when they don't have the requested Scala
version.
This addresses [#1090](https://github.com/VirtusLab/scala-cli/issues/1090)

Fixed in [#1096](https://github.com/VirtusLab/scala-cli/pull/1096) by [lwronski](https://github.com/lwronski)

#### Bloop now uses `JAVA_HOME` by default

Bloop should now pick up the JDK available in `JAVA_HOME`. It was formerly necessary to pass `--bloop-jvm system`
explicitly. This addresses [#1102](https://github.com/VirtusLab/scala-cli/issues/1102)

Fixed in [#1084](https://github.com/VirtusLab/scala-cli/pull/1084) by [lwronski](https://github.com/lwronski)

#### The `-coverage-out` option now accepts relative paths

Scala CLI now correctly processes relative paths when passed to the `-coverage-out` option. Formerly,
the `scoverage.coverage` file would not be properly generated when a relative path was passed.
This addresses [#1072](https://github.com/VirtusLab/scala-cli/issues/1072)

Fixed in [#1080](https://github.com/VirtusLab/scala-cli/pull/1080) by [lwronski](https://github.com/lwronski)

### Other changes

#### Documentation PRs

* Improve scripts guide by [Gedochao](https://github.com/Gedochao)
  in [#1074](https://github.com/VirtusLab/scala-cli/pull/1074)
* Update installation instructions for Nix by [kubukoz](https://github.com/kubukoz)
  in [#1082](https://github.com/VirtusLab/scala-cli/pull/1082)
* Tweak docs by [alexarchambault](https://github.com/alexarchambault)
  in [#1085](https://github.com/VirtusLab/scala-cli/pull/1085)
* Some typos & rewording on the single-module projects use case page by [Baccata](https://github.com/Baccata)
  in [#1089](https://github.com/VirtusLab/scala-cli/pull/1089)

#### Fixes

* Add suffix to project name which contains virtual files by [lwronski](https://github.com/lwronski)
  in [#1070](https://github.com/VirtusLab/scala-cli/pull/1070)

#### Build and internal changes

* Update scala-cli.sh launcher for 0.1.7 by [github-actions](https://github.com/features/actions)
  in [#1076](https://github.com/VirtusLab/scala-cli/pull/1076)
* Tweaking by [alexarchambault](https://github.com/alexarchambault)
  in [#1087](https://github.com/VirtusLab/scala-cli/pull/1087)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v0.1.7...v0.1.8

## Older versions

The release notes for all the past versions of Scala CLI can be viewed
on [our releases page on GitHub](https://github.com/VirtusLab/scala-cli/releases).
