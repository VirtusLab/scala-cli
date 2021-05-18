package scala.cli.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.Sources
import scala.cli.internal.CustomCodeWrapper
import scala.cli.tests.TestUtil._

class SourcesTests extends munit.FunSuite {

  def scalaVersion = "2.13.5"
  def scalaBinaryVersion = "2.13"

  test("dependencies in .scala") {
    val testInputs = TestInputs(
      os.rel / "something.scala" ->
        """import $ivy.`org1:name1:1.1`
          |import $ivy.`org2::name2:2.2`
          |import $ivy.`org3:::name3:3.3`
          |import scala.collection.mutable
          |
          |object Something {
          |  def a = 1
          |}
          |""".stripMargin
    )
    val expectedDeps = Seq(
      coursierapi.Dependency.of("org1", "name1", "1.1"),
      coursierapi.Dependency.of("org2", "name2_" + scalaBinaryVersion, "2.2"),
      coursierapi.Dependency.of("org3", "name3_" + scalaVersion, "3.3")
    )
    testInputs.withInputs { (root, inputs) =>
      val sources = Sources(root, inputs, CustomCodeWrapper, "", scalaVersion, scalaBinaryVersion)

      expect(sources.dependencies == expectedDeps)
      expect(sources.paths.isEmpty)
      expect(sources.inMemory.length == 1)
      expect(sources.inMemory.map(_._2) == Seq(os.rel / "something.scala"))
    }
  }

  test("dependencies in .sc") {
    val testInputs = TestInputs(
      os.rel / "something.sc" ->
        """import $ivy.`org1:name1:1.1`
          |import $ivy.`org2::name2:2.2`
          |import $ivy.`org3:::name3:3.3`
          |import scala.collection.mutable
          |
          |def a = 1
          |""".stripMargin
    )
    val expectedDeps = Seq(
      coursierapi.Dependency.of("org1", "name1", "1.1"),
      coursierapi.Dependency.of("org2", "name2_" + scalaBinaryVersion, "2.2"),
      coursierapi.Dependency.of("org3", "name3_" + scalaVersion, "3.3")
    )
    testInputs.withInputs { (root, inputs) =>
      val sources = Sources(root, inputs, CustomCodeWrapper, "", scalaVersion, scalaBinaryVersion)

      expect(sources.dependencies == expectedDeps)
      expect(sources.paths.isEmpty)
      expect(sources.inMemory.length == 1)
      expect(sources.inMemory.map(_._2) == Seq(os.rel / "something.scala"))
    }
  }

}
