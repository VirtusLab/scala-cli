---
title: Test
---

The `test` command allows to run test suites.

## Test framework

In order to run tests with it, add a test framework dependency to your
application. Some of the most popular test frameworks in Scala are
- [munit](https://scalameta.org/munit): `org.scalameta::munit::0.7.27`
- [utest](https://github.com/com-lihaoyi/utest): `com.lihaoyi::utest::0.7.10`
- [ScalaTest](https://www.scalatest.org): `org.scalatest::scalatest::3.2.9`
- [JUnit 4](https://junit.org/junit4), that can be used via a [dedicated interface](https://github.com/sbt/junit-interface): `com.github.sbt:junit-interface:0.13.2`

For example, let's run a simple munit-based test suite:
```bash
cat MyTests.scala
# import $ivy.`org.scalameta::munit::0.7.27`
#
# class MyTests extends munit.FunSuite {
#   test("foo") {
#     assert(2 + 2 == 4)
#   }
# }

scala-cli test MyTests.scala
# Compiling project_8686a5fa42 (1 Scala source)
# Compiled 'project_8686a5fa42'
# MyTests:
#   + foo 0.143s
```

## Test arguments

You can pass test arguments to your test framework, by passing them after a `--`:
```bash
cat MyTests.scala
# import $ivy.`org.scalatest::scalatest::3.2.9`
#
# import org.scalatest._
# import org.scalatest.flatspec._
# import org.scalatest.matchers._
#
# class Tests extends AnyFlatSpec with should.Matchers {
#   "A thing" should "thing" in {
#     assert(2 + 2 == 4)
#   }
# }

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
