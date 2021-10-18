---
title: Test
sidebar_position: 7
---

The `test` command allows to run test suites that come from the test sources. 
Test sources are complied separately (after the 'main' sources) and may use different dependencies, compiler options and other configuration. 

All command line options applies to the both main and test sources so [using directives](http://localhost:3000/docs/20-guides/using-directives) (or [special imports](/docs/20-guides/configuration#special-imports)) can be used to provide test-specific configuration. 

## Test sources

Given source file is treated as test source if:
 - contains `using target test` directive
 - file name ends with `.test.scala`
 - file comes from a directory that is provided as input, and relative path from that file to its original directory contains `test` directory.

The last rule may sound a bit more complicated so lets explain it using following file structure:

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

Now, lets analyze what file will be treated as tests based on the provided inputs.

`scala-cli example` will result in following files treated as test sources:
- `a.test.scala` - since it ends with `.test.scala`
- `src/test/scala/b.scala` - since it comes from a directory and relative path to that directory contains `test`

Please note that e.scala was not treated as test source since it lacks directory named precisly `test` on its relative path to one of inputs (`test_unit` only starts with `test`).

`scala-cli example/src` will result in `src/test/scala/b.scala` being treated as tests since again its relative path to the one of the input directories (`test/scala/b.scala`) contains test directory.


`scala-cli example/src/test` will result in no test sources since the relative path to `b.scala` does not contains `test` (the fact that directory provided as input is named `test` does not make its content a test sources).

Directives take precedence over file or path names, so `using target main` can be used to force `test/a.scala` or `a.test.scala` to not be treated as tests.

As a rule of thumb, we recommend to simply name all your tests file with `.tests.scala` suffix.

## Test framework

In order to run tests with it, add a test framework dependency to your
application. Some of the most popular test frameworks in Scala are
- [munit](https://scalameta.org/munit): `org.scalameta::munit::0.7.27`
- [utest](https://github.com/com-lihaoyi/utest): `com.lihaoyi::utest::0.7.10`
- [ScalaTest](https://www.scalatest.org): `org.scalatest::scalatest::3.2.9`
- [JUnit 4](https://junit.org/junit4), that can be used via a [dedicated interface](https://github.com/sbt/junit-interface): `com.github.sbt:junit-interface:0.13.2`

For example, let's run a simple munit-based test suite:

```scala title=MyTests.scala
using lib org.scalameta::munit::0.7.27

class MyTests extends munit.FunSuite {
  test("foo") {
    assert(2 + 2 == 4)
  }
}
```

```bash
scala-cli test MyTests.scala
# Compiling project_8686a5fa42 (1 Scala source)
# Compiled 'project_8686a5fa42'
# MyTests:
#   + foo 0.143s
```

## Test arguments

You can pass test arguments to your test framework, by passing them after a `--`:

```scala title=MyTests.scala
import $ivy.`org.scalatest::scalatest::3.2.9`

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
# Compiling project_8686a5fa42-4bae49baeb (1 Scala source)
# Compiled 'project_8686a5fa42-4bae49baeb'
# Tests:
# A thing
# - should thing (22 milliseconds)
# Run completed in 359 milliseconds.
# Total number of tests run: 1
# Suites: completed 1, aborted 0
# Tests: succeeded 1, failed 0, canceled 0, ignored 0, pending 0
# All tests passed.
```
