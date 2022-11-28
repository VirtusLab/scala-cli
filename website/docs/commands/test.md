---
title: Test
sidebar_position: 7
---

The `test` command runs test suites in the test sources.
Test sources are complied separately (after the 'main' sources), and may use different dependencies, compiler options, and other configurations.

By default, all command line options apply to both the main and test sources, so [using directives](/docs/guides/using-directives.md) (or [special imports](../guides/configuration#special-imports)) can be used to provide test-specific configurations.

## Test sources

A source file is treated as test source if:

 - it contains the `//> using target.scope "test"` directive, or
 - the file name ends with `.test.scala`, or
 - the file comes from a directory that is provided as input, and the relative path from that file to its original directory contains a `test` directory

The last rule may sound a bit complicated, so let's explain it using following directory structure:

```bash ignore
example/
├── a.scala
├── a.test.scala
└── src
    ├── main
    │   └── scala
    │       └── d.scala
    ├── test
    │   └── scala
    │       └── b.scala
    └── test_unit
        └── scala
            └── e.scala
```

Given that directory structure, let's analyze what file(s) will be treated as tests based on the provided inputs.

`scala-cli example` results in the following files being treated as test sources:

- `a.test.scala`, since it ends with `.test.scala`
- `src/test/scala/b.scala`, since the path to that directory contains a directory named `test`

Note that `e.scala` is not treated as a test source since it lacks a parent directory in its relative path that is exactly named `test` (the name`test_unit` starts with `test`, but `scala-cli` only looks for parent directories on the relative path with the exact name `test`).

`scala-cli example/src` results in `src/test/scala/b.scala` being treated as a test file since its relative path (`test/scala/b.scala`) contains a directory named `test`.

Conversely, `scala-cli example/src/test` results in no test sources, since the relative path to `b.scala` does not contain `test` (the fact that the directory provided as input is named `test` does not make its content a test source).

Directives take precedence over file or path names, so `using target main` can be used to force `test/a.scala` or `a.test.scala` to not be treated as tests.

As a rule of thumb, we recommend naming all of your test files with the `.test.scala` suffix.

## Test framework

In order to run tests with a test framework, add the framework dependency to your application.
Some of the most popular test frameworks in Scala are:
- [munit](https://scalameta.org/munit): `org.scalameta::munit::0.7.27`
- [utest](https://github.com/com-lihaoyi/utest): `com.lihaoyi::utest::0.7.10`
- [ScalaTest](https://www.scalatest.org): `org.scalatest::scalatest::3.2.9`
- [JUnit 4](https://junit.org/junit4), which can be used via a [dedicated interface](https://github.com/sbt/junit-interface): `com.github.sbt:junit-interface:0.13.2`

The following example shows how to run an munit-based test suite:

```scala title=MyTests.scala
//> using lib "org.scalameta::munit::0.7.27"

class MyTests extends munit.FunSuite {
  test("foo") {
    assert(2 + 2 == 4)
  }
}
```

```bash
scala-cli test MyTests.scala
# Compiling project (1 Scala source)
# Compiled project
# MyTests:
#   + foo 0.143s
```

<!-- Expected:
MyTests
foo
-->

## Filter test suite

Passing the `--test-only` option to the `test` sub-command filters the test suites to be run:

```scala title=BarTests.scala
//> using lib "org.scalameta::munit::0.7.29"
package tests.only

class BarTests extends munit.FunSuite {
  test("bar") {
    assert(2 + 3 == 5)
  }
}
```
```scala title=HelloTests.scala
package tests

class HelloTests extends munit.FunSuite {
  test("hello") {
    assert(2 + 2 == 4)
  }
}
```

```bash
scala-cli test . --test-only 'tests.only*' 
# tests.only.BarTests:
#   + bar 0.045s
```

<!-- Expected:
tests.only.BarTests:
+ bar
-->

## Filter test case 

### Munit

To run a specific test case inside the unit test suite pass `*exact-test-name*` as an argument to scala-cli:

```scala title=BarTests.scala
//> using lib "org.scalameta::munit::0.7.29"
package tests.only

class Tests extends munit.FunSuite {
  test("bar") {
    assert(2 + 2 == 5)
  }
  test("foo") {
    assert(2 + 3 == 5)
  }
  test("foo-again") {
    assert(2 + 3 == 5)
  }
}
```
```bash
scala-cli test . --test-only 'tests.only*'  -- '*foo*'
# tests.only.Tests:
#   + foo 0.045s
#   + foo-again 0.001s
```

<!-- Expected:
tests.only.Tests:
+ foo
+ foo-again
-->


## Test arguments

You can pass test arguments to your test framework by passing them after `--`:

```scala title=MyTests.scala
//> using lib "org.scalatest::scalatest::3.2.9"

import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

class Tests extends AnyFlatSpec with should.Matchers {
  "A thing" should "thing" in {
    assert(2 + 2 == 4)
  }
}
```

```bash
scala-cli test MyTests.scala -- -oD
# Compiling project (1 Scala source)
# Compiled project
# Tests:
# A thing
# - should thing (22 milliseconds)
# Run completed in 359 milliseconds.
# Total number of tests run: 1
# Suites: completed 1, aborted 0
# Tests: succeeded 1, failed 0, canceled 0, ignored 0, pending 0
# All tests passed.
```

<!-- Expected:
Tests:
A thing
should thing
All tests passed.
-->
