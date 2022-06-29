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
