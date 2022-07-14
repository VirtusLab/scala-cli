---
title: Managing dependencies
sidebar_position: 9
---

# Dependencies

You can add dependencies on the command line, with the `--dependency` option:
```bash
scala-cli compile Hello.scala \
  --dependency org.scala-lang.modules::scala-parallel-collections:1.0.4
```

You can also add a URL fallback for a JAR dependency, if it can't be fetched otherwise:
```bash
scala-cli compile Hello.scala \
  -- dependency "org::name::version,url=https://url-to-the-jar"
```

Note that `--dependency` is only meant as a convenience.
You should favor adding dependencies in the sources themselves via [using directives](/docs/guides/configuration.md#special-imports).
<!-- TODO #344 
However, `--dependency` CLI option takes precedence over `using` directives, so it can be used to override the `using` directive, such as when you want to work with a different dependency version. -->

You can also add repositories on the command-line, via `--repository`:
```bash
scala-cli compile Hello.scala \
  --dependency com.pany::util:33.1.0 --repo https://artifacts.pany.com/maven
```

Lastly, you can also add simple JAR files as dependencies with `--jar`:
```bash
scala-cli compile Hello.scala --jar /path/to/library.jar
```

## Update dependencies

To check if dependencies in using directives are up to date use `dependency-update` command:

```scala title=Hello.scala
//> using lib "com.lihaoyi::os-lib:0.7.8"
//> using lib "com.lihaoyi::utest:0.7.10"
import $ivy.`com.lihaoyi::geny:0.6.5`
import $dep.`com.lihaoyi::pprint:0.6.6`

object Hello extends App {
  println("Hello World")
}
```

```bash
scala-cli dependency-update Hello.scala
# Updates
#    * com.lihaoyi::os-lib:0.7.8 -> 0.8.1
#    * com.lihaoyi::utest:0.7.10 -> 0.8.0
#    * com.lihaoyi::geny:0.6.5 -> 0.7.1
#    * com.lihaoyi::pprint:0.6.6 -> 0.7.3
# To update all dependencies run: 
#     scala-cli dependency-update --all
```

Passing `--all` to the `dependency-update` sub-command updates all dependencies in your sources.

```bash
scala-cli dependency-update Hello.scala --all
# Updated dependency to: com.lihaoyi::os-lib:0.8.1
# Updated dependency to: com.lihaoyi::utest:0.8.0
# Updated dependency to: com.lihaoyi::geny:0.7.1
# Updated dependency to: com.lihaoyi::pprint:0.7.3
```
