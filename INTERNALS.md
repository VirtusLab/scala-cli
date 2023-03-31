# Internals overview

## Modules

Modules live under `modules/`. Each sub-directory there has a
corresponding mill module definition in `build.sc` (but for `integration`).

Most of the code currently lives in the `build` module.

The `cli` module depends on `build`, gets
packaged as a native-image executable, and distributed as `scala-cli` binary.

The other modules are either:
- integration tests
- utility modules, that `build` either:
  - depends on
  - fetches at run-time.

## Utility modules

These are:
- `runner`: simple app that starts a main class, catches any exception it throws and pretty-prints it.
- `test-runner`: finds test frameworks, test suites, and runs them
- `tasty-lib`: edits file names in `.tasty` files

## Tests

The tests live either in:
- `build`: unit tests
- `integration`: integration tests

Run unit tests with
```bash
./mill 'build[_].test'
```

Run integration tests with a JVM-based `scala-cli` with
```bash
./mill integration.test.jvm
```

Run integration tests with a native-image-based `scala-cli` with
```bash
./mill integration.test.native
```

## General workflow in most `scala-cli` commands

We roughly go from user inputs to byte code through 3 classes:
- `Inputs`: ADT for input files / directories.
- `Sources`: processed sources, ready to be passed to scalac
- `Build`: compilation result: success or failure.

Most commands
- take the arguments passed on the command-line: we have an `Array[String]`
- check whether each of them is a `.scala` file, an `.sc` file, a directory, …: we get an `Inputs` instance
- reads the directories, the `.scala` / `.sc` files: we get a `Sources` instance
- compile those sources: we get a `Build` instance
- do something with the build output (run it, run tests, package it, …)

In watch mode, we loop over the last 3 steps (`Inputs` is computed only once, the rest is re-computed upon file change).

## Source pre-processing

Some input files cannot be passed as is to scalac, if they are scripts (`.sc` files), which contain top-level statements

Scripts get wrapped. If the script `a/b/foo.sc` contains
```scala
val n = 2
```
we compile it as
```scala
package a.b
object foo {
val n = 2
def main(args: Array[String]): Unit = ()
}
```
Basically,
- its directory dictates its package
- we put its sources as is in an object
- we add a `main` method

## Build outputs post-processing

The source generation changes:
- file names, which now correspond to the directory where we write generated sources
- positions, when we wrap code (for `.sc` files)

As a consequence, some build outputs contains wrong paths or positions:
- diagnostics (warning and error messages) contain file paths and positions, used in reporting
- byte code contains file names and line numbers, that are used in stack traces
- semantic DBs contain relative file paths and positions, used by IDEs
- TASTy files contain relative file paths, used in pretty stack traces

We post-process those build outputs, to adjust positions and file paths of the generated sources:
various "mappings" are computed out of the generated sources list, and are used to adjust:
- diagnostics: done in memory, right before printing diagnostics
- byte code: done using the ASM library
- semantic DBs: we parse the semantic DBs, edit them in memory, and write them back on disk
- TASTy files: we partly parse them in memory, edit names that contain source file paths, and write them back on disk
