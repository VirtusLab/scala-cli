---
title: Release notes
sidebar_position: 99
---
import {ChainedSnippets} from "../src/components/MarkdownComponents.js";
import ReactPlayer from 'react-player'


# Release notes

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
* Back port of documentation changes to main by @github-actions in [#2569](https://github.com/VirtusLab/scala-cli/pull/2569)
* Document --python flag by [@MaciejG604](https://github.com/MaciejG604) in [#2574](https://github.com/VirtusLab/scala-cli/pull/2574)
* Document publishing process configuration by [@MaciejG604](https://github.com/MaciejG604) in [#2580](https://github.com/VirtusLab/scala-cli/pull/2580)
* Back port of documentation changes to main by @github-actions in [#2593](https://github.com/VirtusLab/scala-cli/pull/2593)

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
* Update scala-cli.sh launcher for 1.0.6 by @github-actions in [#2542](https://github.com/VirtusLab/scala-cli/pull/2542)
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

```bash
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

```bash 
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

```bash
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

```bash
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
* Add weaver test framework instruction by @lenguyenthanh in [#2021](https://github.com/VirtusLab/scala-cli/pull/2021)

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
#   scala-cli config power true.
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
scala-cli -e 'println("Hello")' --native --native-lto thin
```

Or with a `using` directive:

```scala compile
//> using platform "scala-native"
//> using nativeLto "thin"
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
* Update scala-cli.sh launcher for 0.2.0 by @github-actions in [#1881](https://github.com/VirtusLab/scala-cli/pull/1881)
* Back port of documentation changes to main by @github-actions in [#1911](https://github.com/VirtusLab/scala-cli/pull/1911)


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
* Back port of documentation changes to main by @github-actions in [#1860](https://github.com/VirtusLab/scala-cli/pull/1860)
* Run generate reference doc as non sip by [@lwronski](https://github.com/lwronski) in [#1866](https://github.com/VirtusLab/scala-cli/pull/1866)
* Bump `case-app` to `2.1.0-M23` by [@lwronski](https://github.com/lwronski) in [#1868](https://github.com/VirtusLab/scala-cli/pull/1868)

#### Documentation updates
* Update docker example command by [@MaciejG604](https://github.com/MaciejG604) in [#1798](https://github.com/VirtusLab/scala-cli/pull/1798)
* Tweak `--watch`/`--restart` disambiguation in the help messages & docs by [@Gedochao](https://github.com/Gedochao) in [#1819](https://github.com/VirtusLab/scala-cli/pull/1819)
* Release notes - msi malware analysis by [@lwronski](https://github.com/lwronski) in [#1832](https://github.com/VirtusLab/scala-cli/pull/1832)
* Improve 'shebang' help message wrt program *arguments* by [@Flowdalic](https://github.com/flowdalic) in [#1829](https://github.com/VirtusLab/scala-cli/pull/1829)
* docs: Fix Yum manual installation step by [@tgodzik](https://github.com/tgodzik) in [#1850](https://github.com/VirtusLab/scala-cli/pull/1850)

#### Updates & maintenance
* Update scala-cli.sh launcher for 0.1.20 by @github-actions in [#1790](https://github.com/VirtusLab/scala-cli/pull/1790)
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
* Back port of documentation changes to main by @github-actions in [#1735](https://github.com/VirtusLab/scala-cli/pull/1735)
* Explain the differences in using shebang vs scala-cli directly in script by [@lwronski](https://github.com/lwronski) in [#1740](https://github.com/VirtusLab/scala-cli/pull/1740)
* Add instruction for Intellij JVM version by [@MaciejG604](https://github.com/MaciejG604) in [#1773](https://github.com/VirtusLab/scala-cli/pull/1773)
* Fix a broken link by [@xerial](https://github.com/xerial) and [@lwronski](https://github.com/lwronski) in [#1777](https://github.com/VirtusLab/scala-cli/pull/1777)

#### Updates & maintenance
* Update svm to 22.3.0 by [@scala-steward](https://github.com/scala-steward-org/scala-steward) in [#1689](https://github.com/VirtusLab/scala-cli/pull/1689)
* Update scala-cli.sh launcher for 0.1.19 by @github-actions in [#1707](https://github.com/VirtusLab/scala-cli/pull/1707)
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
* Update scala-cli.sh launcher for 0.1.18 by @github-actions in [#1624](https://github.com/VirtusLab/scala-cli/pull/1624)
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
* Update scala-cli.sh launcher for 0.1.17 by @github-actions in [#1564](https://github.com/VirtusLab/scala-cli/pull/1564)
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
//> using jvm "adopt:11"
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
* Update scala-cli.sh launcher for 0.1.16 by @github-actions in [#1458](https://github.com/VirtusLab/scala-cli/pull/1458)
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
* Update scala-cli.sh launcher for 0.1.15 by @github-actions in [#1401](https://github.com/VirtusLab/scala-cli/pull/1401)
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

* Update scala-cli.sh launcher for 0.1.14 by @github-actions in [#1362](https://github.com/VirtusLab/scala-cli/pull/1362)
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
* Update scala-cli.sh launcher for 0.1.13 by @github-actions in [#1351](https://github.com/VirtusLab/scala-cli/pull/1351)
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

This change was added by [@Gedochao](https://github.com/Gedochao) in [#1268]( https://github.com/VirtusLab/scala-cli/pull/1268)

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

Added in [#1260]( https://github.com/VirtusLab/scala-cli/pull/1260) by [@wleczny](https://github.com/wleczny)

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

Added in [#812]( https://github.com/VirtusLab/scala-cli/pull/812) by [@jchyb](https://github.com/jchyb)

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

Added in [#1313]( https://github.com/VirtusLab/scala-cli/pull/1313) by [@Gedochao](https://github.com/Gedochao)

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

Added in [#1268]( https://github.com/VirtusLab/scala-cli/pull/1268) by [@Gedochao](https://github.com/Gedochao)

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

Added in [#1295]( https://github.com/VirtusLab/scala-cli/pull/1295) by [@alexarchambault](https://github.com/alexarchambault)

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
