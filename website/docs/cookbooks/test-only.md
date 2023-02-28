---
title: Filter the test suites to run
sidebar_position: 11
---

The `--test-only` option is useful for when you only want to run a specific subset of tests.

In this cookbook, we will walk through how to use the `--test-only` option with the `test` sub-command in Scala CLI,
specifically when using the `munit` and `utest` test frameworks.

## Filter the test suites

The `--test-only` option in Scala CLI supports using glob patterns to filter test suites to run. A glob pattern is a
string which contains asterisk `*` characters to match a set of test suites.

Here are three examples of glob patterns for how to filter test suites with `--test-only`:

- start with `test` - `test*`
- end with `test` - `*test`
- contains `test` -  `*test*`

:::note
The `--test-only` option is supported for every test framework running with Scala CLI.
:::

For example, passing `tests.only*` to the `--test-only` option runs only the test suites which start with `tests.only`:

```scala title=BarTests.scala
//> using dep "org.scalameta::munit::0.7.29"
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

## Running a single test case by name

:::note
Test frameworks may have their own specific API for specifying the test cases to run aside from the test-only input.
:::

### Munit

To run a specific test case inside a test suite pass `*test-name*` as an argument to Scala CLI:

<!-- clear -->

```scala title=MunitTests.scala
//> using dep "org.scalameta::munit::0.7.29"
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
scala-cli test .  -- '*foo*'
# tests.only.Tests:
#   + foo 0.045s
#   + foo-again 0.001s
```

<!-- Expected:
tests.only.Tests:
+ foo
+ foo-again
-->

This will only run the test which contains the name `foo`, any other tests in your test suite will be skipped.

### Utest

Unfortunately, the `utest` test framework doesn't support using glob pattern `*` to filter the test cases to run. In
order to run a specific test case you will need to specify the exact name of the test you want to run.

<!-- clear -->

```scala title=MyTests.scala
//> using dep "com.lihaoyi::utest::0.7.10"

import utest._

object MyTests extends TestSuite {
  val tests = Tests {
    test("foo") {
      assert(2 + 2 == 4)
      println("Hello from " + "tests")
    }
    test("bar") {
      assert(2 + 2 == 4)
      println("Hello from " + "tests")
    }
  }
}
```

```bash
scala-cli test .  -- 'MyTests.foo'
# -------------------------- Running Tests MyTests.foo --------------------------
# Hello from tests
# + MyTests.foo 8ms  
# Tests: 1, Passed: 1, Failed: 0
```

<!-- Expected:
Running Tests MyTests.foo
Hello from tests
+ MyTests.foo
Tests: 1, Passed: 1, Failed: 0
-->

This will run only the test case with the name `MyTests.foo`.
