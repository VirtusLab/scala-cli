import $ivy.`com.lihaoyi::utest::0.7.9`, utest._

object MyTests extends TestSuite {
  val tests = Tests {
    test("foo") {
      assert(2 + 2 == 4)
    }
    test("nope") {
      assert(2 + 2 == 5)
    }
  }
}
