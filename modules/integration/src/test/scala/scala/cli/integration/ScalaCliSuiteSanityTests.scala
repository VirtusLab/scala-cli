package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.concurrent.Future

class ScalaCliSuiteSanityTests extends ScalaCliSuite {
  private val sanityTag: munit.Tag   = ScalaCliSuite.sanity
  private val scala2Tag: munit.Tag   = ScalaCliSuite.scala2Sanity
  private val scala212Tag: munit.Tag = ScalaCliSuite.scala212Sanity
  private val scala213Tag: munit.Tag = ScalaCliSuite.scala213Sanity

  private val allTests: Seq[Test] =
    new Test("untagged", () => Future.unit) +:
      Seq(sanityTag, scala2Tag, scala212Tag, scala213Tag)
        .map(tag => new Test(tag.value, () => Future.unit).tag(tag))

  private def selectedNames(suite: ScalaCliSuite): Set[String] =
    suite.selectSanityTests(allTests).map(_.name).toSet

  private def namesOf(tags: munit.Tag*): Set[String] = tags.map(_.value).toSet

  test("suites without a Scala version only select .sanity tests") {
    expect(selectedNames(new ScalaCliSuiteSanityTests) == namesOf(sanityTag))
  }

  test("Scala 3 suites only select .sanity tests") {
    expect(selectedNames(new DocTestsDefault) == namesOf(sanityTag))
    expect(selectedNames(new DocTests39Lts) == namesOf(sanityTag))
  }

  test("Scala 2.12 suites select .sanity, .scala2Sanity and .scala212Sanity tests") {
    expect(selectedNames(new DocTests212) == namesOf(sanityTag, scala2Tag, scala212Tag))
  }

  test("Scala 2.13 suites select .sanity, .scala2Sanity and .scala213Sanity tests") {
    expect(selectedNames(new DocTests213) == namesOf(sanityTag, scala2Tag, scala213Tag))
  }

  test("sanity modifiers tag tests") {
    expect("a".sanity.tags == Set(sanityTag))
    expect("a".scala2Sanity.tags == Set(scala2Tag))
    expect("a".scala212Sanity.tags == Set(scala212Tag))
    expect("a".scala213Sanity.tags == Set(scala213Tag))
    expect("a".flaky.scala2Sanity.tags == Set(munit.Flaky, scala2Tag))
  }
}
