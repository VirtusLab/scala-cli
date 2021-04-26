package scala.cli.tests

import scala.cli.{Build, Inputs}
import scala.util.Properties

class BuildTests extends munit.FunSuite {

  test("simple") {
    val testInputs = TestInputs(
      files = Seq(
        os.rel / "simple.sc" ->
          """val n = 2
            |println(s"n=$n")
            |""".stripMargin
      )
    )
    val expectedCompilerOutput = Set(
      "simple.class",
      "simple$.class"
    )
    testInputs.withInputs { (root, inputs) =>
      val options = Build.Options(
        scalaVersion = "2.13.5",
        scalaBinaryVersion = "2.13"
      )
      val build = Build.build(inputs, options, TestLogger())
      val generated = os.walk(os.Path(build.output))
        .filter(os.isFile(_))
        .map(_.relativeTo(os.Path(build.output)))
      assert(generated.map(_.toString).toSet == expectedCompilerOutput)
    }
  }

  test("simple JS") {
    val testInputs = TestInputs(
      files = Seq(
        os.rel / "simple.sc" ->
          """val n = 2
            |println(s"n=$n")
            |""".stripMargin
      )
    )
    val expectedCompilerOutput = Set(
      "simple.class",
      "simple$.class",
      "simple.sjsir",
      "simple$.sjsir"
    )
    val scalaVersion = "2.13.5"
    val scalaBinaryVersion = "2.13"
    testInputs.withInputs { (root, inputs) =>
      val options = Build.Options(
        scalaVersion = scalaVersion,
        scalaBinaryVersion = scalaBinaryVersion,
        scalaJsOptions = Some(Build.scalaJsOptions(scalaVersion, scalaBinaryVersion))
      )
      val build = Build.build(inputs, options, TestLogger())
      val generated = os.walk(os.Path(build.output))
        .filter(os.isFile(_))
        .map(_.relativeTo(os.Path(build.output)))
      assert(
        generated.map(_.toString).toSet == expectedCompilerOutput,
        {
          pprint.log(generated)
          pprint.log(expectedCompilerOutput)
          ""
        }
      )
    }
  }

  def simpleNativeTest(): Unit = {
    val testInputs = TestInputs(
      files = Seq(
        os.rel / "simple.sc" ->
          """val n = 2
            |println(s"n=$n")
            |""".stripMargin
      )
    )
    val expectedCompilerOutput = Set(
      "simple.class",
      "simple$.class",
      // "simple.nir", // not sure why Scala Native doesn't generate this one.
      "simple$.nir"
    )
    val scalaVersion = "2.13.5"
    val scalaBinaryVersion = "2.13"
    testInputs.withInputs { (root, inputs) =>
      val options = Build.Options(
        scalaVersion = scalaVersion,
        scalaBinaryVersion = scalaBinaryVersion,
        scalaNativeOptions = Some(Build.scalaNativeOptions(scalaVersion, scalaBinaryVersion))
      )
      val build = Build.build(inputs, options, TestLogger())
      val generated = os.walk(os.Path(build.output))
        .filter(os.isFile(_))
        .map(_.relativeTo(os.Path(build.output)))
      assert(
        generated.map(_.toString).toSet == expectedCompilerOutput,
        {
          pprint.log(generated)
          pprint.log(expectedCompilerOutput)
          ""
        }
      )
    }
  }
  if (!Properties.isWin)
    test("simple native") {
      simpleNativeTest()
    }
}
