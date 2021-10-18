---
title: Managing dependencies
sidebar_position: 23
---

# Dependencies

You can add dependencies on the command-line, via `--dependency`:
```bash
scala-cli compile Hello.scala --dependency dev.zio::zio:1.0.9
```

Note that `--dependency` is only meant as a convenience. You should favour
adding dependencies in the sources themselves via [using directives](./guides/configuration.md#special-imports).

You can also add repositories on the command-line, via `--repository`:
```bash
scala-cli compile Hello.scala --dependency com.pany::util:33.1.0 --repo https://artifacts.pany.com/maven
```

Lastly, you can also add simple JAR files as dependencies, with `--jar`:
```bash
scala-cli compile Hello.scala --jar /path/to/library.jar
```