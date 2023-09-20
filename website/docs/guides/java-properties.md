---
title: Java properties
sidebar_position: 8
---

Although the Scala CLI runner can be used as a native image and thus will not always be run on the JVM it still supports java properties.
There are a couple ways to specify them:
- as command line arguments, before the sub-command name and sources, when invoking `scala-cli`, e.g.
```bash ignore
    scala-cli '-Dcoursier.credentials=maven.pkg.github.com Private_Token:gh_token1234' run .
```
- save them in `.scala-jvmopts` file in the project's root, e.g.
```text
-Dcoursier.credentials=maven.pkg.github.com Private_Token:gh_token1234
-Dhttp.proxy=4.4.4.4
-Dhttp.user=User2
```
- set them globally using `scala-cli config`, e.g. 
```bash ignore
    scala-cli config -i java.properties "http.proxy=4.4.4.4" "http.user=User2" "coursier.credentials=..."
```
- set them globally in `JAVA_OPTS` or `JDK_JAVA_OPTIONS` environment variable, e.g. 
```bash ignore
    export JAVA_OPTS="-Dhttp.proxy=4.4.4.4 -Dhttp.user=User2"
```

:::note
The `-D` prefix can only be dropped when writing the values to config.
:::