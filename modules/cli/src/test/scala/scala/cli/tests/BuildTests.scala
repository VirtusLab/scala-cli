package scala.cli.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.{Build, Inputs}
import scala.cli.tests.TestUtil._
import scala.meta.internal.semanticdb.TextDocuments
import scala.util.Properties

class BuildTests extends munit.FunSuite {

  val defaultOptions = Build.Options(
    scalaVersion = "2.13.5",
    scalaBinaryVersion = "2.13"
  )

  val defaultScala3Options = defaultOptions.copy(
    scalaVersion = "3.0.0",
    scalaBinaryVersion = "3"
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

  test("scala 3") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """val n = 2
          |println(s"n=$n")
          |""".stripMargin
    )
    testInputs.withBuild(defaultScala3Options) { (root, inputs, build) =>
      build.assertGeneratedEquals(
        "simple.class",
        "simple$.class",
        "simple.tasty"
      )
    }
  }

  test("semantic DB") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """val n = 2
          |println(s"n=$n")
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions.copy(generateSemanticDbs = true)) { (root, inputs, build) =>
      build.assertGeneratedEquals(
        "simple.class",
        "simple$.class",
        "META-INF/semanticdb/simple.sc.semanticdb"
      )

      val outputDir = build.outputOpt.getOrElse(sys.error("no build output???"))
      val semDb = os.read.bytes(outputDir / "META-INF" / "semanticdb" / "simple.sc.semanticdb")
      val doc = TextDocuments.parseFrom(semDb)
      val uris = doc.documents.map(_.uri)
      expect(uris == Seq("simple.sc"))
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
