---
title: Java properties
sidebar_position: 45
---

Although the Scala CLI runner can be used as a native image and thus will not always be run on the JVM it still supports Java properties.
There are a couple ways to specify them.

### On the Command line
Java properties can be passed as arguments, before the sub-command name and sources, when invoking `scala-cli`, e.g.
```bash ignore
    scala-cli '-Dcoursier.credentials=maven.pkg.github.com Private_Token:gh_token1234' run .
```
:::note
- `scala-cli run . -Dfoo=bar` would pass the java property into your Scala app
- `scala-cli -Dfoo=bar run .` would pass the java property into `scala-cli`.
  :::

### File named `.scala-jvmopts` 
You can also use a `.scala-jvmopts` file placed in the project's root, example file content:
```text
-Dcoursier.credentials=maven.pkg.github.com Private_Token:gh_token1234
-Dhttp.proxy=4.4.4.4
-Dhttp.user=User2
```

### Scala CLI's `config`
It's also possible to use the `config` subcommand to set the properties globally:
```bash ignore
    scala-cli --power config -i java.properties "http.proxy=4.4.4.4" "http.user=User2" "coursier.credentials=..."
```
:::note
Please note that if you need to modify the Java properties, you have to redefine all of them. It's not possible
to update just a single value via the `config` command. Each update effectively replaces the entire Java properties
list.
:::

### `JAVA_OPTS` and `JDK_JAVA_OPTIONS`
Scala CLI will also read environment variables `JAVA_OPTS` and `JDK_JAVA_OPTIONS` and scan them for Java properties:
```bash ignore
    export JAVA_OPTS="-Dhttp.proxy=4.4.4.4 -Dhttp.user=User2"
```

:::note
The `-D` prefix can only be dropped when writing the values to config.
:::