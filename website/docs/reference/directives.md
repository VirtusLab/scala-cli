---
title: Directives
sidebar_position: 2
---

## using directives

### Java Home

**Directives**: `javaHome`, `java-home`

Set the Java home directory

**Usage**:

```
//> using javaHome <path>
```

Where `<path>` is String literal.

**Examples**:

```
//> using javaHome "/home/user/jvm"
//> using java-home "<path>"
```


### Java Options

**Directives**: `javaOpt`, `java-opt`

Set Java options, such as `-Xmx1g`

**Usage**:

```
//> using javaOpt <java-options> [, <java-options>]*
```

Where `<java-options>` is String literal.

**Examples**:

```
//> using javaOpt List("-Xmx2g", "-Xms1g", "-Xnoclassgc")
//> using java-opt List("-Xmx2g", "-Xms1g", "-Xnoclassgc")
```


### Java Properties

**Directives**: `javaProp`, `java-prop`

Set Java properties

**Usage**:

```
//> using javaProp <key=value|key> [, <key=value|key>]*
```

Where `<key=value|key>` is String literal.

**Examples**:

```
//> using javaProp List("-Xmx2g", "-Xms1g", "-Xnoclassgc")
//> using java-prop List("-Xmx2g", "-Xms1g", "-Xnoclassgc")
```


### Location of clang

**Directives**: `nativeClang`, `native-clang`

Path to the Clang command

**Usage**:

```
//> using nativeClang <string?>
```

Where `<string?>` is String literal.

**Examples**:

```
//> using nativeClang "/usr/bin/clang"
//> using native-clang "/usr/bin/clang"
```


### Location of clang++

**Directives**: `nativeClangPP`, `native-clang-pp`

No argument!

**Usage**:

```
//> using nativeClangPP <value>
```

Where `<value>` is String literal.

**Examples**:

```
//> using nativeClangPP "/usr/bin/clang++"
//> using native-clang-pp "/usr/bin/clang++"
```


### Main class

**Directives**: `main-class`, `mainClass`

No argument!

**Usage**:

```
//> using main-class <value>
```

Where `<value>` is String literal.

**Examples**:

```
//> using mainClass "foo.bar.Baz"
//> using main-class "app.Main"
```


### Platform

**Directives**: `platform`, `platforms`

No argument!

**Usage**:

```
//> using platform <value> [, <value>]*
```

Where `<value>` is String literal.

**Examples**:

```
//> using platform "scala-js"
//> using platform "jvm", "scala-native"
```


### Publish ComputeVersion

**Directives**: `publishComputeVersion`, `publish-compute-version`

No argument!

**Usage**:

```
//> using publishComputeVersion <value>
```

Where `<value>` is String literal, using following format:

 ```
git:(dynver|tag)[:repo] | comamnd:<command>
```

**Examples**:

```
//> using publishComputeVersion "git:tag"
//> using publish-compute-version "git:tag:<repo>"
//> using publishComputeVersion "git:dynver"
//> using publish-compute-version "git:dynver:<repo>"
//> using publishComputeVersion "command:<command>"
```


### Publish Developers

**Directives**: `developer`, `developers`

Developer(s) to add in publishing metadata, like "alex|Alex|https://alex.info" or "alex|Alex|https://alex.info|alex@alex.me"

**Usage**:

```
//> using developer <id|name|URL|email> [, <id|name|URL|email>]*
```

Where `<id|name|URL|email>` is String literal.

**Examples**:

```
//> using developer List("_example|Sarah Jones|https://docs.github.com/_example", "_example|Sarah Jones|https://docs.github.com/_example", "_example2|Nick Smith|https://docs.github.com/_example|nick@example.org")
//> using developers List("_example|Sarah Jones|https://docs.github.com/_example", "_example|Sarah Jones|https://docs.github.com/_example", "_example2|Nick Smith|https://docs.github.com/_example|nick@example.org")
```


### Publish GpgKey

**Directives**: `publishGpgKey`, `publish-gpg-key`

No argument!

**Usage**:

```
//> using publishGpgKey <value>
```

Where `<value>` is String literal.

**Examples**:

```
//> using publishGpgKey "user@email.com"
//> using publish-gpg-key "user@email.com"
```


### Publish GpgOptions

**Directives**: `publishGpgOptions`, `publish-gpg-options`

No argument!

**Usage**:

```
//> using publishGpgOptions <value> [, <value>]*
```

Where `<value>` is String literal.

**Examples**:

```
//> using publishGpgOptions List("--armor", "--local-user", "sarah_j")
//> using publish-gpg-options List("--armor", "--local-user", "sarah_j")
```


