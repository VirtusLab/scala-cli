---
title: Scripts
sidebar_position: 30
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

# Scripts

Scala CLI accepts Scala scripts as files that end in `.sc`.
Unlike `.scala` files, in scripts, any kind of statement is accepted at the top-level:

```scala title=hello.sc
val message = "Hello from Scala script"
println(message)
```

A script is run with the Scala CLI command:

<ChainedSnippets>

```bash
scala-cli hello.sc
```

```text
Hello from Scala script
```

</ChainedSnippets>

## Using multiple scripts together

When you pass multiple scripts to Scala CLI at once ([or add them with `//> using file ...`](#define-source-files-in-using-directives), they are all compiled together and can reference each other.
Their names are inferred from the file name e.g. `hello.sc` becomes `hello` and `main.sc` becomes `main`.

:::caution
Referencing a script from `main.sc` is not always possible.
More in [Scala 2 scripts wrapper](#scala-2-scripts-wrapper).
:::

```scala title=message.sc
def msg = "from Scala script"
```

```scala title=hello.sc
println("Hello " + message.msg)
```

<ChainedSnippets>

```bash
scala-cli hello.sc message.sc
```

```text
Hello from Scala script
```

</ChainedSnippets>

When a script is in a sub-directory, a package name is also inferred:

```scala title=my-app/constants/message.sc
def msg = "Hello from Scala scripts"
```

```scala title=my-app/main.sc
import constants.message
println("Hello " + message.msg)
```

Please note: when referring to code from another script, the actual relative path from the project root is used for the
package path. In the example above, as `message.sc` is located in the `my-app/constants/` directory, to use the `msg`
function you have to call `constants.message.msg`.

When referencing code from a piped script, just use `stdin`.

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
```

```text
Hello from Scala scripts
```

</ChainedSnippets>

:::caution
When specifying a main class from Scala 2 scripts, you need to use the script file name without the `_sc` suffix.
More in [Scala 2 scripts wrapper](#scala-2-scripts-wrapper).
:::

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

## Define source files in using directives

You can also add source files with the using directive `//> using file` in Scala scripts:

```scala title=main.sc
//> using file Utils.scala

println(Utils.message)
```

```scala title=Utils.scala
object Utils {
  val message = "Hello World"
}
```

Scala CLI takes into account and compiles `Utils.scala`.

<ChainedSnippets>

```bash
scala-cli main.sc
```

```text
Hello World
```

</ChainedSnippets>

<!-- Expected:
Hello World
-->

## Self executable Scala Script

You can define a file with the “shebang” header to be self-executable. Please remember to use `scala-cli shebang`
command, which makes Scala CLI compatible with Unix shebang interpreter directive. For example, given this script:

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

It is also possible to set Scala CLI command-line options in the shebang line, for example

```scala title=Shebang213.sc
#!/usr/bin/env -S scala-cli shebang --scala-version 2.13
```

The command `shebang` also allows script files to be executed even if they have no file extension,
provided they start with the [`shebang` header](../guides/shebang.md#shebang-script-headers).
Note that those files are always run as scripts even though they may contain e.g. valid `.scala` program.

## Arguments

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

## The name of script

You can access the name of the running script inside the script itself using the special `scriptPath` variable:

```scala title=script.sc
#!/usr/bin/env -S scala-cli shebang

println(scriptPath)
```

<ChainedSnippets>

```bash
chmod +x script.sc
./script.sc
```

```text
./script.sc
```

<!-- Expected:
./script.sc
-->

</ChainedSnippets>

## Script wrappers

The compilation and execution of a source file containing top-level definitions is possible due to the script's code being wrapper in an additional construct and given a `main` method.
Scala CLI as of version v1.1.0 uses three kinds of script wrappers depending on the project's configuration.
They each differ slightly and have different capabilities and limitations.

### Scala 2 scripts wrapper

For scripts compiled with Scala 2.12 and 2.13 there's only a single wrapper available.
It uses an object extending the `App` trait to wrap the user's code.

**Limitations**  
Thanks to the mechanics of `App` in Scala 2, this wrapper has no reported limitations when it comes to the code that can be run in it. 

**Differences in behaviour**  
- It is not possible to reference contents of a script from a file called `main.sc`, as the name `main` clashes with a `main` method each wrapper contains.
- The main class name is the name of the script file without the `.sc` suffix. For example, `hello.sc` becomes `hello`.

### Scala 3 scripts wrappers

For Scala 3 there are two wrappers available:
- Class Wrapper - default wrapper for Scala 3 scripts
- Object Wrapper - extra wrapper that can be forced with `--object-wrapper` flag and `>// using  objectWrapper` directive

#### Class Wrapper
This wrapper is the default for scripts in Scala 3, however, it cannot be used when the script is compiled for the JS platform, [Object Wrapper](#object-wrapper) is then used.
Due to the usage of `export` keyword it is not possible to use it in Scala 2.

**Limitations**  
- Can't be used with scripts compiled for the JS platform
- Can't be used in Scala 2
- When referencing types defined in the script, the type's path can be different from expected and compilation may fail with:  
`Error: Unexpected error when compiling project: 'assertion failed: asTerm called on not-a-Term val <none>'`

**Differences in behaviour**  
The Class Wrapper's behaviour is the default described throughout the documentation.

#### Object Wrapper
This wrapper is an alternative to the [Class Wrapper](#class-wrapper) and can be forced with `--object-wrapper` flag and `>// using  objectWrapper` directive.
It is used by default for Scala 3 scripts compiled for JS platform. Can suffer from deadlocks then using multithreaded code.

**Limitations**  
- When running background threads from the script and using e.g. `scala.concurrent.Await` on them may result in a deadlock due to unfinished initialization of the wrapper object.

**Differences in behaviour**  
The Object Wrapper's behaviour is the default described throughout the documentation.

### Summary
The wrapper type used according to the configuration used ((platform + forced type) X Scala version) is summarized in the table below:

|                             | Scala 2.12  | Scala 2.13  | Scala 3        |
|-----------------------------|-------------|-------------|----------------|
| `>// using platform jvm`    | App Wrapper | App Wrapper | Class Wrapper  |
| `>// using platform native` | App Wrapper | App Wrapper | Class Wrapper  |
| `>// using platform js`     | App Wrapper | App Wrapper | Object Wrapper |
| `>// using objectWrapper`   | App Wrapper | App Wrapper | Object Wrapper |

## Differences with Ammonite scripts

[Ammonite](http://ammonite.io) is a popular REPL for Scala that can also compile and run `.sc` files.

Scala CLI and Ammonite are similar, but differ significantly when your code is split in multiple scripts:

- In Ammonite, a script needs to use `import $file` directives to use values defined in another script
- With Scala CLI, all scripts passed can reference each other without such directives

On the other hand:

- You can pass a single "entry point" script as input to Ammonite, and Ammonite finds the scripts it depends on via
  the `import $file` directives
- Scala CLI requires all scripts to be added with `//> using file ...` or to be passed beforehand, either one-by-one, or by putting them in a directory, and
  passing the directory to Scala CLI