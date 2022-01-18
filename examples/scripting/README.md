<!--
  File was generated from based on docs/cookbooks/scripting.md, do not edit manually!
-->

# Develop Scripts in Scala

You can use scala-cli to write scripts with dependencies.

Let's try to build super-simple `ls` replacement in Scala. It's only feature is to
print all files located in path passed as the first argument

Create a file name `ls.sc`
```scala
#!/usr/bin/env scala-cli
//> using lib "com.lihaoyi::os-lib:0.7.8"
val cwd = if(args.nonEmpty) os.Path(args(0), os.pwd) else os.pwd
println(os.list(cwd).map(_.last).mkString("\n"))
```

Then you may just type

``` bash
./ls.sc .
./ls.sc /
./ls.sc ~
./ls.sc
```

#### Explaination:

 - `using` directive can be used to download and import dependencies
 - `args` is a special veriable of type `Array[String]` that contains command line arguments passed to the script