### Publish License

**Directives**: `publishLicense`, `publish-license`

No argument!

**Usage**:

```
//> using publishLicense <value>
```

Where `<value>` is String literal.

**Examples**:

```
//> using publishLicense "Apache 2.0"
//> using publish-license "Apache 2.0"
```


### Publish Name

**Directives**: `publishName`, `publish-name`

No argument!

**Usage**:

```
//> using publishName <value>
```

Where `<value>` is String literal.

**Examples**:

```
//> using publishName "scala-cli-core"
//> using publish-name "scala-cli-core"
```


### Publish Organization

**Directives**: `publishOrganization`, `publish-organization`

No argument!

**Usage**:

```
//> using publishOrganization <value>
```

Where `<value>` is String literal.

**Examples**:

```
//> using publishOrganization "com.githib.scala-cli"
//> using publish-organization "com.githib.scala-cli"
```


### Publish Repository

**Directives**: `publishRepository`, `publish-repository`

Repository to publish to

**Usage**:

```
//> using publishRepository <URL or path>
```

Where `<URL or path>` is String literal.

**Examples**:

```
//> using publishRepository "https://repo.maven.apache.org/maven2"
//> using publish-repository "https://repo.maven.apache.org/maven2"
```


### Publish ScalaPlatformSuffix

**Directives**: `publishScalaPlatformSuffix`, `publish-scala-platform-suffix`

No argument!

**Usage**:

```
//> using publishScalaPlatformSuffix <value>
```

Where `<value>` is String literal.

**Examples**:

```
//> using publishScalaPlatformSuffix "_js"
//> using publish-scala-platform-suffix "_js"
```


### Publish ScalaVersionSuffix

**Directives**: `publishScalaVersionSuffix`, `publish-scala-version-suffix`

No argument!

**Usage**:

```
//> using publishScalaVersionSuffix <value>
```

Where `<value>` is String literal.

**Examples**:

```
//> using publishScalaVersionSuffix "_3"
//> using publish-scala-version-suffix "_3"
```


### Publish URL

**Directives**: `publishURL`, `publish-u-r-l`

No argument!

**Usage**:

```
//> using publishURL <value>
```

Where `<value>` is String literal.

**Examples**:

```
//> using publishURL "https://scala-cli.virtuslab.org/"
//> using publish-u-r-l "https://scala-cli.virtuslab.org/"
```


### Publish Version

**Directives**: `publishVersion`, `publish-version`

No argument!

**Usage**:

```
//> using publishVersion <value>
```

Where `<value>` is String literal.

**Examples**:

```
//> using publishVersion "1.0.1-RC2"
//> using publish-version "1.0.1-RC2"
```


### Publish VersionControl

**Directives**: `publishVersionControl`, `publish-version-control`, `scm`

No argument!

**Usage**:

```
//> using publishVersionControl <value>
```

Where `<value>` is String literal.

**Examples**:

```
//> using publishVersionControl "github:VirtusLab/scala-cli.git"
//> using publish-version-control "<url>|<connection>|<dev_connection>"
//> using scm "github:VirtusLab/scala-cli.git"
```


### Scala Native CompileOptions

**Directives**: `nativeCompileOptions`, `native-compile-options`

No argument!

**Usage**:

```
//> using nativeCompileOptions <value> [, <value>]*
```

Where `<value>` is String literal.

**Examples**:

```
//> using nativeCompileOptions List("TODO", "TODO", "TODO")
//> using native-compile-options List("TODO", "TODO", "TODO")
```


### Scala Native GC

**Directives**: `nativeGC`, `native-g-c`

No argument!

**Usage**:

```
//> using nativeGC <value>
```

Where `<value>` is String literal.

**Examples**:

```
//> using nativeGC "none"
//> using native-g-c "boehm"
```


### Scala Native LinkingOptions

**Directives**: `nativeLinkingOptions`, `native-linking-options`

No argument!

**Usage**:

```
//> using nativeLinkingOptions <value> [, <value>]*
```

Where `<value>` is String literal.

**Examples**:

```
//> using nativeLinkingOptions List("TODO", "TODO", "TODO")
//> using native-linking-options List("TODO", "TODO", "TODO")
```


### Scala Native Mode

**Directives**: `nativeMode`, `native-mode`

Set Scala Native compilation mode

**Usage**:

```
//> using nativeMode <string?>
```

Where `<string?>` is String literal.

**Examples**:

```
//> using nativeMode "release"
//> using native-mode "release-fast"
```


### Scala Native Version

