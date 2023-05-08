package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.build.CollectionOps.distinctBy
class DistinctByTests extends munit.FunSuite {
  case class Message(a: String, b: Int)
  val distinctData = Seq(
    Message(a = "1", b = 4),
    Message(a = "2", b = 3),
    Message(a = "3", b = 2),
    Message(a = "4", b = 1)
  )
  val repeatingData = Seq(
    Message(a = "1", b = 4),
    Message(a = "1", b = 44),
    Message(a = "2", b = 3),
    Message(a = "22", b = 3),
    Message(a = "3", b = 22),
    Message(a = "33", b = 2),
    Message(a = "4", b = 1),
    Message(a = "4", b = 11)
  )

  test("distinctBy where data is already distinct") {
    val distinctByA     = distinctData.distinctBy(_.a)
    val distinctByB     = distinctData.distinctBy(_.b)
    val generalDistinct = distinctData.distinct
    expect(distinctData == generalDistinct)
    expect(distinctData == distinctByA)
    expect(distinctData == distinctByB)
  }

  test("distinctBy doesn't change data order") {
    val expectedData = Seq(
      Message(a = "1", b = 4),
      Message(a = "2", b = 3),
      Message(a = "22", b = 3),
      Message(a = "3", b = 22),
      Message(a = "33", b = 2),
      Message(a = "4", b = 1)
    )
    expect(repeatingData.distinctBy(_.a) == expectedData)
  }
}
