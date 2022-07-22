# [v0.1.10](https://github.com/VirtusLab/scala-cli/releases/tag/v0.1.10)

## Initial support for importing other sources via `using` directives

It is now possible to add sources to a Scala CLI project from a source file, with `using file` directives:
```scala
//> using file "Other.scala"
//> using file "extra/"
```

Note that several sources can be specified in a single directive
```scala
//> using file "Other.scala" "extra/"
```

Added in [#1157](https://github.com/VirtusLab/scala-cli/pull/1157) by @lwronski.

## Add `dependency update` sub-command

Scala CLI can now update dependencies in user projects, using the `dependency-update` sub-command, like
```text
scala-cli dependency-update --all .
```
When updates are available, this sub-command asks whether to update each of those, right where these dependencies are defined.

Added in [#1055](https://github.com/VirtusLab/scala-cli/pull/1055) by @lwronski.

## Running snippets passed as arguments

Scala CLI can now run Scala or Java code passed on the command-line, via `-e` / `--script-snippet` / `--scala-snippet` / `--java-snippet`:
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

Added in [#1166](https://github.com/VirtusLab/scala-cli/pull/1166) by @Gedochao.

## Uninstall instructions and `uninstall` sub-command

Uninstalling Scala CLI is now documented in the main installation page, right after the installation instructions. In particular, when installed via the [installation script](https://github.com/VirtusLab/scala-cli-packages/blob/main/scala-setup.sh), Scala CLI can be uninstalled via a newly added `uninstall` sub-command.

Added in [#1122](https://github.com/VirtusLab/scala-cli/pull/1122) and #1152 by @wleczny.

## Important fixes & enhancements

### ES modules

Scala CLI now supports the ES Scala.js module kind, that can be enabled via a `//> using jsModuleKind "esmodule"` directive, allowing to import other ES modules in particular.

Added in [#1142](https://github.com/VirtusLab/scala-cli/pull/1142) by @hugo-vrijswijk.

### Putting Java options in assemblies, launchers, and docker images, in `package` sub-command

Passing `--java-opt` and `--java-prop` options to the `package` sub-command is now allowed. The passed options are
hard-coded in the generated assemblies or launchers, and in docker images.

Added in [#1167](https://github.com/VirtusLab/scala-cli/pull/1167) by @wleczny.

### `--command` and `--scratch-dir` options in `run` sub-command

The `run` sub-command can now print the command it would have run, rather than running it. This can be useful for debugging purposes, or if users want to manually tweak commands right before they are run. Pass `--command` to run to enable it. This prints one argument per line, for easier automated processing:
```text
$ scala-cli run --command -e 'println("Hello")' --runner=false
~/Library/Caches/Coursier/arc/https/github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.2%252B8/OpenJDK17U-jdk_x64_mac_hotspot_17.0.2_8.tar.gz/jdk-17.0.2+8/Contents/Home/bin/java
-cp
~/Library/Caches/ScalaCli/virtual-projects/ee/project-3c6fdea1/.scala-build/project_ed4bea6d06_ed4bea6d06/classes/main:~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala3-library_3/3.1.3/scala3-library_3-3.1.3.jar:~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.8/scala-library-2.13.8.jar
snippet_sc
```

When `run` relies on temporary files (when Scala.js is used for example), one can pass a temporary directory via `--scratch-dir`, so that temporary files are kept even when `scala-cli` doesn't run anymore:
```text
$ scala-cli run --command -e 'println("Hello")' --js --runner=false --scratch-dir ./tmp
node
./tmp/main1690571004533525773.js
```

Added in [#1163](https://github.com/VirtusLab/scala-cli/pull/1163) by by @alexarchambault.

### Don't put Scala CLI internal modules in packages

Scala CLI doesn't put anymore its stubs module and its "runner" module in generated packages, in the `package` sub-command.

Fixed in [#1161](https://github.com/VirtusLab/scala-cli/pull/1161) by @alexarchambault.

### Don't write preambles in generated assemblies in the `package` sub-command

Passing `--preamble=false` to `scala-cli package --assembly` makes it generate assemblies without a shell preamble. As a consequence, these assemblies cannot be made executable, but these look more like "standard" JARs, which is required in some contexts.

Fixed in [#1161](https://github.com/VirtusLab/scala-cli/pull/1161) by @alexarchambault.

### Don't put some dependencies in generated assemblies in the `package` sub-command

Some dependencies, alongside all their transitive dependencies, can be excluded from the generated assemblies. Pass `--provided org:name` to `scala-cli package --assembly` to remove a dependency, like
```text
$ scala-cli package SparkJob.scala --assembly --provided org.apache.spark::spark-sql
```

Note that unlike "provided" dependencies in sbt, and compile-time dependencies in Mill, all transitive dependencies are excluded from the assembly. In the Spark example above, for example, as `spark-sql` depends on `scala-library` (the Scala standard library), the latter gets excluded from the assembly too (which works fine in the context of Spark jobs).

Fixed in [#1161](https://github.com/VirtusLab/scala-cli/pull/1161) by @alexarchambault.

## In progress

### Experimental Spark capabilities

The `package` sub-command now accepts a `--spark` option, to generate assemblies for Spark jobs, ready to be passed to `spark-submit`. This option is hidden (not printed in `scala-cli package --help`, only in `--help-full`), and should be considered experimental.

See [this document](https://github.com/VirtusLab/scala-cli/blob/410f54c01ac5d9cb046461dce07beb5aa008231e/website/src/pages/spark.md) for more details about these experimental Spark features.

Added in [#1086](https://github.com/VirtusLab/scala-cli/pull/1086) by @alexarchambault.

## Other changes

### Documentation
* Add cookbooks for working with Scala CLI in IDEA IntelliJ by @Gedochao in [#1149](https://github.com/VirtusLab/scala-cli/pull/1149)
* Fix VL branding by @lwronski in [#1151](https://github.com/VirtusLab/scala-cli/pull/1151)
* Back port of documentation changes to main by @github-actions in [#1154](https://github.com/VirtusLab/scala-cli/pull/1154)
* Update using directive syntax in scenarios by @lwronski in [#1159](https://github.com/VirtusLab/scala-cli/pull/1159)
* Back port of documentation changes to main by @github-actions in [#1165](https://github.com/VirtusLab/scala-cli/pull/1165)
* Add docs depedency-update by @lwronski in [#1178](https://github.com/VirtusLab/scala-cli/pull/1178)
* Add docs how to install scala-cli via choco by @lwronski in [#1179](https://github.com/VirtusLab/scala-cli/pull/1179)

### Build and internal changes
* Update scala-cli.sh launcher for 0.1.9 by @github-actions in [#1144](https://github.com/VirtusLab/scala-cli/pull/1144)
* Update release procedure by @wleczny in [#1156](https://github.com/VirtusLab/scala-cli/pull/1156)
* chore(ci): add in mill-github-dependency-graph by @ckipp01 in [#1164](https://github.com/VirtusLab/scala-cli/pull/1164)
* chore(ci): bump version of mill-github-dependency-graph by @ckipp01 in [#1171](https://github.com/VirtusLab/scala-cli/pull/1171)
* Use Scala CLI 0.1.9 in build by @alexarchambault in [#1173](https://github.com/VirtusLab/scala-cli/pull/1173)
* Stop compiling most stuff with Scala 2 by @alexarchambault in [#1113](https://github.com/VirtusLab/scala-cli/pull/1113)
* Turn the sip mode also for `scala-cli-sip` binary by @romanowski in [#1168](https://github.com/VirtusLab/scala-cli/pull/1168)
* chore(ci): use mill-dependency-submission action by @ckipp01 in [#1174](https://github.com/VirtusLab/scala-cli/pull/1174)
* Fix snippet tests for Windows by @Gedochao in [#1172](https://github.com/VirtusLab/scala-cli/pull/1172)

### Updates
* Update mill-main to 0.10.5 by @scala-steward in [#1148](https://github.com/VirtusLab/scala-cli/pull/1148)
* Update snailgun-core, snailgun-core_2.13 to 0.4.1-sc2 by @scala-steward in [#1155](https://github.com/VirtusLab/scala-cli/pull/1155)
* Update jsoniter-scala-core_2.13 to 2.13.35 by @scala-steward in [#1169](https://github.com/VirtusLab/scala-cli/pull/1169)
* Update scala-collection-compat to 2.8.0 by @scala-steward in [#1170](https://github.com/VirtusLab/scala-cli/pull/1170)
* Update jsoniter-scala-core_2.13 to 2.13.36 by @scala-steward in [#1175](https://github.com/VirtusLab/scala-cli/pull/1175)

## New Contributors
* @hugo-vrijswijk made their first contribution in [#1142](https://github.com/VirtusLab/scala-cli/pull/1142)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v0.1.9...v0.1.10

# [v0.1.9](https://github.com/VirtusLab/scala-cli/releases/tag/v0.1.9)

## `--list-main-classes` for `publish` & `package`

`publish` and `package` sub-commands now support the `--list-main-classes` option, which allows to list all the available main classes. Previously it was only available in the `run` command.

Added in https://github.com/VirtusLab/scala-cli/pull/1118 by @Gedochao

## Important fixes & enhancements

### `fmt` options improvement

Added missing documentation on how to pass native `scalafmt` options in the `fmt` sub-command with the `-F` option.
```
$ scala-cli fmt -F --version
scalafmt 3.5.2
```

Additionally, a couple of `scalafmt`'s native options received aliases in Scala CLI: 

`--respect-project-filters` is an alias for `-F --respect-project-filters`. Because of the way sources are passed by Scala CLI to `scalafmt` under the hood, we now turn it on by default to respect any `project.excludePaths` settings in the user's `.scalafmt.conf`.  
It can be disabled by passing `--respect-project-filters=false` to revert to previous behaviour. 
This addresses https://github.com/VirtusLab/scala-cli/issues/1121

`--scalafmt-help` is an alias for `-F --help`. It shows the `--help` output from `scalafmt`, which might prove as helpful reference when in need of using native `scalafmt` options with `-F`.

Added in https://github.com/VirtusLab/scala-cli/pull/1135 by @Gedochao

### Include `libsodium.dll` on Windows

Static linking of libsodium in Windows launcher has been fixed. 
This addresses https://github.com/VirtusLab/scala-cli/issues/1114

Added in https://github.com/VirtusLab/scala-cli/pull/1115 by @alexarchambault

### Force interactive mode for `update` command

Interactive mode for `update` sub-command is now enabled by default. 

Added in https://github.com/VirtusLab/scala-cli/pull/1100 by @lwronski

## In progress

### Publishing-related features 

* Publish tweaks + documentation by @alexarchambault in  https://github.com/VirtusLab/scala-cli/pull/1107

### Better BSP support for Scala scripts

* Add scala-sc language to BSP supported languages by @alexarchambault in https://github.com/VirtusLab/scala-cli/pull/1140

## Other changes

### Documentation PRs

* Update scala 2.12 to 2.12.16 in docs by @lwronski in https://github.com/VirtusLab/scala-cli/pull/1108
* Back port of documentation changes to main by @github-actions in https://github.com/VirtusLab/scala-cli/pull/1111
* Tweak release procedure by @Gedochao in https://github.com/VirtusLab/scala-cli/pull/1112

### Build and internal changes

* Add choco configuration files by @lwronski in https://github.com/VirtusLab/scala-cli/pull/998
* Tweaking by @alexarchambault in https://github.com/VirtusLab/scala-cli/pull/1105
* Add scala-cli-setup deploy key to ssh-agent by @lwronski in https://github.com/VirtusLab/scala-cli/pull/1117

### Updates

* Update scala-cli.sh launcher for 0.1.8 by @github-actions in https://github.com/VirtusLab/scala-cli/pull/1106
* Update case-app to 2.1.0-M14 by @alexarchambault in https://github.com/VirtusLab/scala-cli/pull/1120
* Update Scala to 3.1.3 by @alexarchambault in https://github.com/VirtusLab/scala-cli/pull/1124
* Update jsoniter-scala-core_2.13 to 2.13.32 by @scala-steward in https://github.com/VirtusLab/scala-cli/pull/1125
* Update coursier-jvm_2.13, ... to 2.1.0-M6-28-gbad85693f by @scala-steward in https://github.com/VirtusLab/scala-cli/pull/1126
* Update libsodiumjni to 0.0.3 by @scala-steward in https://github.com/VirtusLab/scala-cli/pull/1127
* Update org.eclipse.jgit to 6.2.0.202206071550-r by @scala-steward in https://github.com/VirtusLab/scala-cli/pull/1128
* Update Scala.js to 1.10.1 by @scala-steward in https://github.com/VirtusLab/scala-cli/pull/1130
* Update Scala Native to 0.4.5 by @alexarchambault in https://github.com/VirtusLab/scala-cli/pull/1133
* Update scala-js-cli to 1.1.1-sc5 by @alexarchambault in https://github.com/VirtusLab/scala-cli/pull/1134
* Update jsoniter-scala-core_2.13 to 2.13.33 by @scala-steward in https://github.com/VirtusLab/scala-cli/pull/1136
* Update `scalafmt`  to 3.5.8 by @Gedochao in https://github.com/VirtusLab/scala-cli/pull/1137
* Update cli-options_2.13, cli_2.13, ... to 0.1.7 by @scala-steward in https://github.com/VirtusLab/scala-cli/pull/1138

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v0.1.8...v0.1.9

# [v0.1.8](https://github.com/VirtusLab/scala-cli/releases/tag/v0.1.8)

## `--list-main-classes` option for the `run` command

You can pass the option `--list-main-classes` to the `run` command to list all the available main classes, including
scripts.

```
$ scala-cli . --list-main-classes
Hello scripts.AnotherScript_sc scripts.Script_sc
```

Added in [#1095](https://github.com/VirtusLab/scala-cli/pull/1095) by @Gedochao

## Add `config` command

The `config` sub-command allows to get and set various configuration values, intended for use by
other Scala CLI sub-commands.

This feature has been added in preparation for the `publish` command, stay tuned for future announcements.

Added in [#1056](https://github.com/VirtusLab/scala-cli/pull/1056) by @alexarchambault

## Prioritise non-script main classes

When trying to run a directory containing scripts and just a single non-script main class, the non-script main class
will now be prioritised and run by default.

```
$ scala-cli .
Running Hello. Also detected script main classes: scripts.AnotherScript_sc, scripts.Script_sc
You can run any one of them by passing option --main-class, i.e. --main-class scripts.AnotherScript_sc
All available main classes can always be listed by passing option --list-main-classes
Hello world
```

Changed in [#1095](https://github.com/VirtusLab/scala-cli/pull/1095) by @Gedochao

## Important bugfixes

### Accept latest Scala versions despite stale Scala version listings in cache

Scala CLI uses version listings from Maven Central to check if a Scala version is valid. When new Scala versions are
released, users could sometimes have stale version listings in their Coursier cache for a short period of time (the
Coursier cache TTL, which is 24 hours by default). This prevented these users to use new Scala versions during that
time.
To work around that, Scala CLI now tries to re-download version listings when they don't have the requested Scala
version.
This addresses [#1090](https://github.com/VirtusLab/scala-cli/issues/1090)

Fixed in [#1096](https://github.com/VirtusLab/scala-cli/pull/1096) by @lwronski

### Bloop now uses `JAVA_HOME` by default

Bloop should now pick up the JDK available in `JAVA_HOME`. It was formerly necessary to pass `--bloop-jvm system`
explicitly. This addresses [#1102](https://github.com/VirtusLab/scala-cli/issues/1102)

Fixed in [#1084](https://github.com/VirtusLab/scala-cli/pull/1084) by @lwronski

### The `-coverage-out` option now accepts relative paths

Scala CLI now correctly processes relative paths when passed to the `-coverage-out` option. Formerly,
the `scoverage.coverage` file would not be properly generated when a relative path was passed.
This addresses [#1072](https://github.com/VirtusLab/scala-cli/issues/1072)

Fixed in [#1080](https://github.com/VirtusLab/scala-cli/pull/1080) by @lwronski

## Other changes

### Documentation PRs

* Improve scripts guide by @Gedochao in [#1074](https://github.com/VirtusLab/scala-cli/pull/1074)
* Update installation instructions for Nix by @kubukoz in [#1082](https://github.com/VirtusLab/scala-cli/pull/1082)
* Tweak docs by @alexarchambault in [#1085](https://github.com/VirtusLab/scala-cli/pull/1085)
* Some typos & rewording on the single-module projects use case page by @Baccata
  in [#1089](https://github.com/VirtusLab/scala-cli/pull/1089)

### Fixes

* Add suffix to project name which contains virtual files by @lwronski
  in [#1070](https://github.com/VirtusLab/scala-cli/pull/1070)

### Build and internal changes

* Update scala-cli.sh launcher for 0.1.7 by @github-actions in [#1076](https://github.com/VirtusLab/scala-cli/pull/1076)
* Tweaking by @alexarchambault in [#1087](https://github.com/VirtusLab/scala-cli/pull/1087)

**Full Changelog**: https://github.com/VirtusLab/scala-cli/compare/v0.1.7...v0.1.8
