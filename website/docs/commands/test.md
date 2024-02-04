---
title: Test
sidebar_position: 7
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

The `test` command runs test suites in the test sources.
Test sources are compiled separately (after the 'main' sources), and may use different dependencies, compiler options,
and other configurations.

By default, all command line options apply to both the main and test sources,
so [using directives](../guides/introduction/using-directives.md) can be used to provide test-specific configurations.

## Test sources

A source file is treated as test source if:

- the file name ends with `.test.scala`, or
- the file comes from a directory that is provided as input, and the relative path from that file to its original
  directory contains a `test` directory, or
- it contains the `//> using target.scope test` directive

:::caution
The `using target` directives are an experimental feature, and may change in future versions of Scala CLI.
:::

The second rule may sound a bit complicated, so let's explain it using following directory structure:

<ChainedSnippets>

```bash
tree example
```

```text
example
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

</ChainedSnippets>

Given that directory structure, let's analyze what file(s) will be treated as tests based on the provided inputs.

`scala-cli example` results in the following files being treated as test sources:

- `a.test.scala`, since it ends with `.test.scala`
- `src/test/scala/b.scala`, since the path to that directory contains a directory named `test`

Note that `e.scala` is not treated as a test source since it lacks a parent directory in its relative path that is
exactly named `test` (the name`test_unit` starts with `test`, but Scala CLI only looks for parent directories on the
relative path with the exact name `test`).

`scala-cli example/src` results in `src/test/scala/b.scala` being treated as a test file since its relative
path (`test/scala/b.scala`) contains a directory named `test`.

Conversely, `scala-cli example/src/test` results in no test sources, since the relative path to `b.scala` does not
contain `test` (the fact that the directory provided as input is named `test` does not make its content a test source).

Directives take precedence over file or path names, so `using target main` can be used to force `test/a.scala`
or `a.test.scala` to not be treated as tests.

As a rule of thumb, we recommend naming all of your test files with the `.test.scala` suffix.

## Test directives

When configuring your tests with `using` directives, it's usually advised to use their test scope equivalents, so that
only tests are affected.

For example, when declaring a test framework dependency, in most cases you wouldn't need it
when running your whole app, you only need it in tests. So rather than declare it globally with `using dep`, you can use
the `test.dep` directive:

```scala compile
//> using test.dep org.scalameta::munit::0.7.29
```

For more details on test directives,
see [the `using` directives guide](../guides/introduction/using-directives.md#directives-with-a-test-scope-equivalent).

## Test framework

In order to run tests with a test framework, add the framework dependency to your application.
Some of the most popular test frameworks in Scala are:

- [munit](https://scalameta.org/munit): `org.scalameta::munit::0.7.29`
- [utest](https://github.com/com-lihaoyi/utest): `com.lihaoyi::utest::0.8.2`
- [ScalaTest](https://www.scalatest.org): `org.scalatest::scalatest::3.2.17`
- [JUnit 4](https://junit.org/junit4), which can be used via
  a [dedicated interface](https://github.com/sbt/junit-interface): `com.github.sbt:junit-interface:0.13.3`
- [Weaver](https://disneystreaming.github.io/weaver-test/): `com.disneystreaming::weaver-cats:0.8.3`. You may need to
  specify weaver's test framework with `//> using testFramework "weaver.framework.CatsEffect"` if you had other test
  framework in your dependencies.

The following example shows how to run an munit-based test suite:

```scala title=MyTests.test.scala
//> using test.dep org.scalameta::munit::0.7.29

class MyTests extends munit.FunSuite {
  test("foo") {
    assert(2 + 2 == 4)
  }
}
```

<ChainedSnippets>

```bash
scala-cli test MyTests.test.scala
```

```text
Compiling project (1 Scala source)
Compiled project
MyTests:
  + foo 0.143s
```

</ChainedSnippets>

<!-- Expected:
MyTests
foo
-->

## Filter test suite

Passing the `--test-only` option to the `test` sub-command filters the test suites to be run:

```scala title=BarTests.test.scala
//> using test.dep org.scalameta::munit::0.7.29
package tests.only

class BarTests extends munit.FunSuite {
  test("bar") {
    assert(2 + 3 == 5)
  }
}
```

```scala title=HelloTests.test.scala
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

```scala title=BarTests.test.scala
//> using test.dep org.scalameta::munit::0.7.29
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

```scala title=MyTests.test.scala
//> using test.dep org.scalatest::scalatest::3.2.9

import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

class Tests extends AnyFlatSpec with should.Matchers {
  "A thing" should "thing" in {
    assert(2 + 2 == 4)
  }
}
```

<ChainedSnippets>

```bash
scala-cli test MyTests.test.scala -- -oD
```

```text
Compiling project (1 Scala source)
Compiled project
Tests:
A thing
- should thing (22 milliseconds)
Run completed in 359 milliseconds.
Total number of tests run: 1
Suites: completed 1, aborted 0
Tests: succeeded 1, failed 0, canceled 0, ignored 0, pending 0
All tests passed.
```

</ChainedSnippets>

<!-- Expected:
Tests:
A thing
should thing
All tests passed.
-->
