---
title: Scala Scripts with instant startup
sidebar_position: 8
---

import {ChainedSnippets, GiflikeVideo} from "../src/components/MarkdownComponents.js";

`scala-cli` allows to easly compile and run Scala Scripts.
It also allows for straightforward compilation with Scala Native. 
Scala Native is an ahead-of-time compiler to native binary allowing 
for instant startup times, meaning that along with scala-cli, it should 
perfectly suit the needs of a fast scripting tool.

## Using Scala Native

As an example, let’s build a script printing files from
a directory with sizes bigger than a passed value.

```scala compile title=size-higher-than.scala
//> using scala "3.1.1"
//> using lib "com.lihaoyi::os-lib::0.8.1"
 
@main
def sizeHigherThan(dir: String, minSizeMB: Int) =
  val wd = os.pwd / dir
  val files = os.walk.attrs(wd).collect{
    case (p, attrs) if attrs.size > minSizeMB * 10E6 => p
  }
  files.foreach(println(_))
```

Running this for a `dir` directory and 20 MB as a lower limit with
`scala-cli size-higher-than.scala – dir 20` can give us for example:


<ChainedSnippets>

```bash ignore
scala-cli size-higher-than.scala -- dir 20
```

```
Compiling project (Scala 3.1.1, JVM)
Compiled project (Scala 3.1.1, JVM)
/Users/user/Documents/workspace/dir/large-file.txt
```
</ChainedSnippets>

A keen eye will notice that we have not yet compiled to Scala Native. We are still running on the JVM!
We can fix that by either running with a `—-native` option, or,
in this case, by including an additional using directive:

```scala compile title=size-higher-than.scala
//> using scala "3.1.1"
//> using lib "com.lihaoyi::os-lib::0.8.1"
//> using platform "scala-native"
 
@main
def sizeHigherThan(dir: String, minSizeMB: Int) =
  val wd = os.pwd / dir
  val files = os.walk.attrs(wd).collect{
    case (p, attrs) if attrs.size > minSizeMB * 10E6 => p
  }
  files.foreach(println(_))
```

After rerunning, you may notice that while the initial compilation took a little longer,
subsequent runs will severely cut on the startup time compared to the JVM.

## Optimization options

We can make the runtime itself even faster, using various Scala Native optimization options:
* `debug` - what was used by default up to this point, fast compilation with a slower runtime 
* `release-fast` - moderate compilation time with a faster runtime
* `release-full` - slow compilation time with the fastest runtime

We pass these using a `-–native-mode` scala-cli option or, like previously, by adding a using directive:

```scala compile title=size-higher-than.scala
//> using scala "3.1.1"
//> using lib "com.lihaoyi::os-lib::0.8.1"
//> using platform "scala-native"
//> using nativeMode "release-full"
 
@main
def sizeHigherThan(dir: String, minSizeMB: Int) =
  val wd = os.pwd / dir
  val files = os.walk.attrs(wd).collect{
    case (p, attrs) if attrs.size > minSizeMB * 10E6 => p
  }
  files.foreach(println(_))
```

We can also package this script into a separate binary with the `package` command,
useful especially on Windows where typically shebangs won’t work:

## Additional considerations

Some things to look out for when working with Scala Native:
 * dependencies - libraries have to be published separately for Scala Native. Notice the `org::project::version` double colon syntax used for os-lib - it basically hides `org::project_native[Scala Native binary version]:version` underneath. Fortunately, many libraries are already available for Scala Native. However, Java dependencies will not work altogether.
 * some [differences](https://scala-native.readthedocs.io/en/stable/user/lang.html) exist when compared to Scala on the JVM.
