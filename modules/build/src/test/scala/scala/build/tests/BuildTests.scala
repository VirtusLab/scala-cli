package scala.build.tests

import java.io.IOException

import ch.epfl.scala.bsp4j
import com.eed3si9n.expecty.Expecty.expect

import scala.build.blooprifle.BloopRifleConfig
import scala.build.{Bloop, Build, BuildThreads, Directories, Inputs}
import scala.build.options.{BuildOptions, ClassPathOptions, InternalOptions, ScalaOptions}
import scala.build.tests.TestUtil._
import scala.meta.internal.semanticdb.TextDocuments
import scala.util.Properties
import scala.build.tastylib.TastyData
import scala.build.Logger
import scala.build.LocalRepo

class BuildTests extends munit.FunSuite {

  val buildThreads = BuildThreads.create()
  val bloopConfig  = BloopRifleConfig.default(() => Bloop.bloopClassPath(Logger.nop))

  val extraRepoTmpDir = os.temp.dir(prefix = "scala-cli-tests-extra-repo-")
  val directories     = Directories.under(extraRepoTmpDir)

  override def afterAll(): Unit = {
    TestInputs.tryRemoveAll(extraRepoTmpDir)
    buildThreads.shutdown()
  }

  def sv2 = "2.13.5"
  val defaultOptions = BuildOptions(
    scalaOptions = ScalaOptions(
      scalaVersion = Some(sv2),
      scalaBinaryVersion = None
    ),
    internal = InternalOptions(
      localRepository = LocalRepo.localRepo(directories.localRepoDir)
    )
  )

  def sv3 = "3.0.0"
  val defaultScala3Options = defaultOptions.copy(
    scalaOptions = defaultOptions.scalaOptions.copy(
      scalaVersion = Some(sv3),
      scalaBinaryVersion = None
    )
  )