**Directives**: `nativeVersion`, `native-version`

Set the Scala Native version

**Usage**:

```
//> using nativeVersion <string?>
```

Where `<string?>` is String literal.

**Examples**:

```
//> using nativeVersion "0.4.0"
//> using native-version "0.4.1"
```


### Scala version

**Directives**: `scala`

Set the Scala version

**Usage**:

```
//> using scala <version> [, <version>]*
```

Where `<version>` is String literal orNumber literal.

**Examples**:

```
//> using scala "3.0.2"
//> using scala 3
//> using scala 3.1
//> using scala "2.13"
//> using scala "2"
//> using scala "2.13.6", "2.12.15"
```


### Scala.js AvoidClasses

**Directives**: `jsAvoidClasses`, `js-avoid-classes`

Avoid class'es when using functions and prototypes has the same observable semantics.

**Usage**:

```
//> using jsAvoidClasses [true|false]
```

**Examples**:

```
//> using jsAvoidClasses
//> using js-avoid-classes false
```


### Scala.js AvoidLetsAndConsts

**Directives**: `jsAvoidLetsAndConsts`, `js-avoid-lets-and-consts`

Avoid lets and consts when using vars has the same observable semantics.

**Usage**:

```
//> using jsAvoidLetsAndConsts [true|false]
```

**Examples**:

```
//> using jsAvoidLetsAndConsts
//> using js-avoid-lets-and-consts false
```


### Scala.js CheckIr

**Directives**: `jsCheckIr`, `js-check-ir`

No argument!

**Usage**:

```
//> using jsCheckIr [true|false]
```

**Examples**:

```
//> using jsCheckIr
//> using js-check-ir false
```


### Scala.js Dom

**Directives**: `jsDom`, `js-dom`

Enable jsdom

**Usage**:

```
//> using jsDom [true|false]
```

**Examples**:

```
//> using jsDom
//> using js-dom false
```


### Scala.js EmitSourceMaps

**Directives**: `jsEmitSourceMaps`, `js-emit-source-maps`

Emit source maps

**Usage**:

```
//> using jsEmitSourceMaps [true|false]
```

**Examples**:

```
//> using jsEmitSourceMaps
//> using js-emit-source-maps false
```


### Scala.js EsVersionStr

**Directives**: `jsEsVersionStr`, `js-es-version-str`

No argument!

**Usage**:

```
//> using jsEsVersionStr <value>
```

Where `<value>` is String literal.

**Examples**:

```
//> using jsEsVersionStr "TODO"
//> using js-es-version-str "TODO"
```


### Scala.js Header

**Directives**: `jsHeader`, `js-header`

A header that will be added at the top of generated .js files

**Usage**:

```
//> using jsHeader <string?>
```

Where `<string?>` is String literal.

**Examples**:

```
//> using jsHeader "TODO"
//> using js-header "TODO"
```


### Scala.js Mode

**Directives**: `jsMode`, `js-mode`

The Scala.js mode, either `dev` or `release`

**Usage**:

```
//> using jsMode <string?>
```

Where `<string?>` is String literal.

**Examples**:

```
//> using jsMode "TODO"
//> using js-mode "TODO"
```


### Scala.js ModuleKind

**Directives**: `jsModuleKind`, `js-module-kind`

The Scala.js module kind: commonjs/common, esmodule/es, nomodule/none

**Usage**:

```
//> using jsModuleKind <string?>
```

Where `<string?>` is String literal.

**Examples**:

```
//> using jsModuleKind "TODO"
//> using js-module-kind "TODO"
```


### Scala.js SmallModuleForPackage

**Directives**: `jsSmallModuleForPackage`, `js-small-module-for-package`

Create as many small modules as possible for the classes in the passed packages and their subpackages.

**Usage**:

```
//> using jsSmallModuleForPackage <string*> [, <string*>]*
```

Where `<string*>` is String literal.

**Examples**:

```
//> using jsSmallModuleForPackage List("pckA", "pckB", "packC")
//> using js-small-module-for-package List("pckA", "pckB", "packC")
```


### Scala.js Version

**Directives**: `jsVersion`, `js-version`

The Scala.js version

**Usage**:

```
//> using jsVersion <string?>
```

Where `<string?>` is String literal.

**Examples**:

```
//> using jsVersion "3.2.1"
//> using js-version "TODO"
```


### Test framework

**Directives**: `test-framework`, `testFramework`

No argument!

**Usage**:

```
//> using test-framework <value>
```

Where `<value>` is String literal.

**Examples**:

```
//> using testFramework "utest.runner.Framework"
```



## target directives

