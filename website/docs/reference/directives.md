---
title: Directives
sidebar_position: 2
---

## using directives

### Java Home

TODO no arg

`//> using javaHome "path"

//> using java-home "path"`

#### Examples
`//> using javaHome "/home/user/jvm"`

`//> using java-home "<path>"`

### Java Options

Set Java options, such as `-Xmx1g`

`//> using javaOpt "option0"

//> using java-opt "option0, option1"`

#### Examples
`//> using javaOpt List("-Xmx2g", "-Xms1g", "-Xnoclassgc")`

`//> using java-opt List("-Xmx2g", "-Xms1g", "-Xnoclassgc")`

### Java Properties

Set Java properties

`//> using javaProp "option0"

//> using java-prop "option0, option1"`

#### Examples
`//> using javaProp List("-Xmx2g", "-Xms1g", "-Xnoclassgc")`

`//> using java-prop List("-Xmx2g", "-Xms1g", "-Xnoclassgc")`

### Location of clang

Path to the Clang command

`//> using nativeClang "path"

//> using native-clang "path"`

#### Examples
`//> using nativeClang "/usr/bin/clang"`

`//> using native-clang "/usr/bin/clang"`

### Location of clang++

TODO no arg

`//> using nativeClangPP "path"

//> using native-clang-pp "path"`

#### Examples
`//> using nativeClangPP "/usr/bin/clang++"`

`//> using native-clang-pp "/usr/bin/clang++"`

### Main class

TODO no arg

`//> using mainClass <fqn>`

#### Examples
`//> using mainClass "foo.bar.Baz"`

`//> using main-class "app.Main"`

### Platform

Set the default platform to Scala.js or Scala Native

`//> using platform `(`jvm`|`scala-js`|`scala-native`)+

#### Examples
`//> using platform "scala-js"`

`//> using platform "jvm", "scala-native"`

### Publish ComputeVersion

TODO no arg

`//> using publishComputeVersion "git:(dynver|tag)[:repo] | comamnd:<command>3"

//> using publish-compute-version "git:(dynver|tag)[:repo] | comamnd:<command>3"`

#### Examples
`//> using publishComputeVersion "git:tag"`

`//> using publish-compute-version "git:tag:<repo>"`

`//> using publishComputeVersion "git:dynver"`

`//> using publish-compute-version "git:dynver:<repo>"`

`//> using publishComputeVersion "command:<command>"`

### Publish Developers

TODO no arg

`//> using developer "id|name|address[|email]0"

//> using developers "id|name|address[|email]0, id|name|address[|email]1"`

#### Examples
`//> using developer List("_example|Sarah Jones|https://docs.github.com/_example", "_example|Sarah Jones|https://docs.github.com/_example", "_example2|Nick Smith|https://docs.github.com/_example|nick@example.org")`

`//> using developers List("_example|Sarah Jones|https://docs.github.com/_example", "_example|Sarah Jones|https://docs.github.com/_example", "_example2|Nick Smith|https://docs.github.com/_example|nick@example.org")`

### Publish GpgKey

TODO no arg

`//> using publishGpgKey "<key>"

//> using publish-gpg-key "<key>"`

#### Examples
`//> using publishGpgKey "user@email.com"`

`//> using publish-gpg-key "user@email.com"`

### Publish GpgOptions

TODO no arg

`//> using publishGpgOptions "option0"

//> using publish-gpg-options "option0, option1"`

#### Examples
`//> using publishGpgOptions List("--armor", "--local-user", "sarah_j")`

`//> using publish-gpg-options List("--armor", "--local-user", "sarah_j")`

### Publish License

TODO no arg

`//> using publishLicense "<license>"

//> using publish-license "<license>"`

#### Examples
`//> using publishLicense "Apache 2.0"`

`//> using publish-license "Apache 2.0"`

### Publish Name

TODO no arg

`//> using publishName "name"

//> using publish-name "name"`

#### Examples
`//> using publishName "scala-cli-core"`

`//> using publish-name "scala-cli-core"`

### Publish Organization

TODO no arg

`//> using publishOrganization "organization"

//> using publish-organization "organization"`

#### Examples
`//> using publishOrganization "com.githib.scala-cli"`

`//> using publish-organization "com.githib.scala-cli"`

### Publish Repository

Repository to publish to

`//> using publishRepository "<repository>"

//> using publish-repository "<repository>"`

#### Examples
`//> using publishRepository "https://repo.maven.apache.org/maven2"`

`//> using publish-repository "https://repo.maven.apache.org/maven2"`

### Publish ScalaPlatformSuffix

TODO no arg

`//> using publishScalaPlatformSuffix "<suffix>"

//> using publish-scala-platform-suffix "<suffix>"`

#### Examples
`//> using publishScalaPlatformSuffix "_js"`

`//> using publish-scala-platform-suffix "_js"`

### Publish ScalaVersionSuffix

TODO no arg

`//> using publishScalaVersionSuffix "<suffix>"

//> using publish-scala-version-suffix "<suffix>"`

#### Examples
`//> using publishScalaVersionSuffix "_3"`

`//> using publish-scala-version-suffix "_3"`

### Publish URL

TODO no arg

`//> using publishURL "<url>"

//> using publish-u-r-l "<url>"`

#### Examples
`//> using publishURL "https://scala-cli.virtuslab.org/"`

`//> using publish-u-r-l "https://scala-cli.virtuslab.org/"`

### Publish Version

TODO no arg

`//> using publishVersion "version"

//> using publish-version "version"`

#### Examples
`//> using publishVersion "1.0.1-RC2"`

`//> using publish-version "1.0.1-RC2"`

### Publish VersionControl

TODO no arg

