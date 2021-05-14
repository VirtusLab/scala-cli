package scala.cli.tests

import scala.cli.{Build, Inputs}
import scala.cli.tests.TestUtil._
import scala.util.Properties

class BuildTests extends munit.FunSuite {

  val defaultOptions = Build.Options(
    scalaVersion = "2.13.5",
    scalaBinaryVersion = "2.13"
  )

  test("simple") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """val n = 2
          |println(s"n=$n")
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions) { (root, inputs, build) =>
      build.assertGeneratedEquals(
        "simple.class",
        "simple$.class"
      )
    }
  }

  test("simple JS") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """val n = 2
          |println(s"n=$n")
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions.enableJs) { (root, inputs, build) =>
      build.assertGeneratedEquals(
        "simple.class",
        "simple$.class",
        "simple.sjsir",
        "simple$.sjsir"
      )
    }
  }

  def simpleNativeTest(): Unit = {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """val n = 2
          |println(s"n=$n")
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions.enableNative) { (root, inputs, build) =>
      build.assertGeneratedEquals(
        "simple.class",
        "simple$.class",
        // "simple.nir", // not sure why Scala Native doesn't generate this one.
        "simple$.nir"
      )
    }
  }
  if (!Properties.isWin)
    test("simple native") {
      simpleNativeTest()
    }

  test("dependencies") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """import $ivy.`com.lihaoyi::geny:0.6.5`
          |import geny.Generator
          |val g = Generator("Hel", "lo")
          |println(g.mkString)
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions) { (root, inputs, build) =>
      build.assertGeneratedEquals(
        "simple.class",
        "simple$.class"
      )
    }
  }

  test("several dependencies") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """import $ivy.`com.lihaoyi::geny:0.6.5`
          |import $ivy.`com.lihaoyi::pprint:0.6.4`
          |import geny.Generator
          |val g = Generator("Hel", "lo")
          |pprint.log(g)
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions) { (root, inputs, build) =>
      build.assertGeneratedEquals(
        "simple.class",
        "simple$.class"
      )
    }
  }

}
