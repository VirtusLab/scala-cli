---
title: Use Scala CLI in GitHub Actions
sidebar_position: 9
---

## Preparing simple aplication

Scala CLI lets you run, test, and package Scala code in various environments, including GitHub CI. 
To use Scala CLI features in a simple way you can use the GitHub Actions [scala-cli-setup](https://github.com/VirtusLab/scala-cli-setup) that installs everything necessary to run your Scala CLI application and more.

For example, here's a simple `ls` application printing the files in a given directory:
```scala title=Ls.scala
//> using scala 2.13
//> using dep com.lihaoyi::os-lib:0.7.8

@main def hello(args: String*) =
  val path = args.headOption match
    case Some(p) => os.Path(p, os.pwd)
    case _       => os.pwd

  if (os.isDir(path)) println(os.list(path).mkString(","))
  else System.err.println("Expected directory path as an input")
```

and some tests for `ls` application:

```scala title=TestsLs.test.scala
//> using dep org.scalameta::munit::0.7.27
import scala.util.Properties

class TestsLs extends munit.FunSuite {
  test("ls") {
    // prepare test directory
    val tempDir = os.temp.dir()
    // create files
    val expectedFiles = Seq("Ls", "Hello").map(tempDir / _)
    expectedFiles.foreach(os.write(_, "Hello"))

    // check
    val scalaCLILauncher = if(Properties.isWin) "scala-cli.bat" else "scala-cli"
    val foundFiles =
      os.proc(scalaCLILauncher, "Ls.scala", "--", tempDir).call().out.trim()

    expectedFiles.map(_.toString).foreach { file =>
      assert(foundFiles.contains(file))
    }
  }
}

```

## Run tests in Github CI

The following configuration of `ci.yml` contains a definition of job that runs tests using Scala CLI for every platform defined in `matrix.OS`.

```yaml
jobs:
  build:
    runs-on: ${{ matrix.OS }}
    strategy:
      matrix:
        OS: ["ubuntu-latest", "macos-latest", "windows-latest"]
    steps:
    - uses: actions/checkout@v3
      with:
        fetch-depth: 0
    - uses: coursier/cache-action@v6.3
    - uses: VirtusLab/scala-cli-setup@v0.1
    - run: scala-cli test .
```

## Check your Scala code format

To check the code style of your sources, you can use [Scalafmt](https://scalameta.org/scalafmt/). 


To check your code format in GitHub CI by adding new job `format`:
```yaml
  format:
    runs-on: "ubuntu-latest"
    steps:
    - uses: actions/checkout@v3
      with:
        fetch-depth: 0
    - uses: coursier/cache-action@v6.3
    - uses: VirtusLab/scala-cli-setup@v0.1
    - name: Scalafmt check
      run: |
        scala-cli fmt --check . || (
          echo "To format code run"
          echo "  scala-cli fmt ."
          exit 1
        )
```

If the `scala-cli fmt --check .` command fails, it can be easily fixed by running `scala-cli fmt .`, which correctly formats your code.

## Package your application

Scala CLI allows to build native executable applications using [GraalVM](https://www.graalvm.org), which can be uploaded as GitHub release artifacts.

```yaml
    - name: Package app
      run: scala-cli .github/scripts/package.sc
```

Given this simple Scala Script `package.sc` to package application to every platform:
```scala title=package.sc
//> using scala 3.1.2
//> using dep com.lihaoyi::os-lib:0.8.0
import scala.util.Properties

val platformSuffix: String = {
  val os =
    if (Properties.isWin) "pc-win32"
    else if (Properties.isLinux) "pc-linux"
    else if (Properties.isMac) "apple-darwin"
    else sys.error(s"Unrecognized OS: ${sys.props("os.name")}")
  os
}
val artifactsPath = os.Path("artifacts", os.pwd)
val destPath =
  if (Properties.isWin) artifactsPath / s"ls-$platformSuffix.exe"
  else artifactsPath / s"ls-$platformSuffix"
val scalaCLILauncher =
  if (Properties.isWin) "scala-cli.bat" else "scala-cli"

os.makeDir(artifactsPath)
os.proc(scalaCLILauncher,"--power",  "package", ".", "-o", destPath, "--native-image")
  .call(cwd = os.pwd)
  .out
  .text()
  .trim
```


## Distribute generated native application

To upload generated native executable applications to artifacts you can use [upload-artifact](https://github.com/actions/upload-artifact) GitHub Actions.

```yaml
    - uses: actions/upload-artifact@v3
      with:
        name: launchers
        path: artifacts
        if-no-files-found: error
        retention-days: 2
```

When release CI pass, you should be able to download artifacts that contain native launchers of your applications.

[Here](https://github.com/lwronski/ls-scala-cli-demo/actions/runs/2376334882) you can find examples of a CI that contains generated launcher based on this cookbook.

You can find the code of this cookbook [here](https://github.com/lwronski/ls-scala-cli-demo).