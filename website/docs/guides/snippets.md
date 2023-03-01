---
title: Snippets
sidebar_position: 43
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

# Snippets

Instead of passing paths to your sources, you can also pass the code itself with the appropriate option.

<ChainedSnippets>

```bash
scala-cli run --scala-snippet '@main def hello() = println("Hello")'
```

```text
Hello
```

</ChainedSnippets>

You can also divide your code into multiple snippets when passing it this way. Each snippet is then treated as a
separate input by Scala CLI.

<ChainedSnippets>

```bash
scala-cli run --scala-snippet '@main def main() = println(Messages.hello)' --scala-snippet 'object Messages { def hello = "Hello" }'
```

```text
Hello
```

</ChainedSnippets>

## Examples

- scripts

<ChainedSnippets>

```bash
scala-cli run -e 'println("Hello")'
```

```text
Hello
```

</ChainedSnippets>

- Scala code

<ChainedSnippets>

```bash
scala-cli run --scala-snippet '@main def hello() = println("Hello")'
```

```text
Hello
```

</ChainedSnippets>

- Java code

<ChainedSnippets>

```bash
scala-cli run --java-snippet 'class Hello { public static void main(String args[]) { System.out.println("Hello"); } }'
```

```text
Hello
```

</ChainedSnippets>

- Markdown code (experimental)

<ChainedSnippets>

````bash
scala-cli run --markdown-snippet '# Markdown snippet
with a code block
```scala
println("Hello")
```'
````

```text
Hello
```

</ChainedSnippets>

- a mix of Scala, Java and scripts

<ChainedSnippets>

```bash
scala-cli run --scala-snippet '@main def hello() = println(s"${JavaSnippet.hello} ${snippet.world}")' --java-snippet 'public class JavaSnippet { public static String hello = "Hello"; }' --script-snippet 'def world = "world"'
```

```text
Hello world
```

</ChainedSnippets>

## Snippets and other kinds of inputs

It is also possible to mix snippets with on-disk sources.

<ChainedSnippets>

```scala title=Main.scala
object Main extends App {
  val snippetData = SnippetData()
  println(snippetData.value)
}
```

```bash
scala-cli Main.scala --scala-snippet 'case class SnippetData(value: String = "Hello")'
```

```text
Hello
```

</ChainedSnippets>

Or even with piped ones, why not.

<ChainedSnippets>

```bash
echo 'println(SnippetData().value)' ||  scala-cli _.sc --scala-snippet 'case class SnippetData(value: String = "Hello")'
```

```text
Hello
```

</ChainedSnippets>

Nothing stops you from mixing everything all at once, really.

<ChainedSnippets>

```scala title=Main.scala
object Main extends App {
  val scalaSnippetString = ScalaSnippet().value
  val javaSnippetString = JavaSnippet.data
  val scriptSnippetString = snippet.script
  val pipedInputString = stdin.piped
  val ondiskScriptString = ondisk.script
  println(s"Output: $scalaSnippetString $javaSnippetString $scriptSnippetString $pipedInputString")
}
```

```scala title=ondisk.sc
def script = "on-disk-script"
```

```bash
echo 'def piped = "piped-script"'|scala-cli . _.sc --scala-snippet 'case class ScalaSnippet(value: String = "scala-snippet")' --java-snippet 'public class JavaSnippet { public static String data = "java-snippet"; }' --script-snippet 'def script = "script-snippet"'
```

```text
Output: scala-snippet java-snippet script-snippet piped-script
```

</ChainedSnippets>

## Referring to code from a snippet

When referring to code coming from a script snippet passed with `--script-snippet` (or `-e`), you use its wrapper
name: `snippet`

<ChainedSnippets>

```bash
scala-cli run --scala-snippet '@main def main() = println(snippet.hello)' --script-snippet 'def hello: String = "Hello"'
```

```text
Hello
```

</ChainedSnippets>

When referring to code coming from multiple script snippets, you use their wrapper names according to the order they
were passed (starting from 0 for the first script snippet): `snippet${snippetNumber}`. The `snippetNumber` is omitted
for the first script snippet (0). In other words, the first passed snippet is just `snippet`, the second is `snippet1`,
then `snippet2` and so on, as in the example:

<ChainedSnippets>

```bash
scala-cli run --scala-snippet '@main def main() = println(s"${snippet.hello} ${snippet1.world}${snippet2.exclamation}")' --script-snippet 'def hello: String = "Hello"' --script-snippet 'def world: String = "world"' --script-snippet 'def exclamation: String = "!"'
```

```text
Hello world!
```

</ChainedSnippets>

This is similar to how you refer to code from piped scripts through their wrapper name (`stdin`), more on which can be
found in [the scripts guide](scripts.md).

In fact, you can refer to both kinds of scripts at one time, just keep in mind that you need to pick which script is to
actually be run with the `--main-class` option when multiple scripts are present on the classpath (and no non-script
main class was passed).

<ChainedSnippets>

```scala title=ondisk.sc
println(s"${stdin.hello} ${snippet.world}${snippet1.exclamation}")
```

```bash
echo 'def hello = "Hello"' | scala-cli _.sc ondisk.sc -e 'def world = "world"' -e 'def exclamation = "!"' --main-class ondisk_sc
```

```text
Hello world!
```

</ChainedSnippets>

When in doubt on what main classes are available on the classpath, you can always refer to the output
of `--list-main-classes`

<ChainedSnippets>

```bash
echo 'def hello = "Hello"' | scala-cli _.sc ondisk.sc -e 'def world = "world"' -e 'def exclamation = "!"' --list-main-classes
```

```text
ondisk_sc snippet_sc stdin_sc
```

</ChainedSnippets>
