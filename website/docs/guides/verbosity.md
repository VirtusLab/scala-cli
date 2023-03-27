---
title: Verbosity
sidebar_position: 44
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

Logging in Scala CLI can be controlled in a number of ways.

```scala title=Hello.sc
println("Hello")
```

Logs, warnings and errors will be printed by default.

<ChainedSnippets>

```bash
scala-cli Hello.sc
```

```text  
Compiling project (Scala 3.2.2, JVM)
Compiled project (Scala 3.2.2, JVM)
Hello
```

</ChainedSnippets>

## Silencing logs with `-q`

All logs except for errors can be silenced by passing the `-q` option.

<ChainedSnippets>

```bash
scala-cli Hello.sc -q
```

```text
Hello
```

</ChainedSnippets>

## Increasing verbosity with `-v`

You can increase verbosity by passing the `-v` option, to print debugging logs or gain extra context.

<ChainedSnippets>

```bash
scala-cli Hello.sc -v
```

```text
Compiling project (Scala 3.2.2, JVM)
Compiled project (Scala 3.2.2, JVM)
Running ~/Library/Java/JavaVirtualMachines/corretto-17.0.5/Contents/Home/bin/java -cp ~/IdeaProjects/scala-cli-tests/hello1/.scala-build/project_853f6d1dbb-28a878fa14/classes/main:~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala3-library_3/3.2.2/scala3-library_3-3.2.2.jar:~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.10/scala-library-2.13.10.jar Hello_sc
Hello
```

</ChainedSnippets>

You can increase verbosity even further by passing the `-v` option multiple times.

<ChainedSnippets>

```bash
scala-cli Hello.sc -v -v
```

```text
Fetching List(Dependency(org.scala-lang:scala3-compiler_3, 3.2.2, Configuration(), Set(), Publication(, Type(), Extension(), Classifier()), false, true)), adding List(IvyRepository(Pattern(List(Const(file://~/Library/Caches/ScalaCli/local-repo/v0.2.0//), Var(organisation), Const(/), Var(module), Const(/), Opt(List(Const(scala_), Var(scalaVersion), Const(/))), Opt(List(Const(sbt_), Var(sbtVersion), Const(/))), Var(revision), Const(/), Var(type), Const(s/), Var(artifact), Opt(List(Const(-), Var(classifier))), Const(.), Var(ext))), None, None, true, true, true, false, None, true))
Found 13 artifacts:
  ~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala3-compiler_3/3.2.2/scala3-compiler_3-3.2.2.jar
  ~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala3-interfaces/3.2.2/scala3-interfaces-3.2.2.jar
  ~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala3-library_3/3.2.2/scala3-library_3-3.2.2.jar
  ~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/tasty-core_3/3.2.2/tasty-core_3-3.2.2.jar
  ~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-asm/9.3.0-scala-1/scala-asm-9.3.0-scala-1.jar
  ~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/compiler-interface/1.3.5/compiler-interface-1.3.5.jar
  ~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/jline/jline-reader/3.19.0/jline-reader-3.19.0.jar
  ~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/jline/jline-terminal/3.19.0/jline-terminal-3.19.0.jar
  ~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/jline/jline-terminal-jna/3.19.0/jline-terminal-jna-3.19.0.jar
  ~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.10/scala-library-2.13.10.jar
  ~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/google/protobuf/protobuf-java/3.7.0/protobuf-java-3.7.0.jar
  ~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/util-interface/1.3.0/util-interface-1.3.0.jar
  ~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/net/java/dev/jna/jna/5.3.1/jna-5.3.1.jar
(...)
Hello

```

</ChainedSnippets>

## Warnings suppression

Some specific warning logs can be suppressed individually.
That can be done by passing an appropriate option or by setting the appropriate global configuration key.

### Warnings about `using` directives spread in multiple files

```scala title=Deps1.sc
//> using dep "com.lihaoyi::os-lib:0.9.1"
```

```scala title=Deps2.sc
//> using dep "com.lihaoyi::pprint:0.8.0"
```

It is generally advised to not spread the `using` directives in multiple files, and put them in the
optional `project.scala` configuration file.
The relevant warnings can be suppressed with the `--suppress-outdated-dependency-warning` option.

```bash
scala-cli Deps1.sc Deps2.sc --suppress-outdated-dependency-warning
```

Alternatively, the global config key `suppress-warning.directives-in-multiple-files` can be used.

```bash
scala-cli config suppress-warning.directives-in-multiple-files true
```

### Warnings about experimental features' usage

Using experimental features produces warnings, which can be suppressed with the `--suppress-experimental-warning`
option.

````bash
scala-cli --power run --suppress-experimental-warning --markdown-snippet '# Markdown snippet
with a code block
```scala
println("Hello")
```'
````

Alternatively, the global config key `suppress-warning.experimental-features` can be used.

```bash
scala-cli config suppress-warning.experimental-features true
```

### Warnings about having outdated dependencies

```scala title=OldDeps.sc
//> using dep "com.lihaoyi::pprint:0.6.6"
```

Depending on outdated libraries produces warnings, which can be suppressed with
the `--suppress-outdated-dependencies-warning` option.

````bash
scala-cli OldDeps.sc --suppress-outdated-dependencies-warning
````

Alternatively, the global config key `suppress-warning.outdated-dependencies-files` can be used.

```bash
scala-cli config suppress-warning.outdated-dependencies-files true
```