  def simple(checkResults: Boolean = true): Unit = {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """val n = 2
          |println(s"n=$n")
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (root, inputs, build) =>
      if (checkResults)
        build.assertGeneratedEquals(
          "simple.class",
          "simple$.class"
        )
    }
  }

  try simple(checkResults = false)
  catch {
    case _: IOException => // ignored
  }

  test("simple") {
    simple()
  }

  test("scala 3") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """val n = 2
          |println(s"n=$n")
          |""".stripMargin
    )
    testInputs.withBuild(defaultScala3Options, buildThreads, bloopConfig) { (root, inputs, build) =>
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
    val buildOptions = defaultOptions.copy(
      scalaOptions = defaultOptions.scalaOptions.copy(
        generateSemanticDbs = Some(true)
      )
    )
    testInputs.withBuild(buildOptions, buildThreads, bloopConfig) { (root, inputs, build) =>
      build.assertGeneratedEquals(
        "simple.class",
        "simple$.class",
        "META-INF/semanticdb/simple.sc.semanticdb"
      )

      val outputDir = build.outputOpt.getOrElse(sys.error("no build output???"))
      val semDb     = os.read.bytes(outputDir / "META-INF" / "semanticdb" / "simple.sc.semanticdb")
      val doc       = TextDocuments.parseFrom(semDb)
      val uris      = doc.documents.map(_.uri)
      expect(uris == Seq("simple.sc"))
    }
  }

  test("TASTy") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """val n = 2
          |println(s"n=$n")
          |""".stripMargin
    )
    val buildOptions = defaultScala3Options.copy(
      scalaOptions = defaultScala3Options.scalaOptions.copy(
        generateSemanticDbs = Some(true)
      )
    )
    testInputs.withBuild(buildOptions, buildThreads, bloopConfig) { (root, inputs, build) =>
      build.assertGeneratedEquals(
        "simple.class",
        "simple$.class",
        "simple.tasty",
        "META-INF/semanticdb/simple.sc.semanticdb"
      )

      val outputDir = build.outputOpt.getOrElse(sys.error("no build output???"))
      val tastyData = TastyData.read(os.read.bytes(outputDir / "simple.tasty"))
      val names     = tastyData.names.simpleNames
      expect(names.exists(_ == "simple.sc"))
    }
  }

  test("simple JS") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """val n = 2
          |println(s"n=$n")
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions.enableJs, buildThreads, bloopConfig) {
      (root, inputs, build) =>
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
    testInputs.withBuild(defaultOptions.enableNative, buildThreads, bloopConfig) {
      (root, inputs, build) =>
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

  test("dependencies - $ivy") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """import $ivy.`com.lihaoyi::geny:0.6.5`
          |import geny.Generator
          |val g = Generator("Hel", "lo")
          |println(g.mkString)
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (root, inputs, build) =>
      build.assertGeneratedEquals(
        "simple.class",
        "simple$.class"
      )
    }
  }
  test("dependencies - $dep") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """import $dep.`com.lihaoyi::geny:0.6.5`
          |import geny.Generator
          |val g = Generator("Hel", "lo")
          |println(g.mkString)
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (root, inputs, build) =>
      build.assertGeneratedEquals(
        "simple.class",
        "simple$.class"
      )
    }
  }
  test("dependencies - using") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """@using lib "com.lihaoyi::geny:0.6.5"
          |import geny.Generator
          |val g = Generator("Hel", "lo")
          |println(g.mkString)
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (root, inputs, build) =>
      build.assertGeneratedEquals(
        "simple.class",
        "simple$.class"
      )
    }
  }

  test("several dependencies - $ivy") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """import $ivy.`com.lihaoyi::geny:0.6.5`
          |import $ivy.`com.lihaoyi::pprint:0.6.6`
          |import geny.Generator
          |val g = Generator("Hel", "lo")
          |pprint.log(g)
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (root, inputs, build) =>
      build.assertGeneratedEquals(
        "simple.class",
        "simple$.class"
      )
    }
  }

  test("several dependencies - using") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """@using lib "com.lihaoyi::geny:0.6.5"
          |@using lib "com.lihaoyi::pprint:0.6.6"
          |import geny.Generator
          |val g = Generator("Hel", "lo")
          |pprint.log(g)
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (root, inputs, build) =>
      build.assertGeneratedEquals(
        "simple.class",
        "simple$.class"
      )
    }
  }

  test("diagnostics") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """val n = 2
          |println(s"n=$n")
          |zz
          |""".stripMargin
    )
    val buildOptions = defaultOptions.copy(
      internal = defaultOptions.internal.copy(
        keepDiagnostics = true
      )
    )
    testInputs.withBuild(buildOptions, buildThreads, bloopConfig) { (root, inputs, build) =>
      val expectedDiag = {
        val start = new bsp4j.Position(2, 0)
        val end   = new bsp4j.Position(2, 2)
        val range = new bsp4j.Range(start, end)
        val d     = new bsp4j.Diagnostic(range, "not found: value zz")
        d.setSource("bloop")
        d.setSeverity(bsp4j.DiagnosticSeverity.ERROR)
        d
      }
      val diagnostics = build.diagnostics
      val expected    = Some(Seq(Right(root / "simple.sc") -> expectedDiag))
      expect(diagnostics == expected)
    }
  }

  test("diagnostics Scala 3") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """val n = 2
          |println(s"n=$n")
          |zz
          |""".stripMargin
    )
    val buildOptions = defaultScala3Options.copy(
      internal = defaultScala3Options.internal.copy(
        keepDiagnostics = true
      )
    )
    testInputs.withBuild(buildOptions, buildThreads, bloopConfig) { (root, inputs, build) =>
      val expectedDiag = {
        val start = new bsp4j.Position(2, 0)
        val end   = new bsp4j.Position(2, 0) // would have expected (2, 2) here :|
        val range = new bsp4j.Range(start, end)
        val d     = new bsp4j.Diagnostic(range, "Not found: zz")
        d.setSource("bloop")
        d.setSeverity(bsp4j.DiagnosticSeverity.ERROR)
        d
      }
      val diagnostics = build.diagnostics
      val expected    = Some(Seq(Right(root / "simple.sc") -> expectedDiag))
      expect(diagnostics == expected)
    }
  }

}