`//> using publishVersionControl "github:<org>/<repo> | <url>|<connection>|<dev_connection>"

//> using publish-version-control "github:<org>/<repo> | <url>|<connection>|<dev_connection>"

//> using scm "github:<org>/<repo> | <url>|<connection>|<dev_connection>"`

#### Examples
`//> using publishVersionControl "github:VirtusLab/scala-cli.git"`

`//> using publish-version-control "<url>|<connection>|<dev_connection>"`

`//> using scm "github:VirtusLab/scala-cli.git"`

### Scala Native CompileOptions

TODO no arg

`//> using nativeCompileOptions "option0"

//> using native-compile-options "option0, option1"`

#### Examples
`//> using nativeCompileOptions List("TODO", "TODO", "TODO")`

`//> using native-compile-options List("TODO", "TODO", "TODO")`

### Scala Native GC

TODO no arg

`//> using nativeGC "gc"

//> using native-g-c "gc"`

#### Examples
`//> using nativeGC "none"`

`//> using native-g-c "boehm"`

### Scala Native LinkingOptions

TODO no arg

`//> using nativeLinkingOptions "option0"

//> using native-linking-options "option0, option1"`

#### Examples
`//> using nativeLinkingOptions List("TODO", "TODO", "TODO")`

`//> using native-linking-options List("TODO", "TODO", "TODO")`

### Scala Native Mode

Set Scala Native compilation mode

`//> using nativeMode "mode"

//> using native-mode "mode"`

#### Examples
`//> using nativeMode "release"`

`//> using native-mode "release-fast"`

### Scala Native Version

Set the Scala Native version

`//> using nativeVersion "version"

//> using native-version "version"`

#### Examples
`//> using nativeVersion "0.4.0"`

`//> using native-version "0.4.1"`

### Scala version

Set the Scala version

`//> using scala <version>

//> using scala <base-version>, <cross-version>

//> using scala <base-version>, <cross-version1>, <cross-version2>`

#### Examples
`//> using scala "3.0.2"`

`//> using scala 3`

`//> using scala 3.1`

`//> using scala "2.13"`

`//> using scala "2"`

`//> using scala "2.13.6", "2.12.15"`

### Scala.js AvoidClasses

Avoid class'es when using functions and prototypes has the same observable semantics.

`//> using jsAvoidClasses [true|false]

//> using js-avoid-classes [true|false]`

#### Examples
`//> using jsAvoidClasses`

`//> using js-avoid-classes false`

### Scala.js AvoidLetsAndConsts

Avoid lets and consts when using vars has the same observable semantics.

`//> using jsAvoidLetsAndConsts [true|false]

//> using js-avoid-lets-and-consts [true|false]`

#### Examples
`//> using jsAvoidLetsAndConsts`

`//> using js-avoid-lets-and-consts false`

### Scala.js CheckIr

TODO nod help

`//> using jsCheckIr [true|false]

//> using js-check-ir [true|false]`

#### Examples
`//> using jsCheckIr`

`//> using js-check-ir false`

### Scala.js Dom

Enable jsdom

`//> using jsDom [true|false]

//> using js-dom [true|false]`

#### Examples
`//> using jsDom`

`//> using js-dom false`

### Scala.js EmitSourceMaps

Emit source maps

`//> using jsEmitSourceMaps [true|false]

//> using js-emit-source-maps [true|false]`

#### Examples
`//> using jsEmitSourceMaps`

`//> using js-emit-source-maps false`

### Scala.js EsVersionStr

TODO no arg

`//> using jsEsVersionStr "version"

//> using js-es-version-str "version"`

#### Examples
`//> using jsEsVersionStr "TODO"`

`//> using js-es-version-str "TODO"`

### Scala.js Header

A header that will be added at the top of generated .js files

`//> using jsHeader "header"

//> using js-header "header"`

#### Examples
`//> using jsHeader "TODO"`

`//> using js-header "TODO"`

### Scala.js Mode

The Scala.js mode, either `dev` or `release`

`//> using jsMode "mode"

//> using js-mode "mode"`

#### Examples
`//> using jsMode "TODO"`

`//> using js-mode "TODO"`

### Scala.js ModuleKind

The Scala.js module kind: commonjs/common, esmodule/es, nomodule/none

`//> using jsModuleKind "kind"

//> using js-module-kind "kind"`

#### Examples
`//> using jsModuleKind "TODO"`

`//> using js-module-kind "TODO"`

### Scala.js SmallModuleForPackage

Create as many small modules as possible for the classes in the passed packages and their subpackages.

`//> using jsSmallModuleForPackage "value0, value1"

//> using js-small-module-for-package "value0"`

#### Examples
`//> using jsSmallModuleForPackage List("pckA", "pckB", "packC")`

`//> using js-small-module-for-package List("pckA", "pckB", "packC")`

### Scala.js Version

The Scala.js version

`//> using jsVersion "version"

//> using js-version "version"`

#### Examples
`//> using jsVersion "3.2.1"`

`//> using js-version "TODO"`

### Test framework

TODO no arg

`using testFramework <class_name> 

using test-framework <class_name>`

#### Examples
`//> using testFramework "utest.runner.Framework"`


## target directives

### Platform

Require a Scala platform for the current file

`//> using target.platform `_platform_

#### Examples
`//> using target.platform "scala-js"`

`//> using target.platform "scala-js", "scala-native"`

`//> using target.platform "jvm"`

### Scala version

Require a Scala version for the current file

`//> using target.scala `_version_

#### Examples
`//> using target.scala "3"`

`//> using target.scala.>= "2.13"`

`//> using target.scala.< "3.0.2"`

### Scope

Require a scope for the current file

`//> using target.scope `_scope_

#### Examples
`//> using target.scope "test"`

