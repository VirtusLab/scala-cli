//> using dep "com.lihaoyi::utest::0.7.10"

import utest._

object MyTests extends TestSuite {
  val tests = Tests {
    test("foo") {
      assert(2 + 2 == 4)
    }
    // test("nope") {
    //   assert(2 + 2 == 5)
    // }
  }
}
