package scala.build

import EitherCps._

class CPSTest extends munit.FunSuite {

  val failed1: Either[Int, String] = Left(1)
  val ok: Either[Int, String]      = Right("OK")

  def checkResult(expected: Either[Int, String])(res: => Either[Int, String]) =
    assertEquals(expected, res)

  test("Basic CPS test") {
    checkResult(Right("OK"))(either("OK"))
    checkResult(Right("OK"))(either(value(ok)))
    checkResult(Left(1))(either(value(failed1)))
  }

  test("Exceptions") {
    intercept[IllegalArgumentException](either(throw new IllegalArgumentException("test")))

    intercept[IllegalArgumentException](either {
      throw new IllegalArgumentException("test")
      value(failed1)
    })
  }

  test("early return") {
    checkResult(Left(1)) {
      either {
        value(failed1)
        throw new IllegalArgumentException("test")
      }
    }
  }

  class E
  class EE  extends E
  class EEE extends E

  class V
  class VV extends V

  class E2 { def ala = 123 }
  class V2

  val ee: Either[EE, V] = Left(new EE)
  val vv: Either[E, VV] = Right(new VV)

  def ee2: Either[E, V] = either {
    value(Left(new EEE))
    value(vv.left.map(_ => new EE))
    new V
  }

  // Mainly to see if compiles
  test("variance 123") {
    val errorRes: Either[E, V] = either(value(ee))
    assert(ee == errorRes)

    val valueRes: Either[E, V] = either(value(vv))
    assert(vv == valueRes)

  }

  test("fallback to Right") {
    val res = new V2
    val a: Either[E2, V2] = either {
      value(Right(res))
    }
    assert(Right(res) == a)
  }
}
