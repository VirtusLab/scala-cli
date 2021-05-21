import $ivy.`org.scalameta::munit::0.7.25`
import $ivy.`org.scala-lang:scala-reflect:2.12.13`

class MyTests extends munit.FunSuite {

  test("foo") {
    assert(2 + 2 == 4)
  }
  test("nope") {
    assert(2 + 2 == 5)
  }
}

