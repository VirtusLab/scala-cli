---
title: Package scala application to executable file
sidebar_position: 2
---

ScalaCli allows you to package your application into `bootstrap` JAR file, that can be easily run.
It only contains the source byte code with no dependencies that must be automatically downloaded on first run.

The following snippect contains short application to detecting the OS:
```scala name:DetectOsApp.scala
object DetectOSApp extends App  {
    def getOperatingSystem(): String = {
        val os: String = System.getProperty("os.name")
        os
    }
    println(s"os: ${getOperatingSystem()}")
}
```

### Default format (Bootstrap)

scala-cli `package` command generate executable `DetectOsApp` bootstrap file. 

```scala-cli
scala-cli package DetectOsApp.scala
```

<!-- Expected:
Wrote DetectOsApp, run it with
  ./DetectOsApp
-->

Bootstrap format require, that `java` command is available in the `PATH` and access to internet. 

```bash
# Run DetectOsApp on MacOs 
./DetectOsApp
# os: Mac OS X
```

```bash
# Using docker to run DetectOsApp on linux
docker run -it  -v $(pwd)/:/app -w /app openjdk:8-jre-slim bash
root@b0459df02263:/app# ./DetectOsApp
# os: Linux
``` 


### Assemblies
scala-cli `package --assembly` command generate executable `assemblies` or `fat JARs` file. 

```scala-cli
scala-cli package --assembly DetectOsApp.scala
```

The Assemblies format also requires that the `java` command be available in` PATH`, but in this case all dependencies are packed into file, so there is no need to automatically download anything before first run.

```bash
# Run DetectOsApp on MacOs 
./DetectOsApp
# os: Mac OS X
```

```bash
# Using docker to run DetectOsApp on linux
docker run -it  -v $(pwd)/:/app -w /app openjdk:8-jre-slim bash
root@b0459df02263:/app# ./DetectOsApp
# os: Linux
``` 