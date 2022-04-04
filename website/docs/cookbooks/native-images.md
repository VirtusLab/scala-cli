---
title: Packaging Scala applications as GraalVM native images
sidebar_position: 7
---

Scala CLI lets you package your applications as native executables
using GraalVM native images.

As an example, let's package the following application as a native executable
using GraalVM native image:
```scala title=Echo.scala
object Echo {
  def main(args: Array[String]): Unit =
    println(args.mkString(" "))
}
```

The following command packages this application as a native executable:
```bash
scala-cli package Echo.scala -o echo
```

<!-- Expected-regex:
Wrote .*echo
.*\/echo
-->

```bash
# Run echo on macOS
./echo a b
# a b
```

<!-- 
```bash
rm ./echo
``` 
-->

You can pass custom options to GraalVM native image by passing them after `--`, like
```bash
scala-cli package Echo.scala -o echo -- --no-fallback
```

<!-- Expected-regex:
Wrote .*echo, run it with
  .*\/echo
-->
