//> using lib "org.scalatest::scalatest::3.2.9"

import org.scalatest._
import flatspec._
import matchers._

class Tests extends AnyFlatSpec with should.Matchers {
  "A thing" should "thing" in {
    assert(2 + 2 == 4)
  }
}
