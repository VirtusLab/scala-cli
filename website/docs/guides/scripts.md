---
title: Scripts
sidebar_position: 19
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

# Scripts

`scala-cli` accepts Scala scripts as files that end in `.sc`.
Unlike `.scala` files, in scripts, any kind of statement is accepted at the top-level:

```scala title=hello.sc
val message = "Hello from Scala script"
println(message)
```

A script is run with the `scala-cli` command:

<ChainedSnippets>

```bash
scala-cli hello.sc
```

```text
Hello from Scala script
```

</ChainedSnippets>

The way this works is that a script is wrapped in an `object` before it's passed to the Scala compiler, and a `main`
method is added to it.
In the previous example, when the `hello.sc` script is passed to the compiler, the altered code looks like this:

```scala
object hello {
  val message = "Hello from Scala script"
  println(message)

  def main(args: Array[String]): Unit = ()
}
```

The name `hello` comes from the file name, `hello.sc`.

When a script is in a sub-directory, a package name is also inferred:

```scala title=my-app/constants/messages.sc
def hello = "Hello from Scala scripts"
```

```scala title=my-app/main.sc
import constants.messages
println(messages.hello)
```

Please note: when referring to code from another script, the actual relative path from the project root is used for the
package path. In the example above, as `messages.sc` is located in the `my-app/constants/` directory, to use the `hello`
function you have to call `constants.messages.hello`.

When referring to code from a piped script, just use its wrapper name: `stdin`.

<ChainedSnippets>

```bash
echo '@main def main() = println(stdin.message)' > PrintMessage.scala
echo 'def message: String = "Hello"' | scala-cli PrintMessage.scala _.sc
```

```text
Hello
```

</ChainedSnippets>

To specify a main class when running a script, use this command:

<ChainedSnippets>

```bash
scala-cli my-app --main-class main_sc
````

```text
Hello from Scala scripts
```

</ChainedSnippets>

Both of the previous scripts (`hello.sc` and `main.sc`) automatically get a main class, so this is required to
disambiguate them. If a main class coming from a regular `.scala` file is present in your app's context, that will be
run by default if the `--main-class` param is not explicitly specified.

When in doubt, you can always list the main classes present in your app by passing `--list-main-classes`.

<ChainedSnippets>

```bash
echo '@main def main1() = println("main1")' > main1.scala
echo '@main def main2() = println("main2")' > main2.scala
echo 'println("on-disk script")' > script.sc
echo 'println("piped script")' | scala-cli --list-main-classes _.sc main1.scala main2.scala script.sc
```

```text
stdin_sc script_sc main2 main1
```

</ChainedSnippets>

### Self executable Scala Script

You can define a file with the “shebang” header to be self-executable. Please remember to use `scala-cli shebang`
command, which makes `scala-cli` compatible with Unix shebang interpreter directive. For example, given this script:

```scala title=HelloScript.sc
#!/usr/bin/env -S scala-cli shebang
println("Hello world")
```

You can make it executable and run it, just like any other shell script:

<ChainedSnippets>

```bash
chmod +x HelloScript.sc
./HelloScript.sc
```

```text
Hello world
```

</ChainedSnippets>

It is also possible to set `scala-cli` command-line options in the shebang line, for example

```scala title=Shebang213.sc
#!/usr/bin/env -S scala-cli shebang --scala-version 2.13
```

### Arguments

You may also pass arguments to your script, and they are referenced with the special `args` variable:

```scala title=p.sc
#!/usr/bin/env -S scala-cli shebang

println(args(1))
```

<ChainedSnippets>

```bash
chmod +x p.sc
./p.sc hello world
```

```text
world
```

</ChainedSnippets>

### Difference with Ammonite scripts

[Ammonite](http://ammonite.io) is a popular REPL for Scala that can also compile and run `.sc` files.

`scala-cli` and Ammonite are similar, but differ significantly when your code is split in multiple scripts:

- In Ammonite, a script needs to use `import $file` directives to use values defined in another script
- With `scala-cli`, all scripts passed can reference each other without such directives

On the other hand:

- You can pass a single "entry point" script as input to Ammonite, and Ammonite finds the scripts it depends on via
  the `import $file` directives
- `scala-cli` requires all scripts to be passed beforehand, either one-by-one, or by putting them in a directory, and
  passing the directory to `scala-cli`
