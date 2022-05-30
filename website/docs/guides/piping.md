---
title: Piping
sidebar_position: 23
---

# Piping

Instead of passing paths to your sources, you can also pipe your code via standard input:
```bash
echo "@main def hello() = println(\"Hello\")" | scala-cli _
# Hello
```

## Wildcards
The `_` wildcard implies that the piped code is a standard Scala app.
It is also possible to pass a script or Java code, when using the appropriate wildcard.
The available options are as follows:
- for standard Scala code use `_`, `_.scala` or `-.scala`;
- for Scala scripts use `-`, `_.sc` or `-.sc`;
- for Java code use `_.java` or `-.java`.

## Examples
- scripts
  ```bash
  echo 'println("Hello")' | scala-cli _.sc
  # Hello
  ```
- Scala code
  ```bash
  echo "@main def hello() = println(\"Hello\")" | scala-cli _.scala
  # Hello
  ```
- Java code
  ```bash
  echo "class Hello { public static void main(String args[]) { System.out.println(\"Hello\"); } }" | scala-cli _.java
  # Hello
  ```

## Mixing piped sources with on-disk ones
It is also possible to pipe some code via standard input, while the rest of your code is on-disk.
```bash
echo "case class HelloMessage(msg: String)" > HelloMessage.scala
echo "@main def hello() = println(HelloMessage(msg = \"Hello\").msg)" | scala-cli _ HelloMessage.scala
# Hello
```
