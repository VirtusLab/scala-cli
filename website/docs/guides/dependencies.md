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
