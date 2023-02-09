//> using dep "org.scalameta::munit::0.7.29"

class MyTests extends munit.FunSuite {
  test("foo") {
    assert(2 + 2 == 4)
  }
  // test("nope") {
  //   assert(2 + 2 == 5)
  // }
}
