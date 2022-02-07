package scala.build.tests

import ch.epfl.scala.bsp4j
import com.eed3si9n.expecty.Expecty.expect

import java.io.IOException

import scala.build.Ops._
import scala.build.options.{
  BuildOptions,
  InternalOptions,
  JavaOpt,
  ScalacOpt,
  ScalaOptions,
  ShadowingSeq
}
import scala.build.tastylib.TastyData
import scala.build.tests.TestUtil._
import scala.build.tests.util.BloopServer
import scala.build.{BuildThreads, Directories, LocalRepo}
import scala.meta.internal.semanticdb.TextDocuments
import scala.util.Properties
import scala.build.preprocessing.directives.SingleValueExpected
import scala.build.errors.ScalaNativeCompatibilityError
import dependency.parser.DependencyParser
import scala.build.Positioned
import scala.build.errors.DependencyFormatError

class BuildTests extends munit.FunSuite {

  val buildThreads = BuildThreads.create()
  def bloopConfig  = BloopServer.bloopConfig

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
      localRepository = LocalRepo.localRepo(directories.localRepoDir),
      keepDiagnostics = true
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
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      if (checkResults)
        maybeBuild.orThrow.assertGeneratedEquals(
          "simple.class",
          "simple_sc.class",
          "simple$.class",
          "simple_sc$.class"
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
    testInputs.withBuild(defaultScala3Options, buildThreads, bloopConfig) {
      (_, _, maybeBuild) =>
        maybeBuild.orThrow.assertGeneratedEquals(
          "simple.class",
          "simple_sc.class",
          "simple_sc.tasty",
          "simple$.class",
          "simple.tasty",
          "simple_sc$.class"
        )
        maybeBuild.orThrow.assertNoDiagnostics
    }
  }

  // Test if we do not print any warnings
  test("scala 3 class in .sc file") {
    val testInputs = TestInputs(
      os.rel / "other.sc" ->
        "class A"
    )
    testInputs.withBuild(defaultScala3Options, buildThreads, bloopConfig) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow
        build.assertGeneratedEquals(
          "other$A.class",
          "other$.class",
          "other.tasty",
          "other.class",
          "other_sc$.class",
          "other_sc.class",
          "other_sc.tasty"
        )
        maybeBuild.orThrow.assertNoDiagnostics
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
    testInputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      val build = maybeBuild.orThrow
      println(build.options.scalaOptions.scalacOptions.values)
      build.assertGeneratedEquals(
        "simple.class",
        "simple_sc.class",
        "simple$.class",
        "simple_sc$.class",
        "META-INF/semanticdb/simple.sc.semanticdb"
      )
      maybeBuild.orThrow.assertNoDiagnostics

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
    testInputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      val build = maybeBuild.orThrow
      build.assertGeneratedEquals(
        "simple.class",
        "simple_sc.class",
        "simple_sc.tasty",
        "simple$.class",
        "simple.tasty",
        "simple_sc$.class",
        "META-INF/semanticdb/simple.sc.semanticdb"
      )
      maybeBuild.orThrow.assertNoDiagnostics
      val outputDir = build.outputOpt.getOrElse(sys.error("no build output???"))
      val tastyData = TastyData.read(os.read.bytes(outputDir / "simple.tasty")).orThrow
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
      (_, _, maybeBuild) =>
        maybeBuild.orThrow.assertGeneratedEquals(
          "simple.class",
          "simple_sc.class",
          "simple.sjsir",
          "simple$.sjsir",
          "simple_sc.sjsir",
          "simple$.class",
          "simple_sc$.class",
          "simple_sc$.sjsir"
        )
        maybeBuild.orThrow.assertNoDiagnostics
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
      (_, _, maybeBuild) =>
        maybeBuild.orThrow.assertGeneratedEquals(
          "simple$.class",
          "simple$.nir",
          "simple.class",
          "simple.nir",
          "simple_sc$$$Lambda$1.nir",
          "simple_sc$.class",
          "simple_sc$.nir",
          "simple_sc.class",
          "simple_sc.nir"
        )
        maybeBuild.orThrow.assertNoDiagnostics
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
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      maybeBuild.orThrow.assertGeneratedEquals(
        "simple.class",
        "simple_sc.class",
        "simple$.class",
        "simple_sc$.class"
      )
      maybeBuild.orThrow.assertNoDiagnostics
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
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      maybeBuild.orThrow.assertGeneratedEquals(
        "simple.class",
        "simple_sc.class",
        "simple$.class",
        "simple_sc$.class"
      )
      maybeBuild.orThrow.assertNoDiagnostics
    }
  }
  test("dependencies - using") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """//> using lib "com.lihaoyi::geny:0.6.5"
          |import geny.Generator
          |val g = Generator("Hel", "lo")
          |println(g.mkString)
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      maybeBuild.orThrow.assertGeneratedEquals(
        "simple.class",
        "simple_sc.class",
        "simple$.class",
        "simple_sc$.class"
      )
      maybeBuild.orThrow.assertNoDiagnostics
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
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      maybeBuild.orThrow.assertGeneratedEquals(
        "simple.class",
        "simple_sc.class",
        "simple$.class",
        "simple_sc$.class"
      )
      maybeBuild.orThrow.assertNoDiagnostics
    }
  }

  test("several dependencies - using") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """//> using lib "com.lihaoyi::geny:0.6.5"
          |//> using lib "com.lihaoyi::pprint:0.6.6"
          |import geny.Generator
          |val g = Generator("Hel", "lo")
          |pprint.log(g)
          |""".stripMargin,
      os.rel / "simple2.sc" ->
        """//> using
          |//  lib "com.lihaoyi::geny:0.6.5"
          |//  lib "com.lihaoyi::pprint:0.6.6"
          |import geny.Generator
          |val g = Generator("Hel", "lo")
          |pprint.log(g)
          |""".stripMargin,
      os.rel / "simple3.sc" ->
        """//> using lib "com.lihaoyi::geny:0.6.5", "com.lihaoyi::pprint:0.6.6"
          |import geny.Generator
          |val g = Generator("Hel", "lo")
          |pprint.log(g)
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      maybeBuild.orThrow.assertGeneratedEquals(
        "simple.class",
        "simple_sc.class",
        "simple$.class",
        "simple_sc$.class",
        "simple2.class",
        "simple2_sc.class",
        "simple2$.class",
        "simple2_sc$.class",
        "simple3.class",
        "simple3_sc.class",
        "simple3$.class",
        "simple3_sc$.class"
      )
      maybeBuild.orThrow.assertNoDiagnostics
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
    testInputs.withBuild(buildOptions, buildThreads, bloopConfig) { (root, _, maybeBuild) =>
      val expectedDiag = {
        val start = new bsp4j.Position(2, 0)
        val end   = new bsp4j.Position(2, 2)
        val range = new bsp4j.Range(start, end)
        val d     = new bsp4j.Diagnostic(range, "not found: value zz")
        d.setSource("bloop")
        d.setSeverity(bsp4j.DiagnosticSeverity.ERROR)
        d
      }
      val diagnostics = maybeBuild.orThrow.diagnostics
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
    testInputs.withBuild(buildOptions, buildThreads, bloopConfig) { (root, _, maybeBuild) =>
      val expectedDiag = {
        val start = new bsp4j.Position(2, 0)
        val end   = new bsp4j.Position(2, 0) // would have expected (2, 2) here :|
        val range = new bsp4j.Range(start, end)
        val d     = new bsp4j.Diagnostic(range, "Not found: zz")
        d.setSource("bloop")
        d.setSeverity(bsp4j.DiagnosticSeverity.ERROR)
        d
      }
      val diagnostics = maybeBuild.orThrow.diagnostics
      val expected    = Some(Seq(Right(root / "simple.sc") -> expectedDiag))
      expect(diagnostics == expected)
    }
  }

  test("ignore files if wrong Scala version requirement") {
    val testInputs = TestInputs(
      os.rel / "Simple.scala" ->
        """object Simple {
          |  def main(args: Array[String]): Unit =
          |    println("Hello")
          |}
          |""".stripMargin,
      os.rel / "Ignored.scala" ->
        """//> using target.scala.== "2.12"
          |object Ignored {
          |  def foo = 2
          |}
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      maybeBuild.orThrow.assertGeneratedEquals(
        "Simple.class",
        "Simple$.class"
      )
    }
  }
  test("ignore files if wrong Scala target requirement") {
    val testInputs = TestInputs(
      os.rel / "Simple.scala" ->
        """object Simple {
          |  def main(args: Array[String]): Unit =
          |    println("Hello")
          |}
          |""".stripMargin,
      os.rel / "Ignored.scala" ->
        """//> using target.platform "scala.js"
          |object Ignored {
          |  def foo = 2
          |}
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      maybeBuild.orThrow.assertGeneratedEquals(
        "Simple.class",
        "Simple$.class"
      )
    }
  }

  test("ignore files if wrong Scala target requirement - JS") {
    val testInputs = TestInputs(
      os.rel / "Simple.scala" ->
        """object Simple {
          |  def main(args: Array[String]): Unit =
          |    println("Hello")
          |}
          |""".stripMargin,
      os.rel / "Ignored.scala" ->
        """//> using target.platform "jvm"
          |object Ignored {
          |  def foo = 2
          |}
          |""".stripMargin,
      os.rel / "IgnoredToo.scala" ->
        """//> using target.platform "native"
          |object IgnoredToo {
          |  def foo = 2
          |}
          |""".stripMargin
    )
    val options = defaultOptions.enableJs
    testInputs.withBuild(options, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      maybeBuild.orThrow.assertGeneratedEquals(
        "Simple.class",
        "Simple$.class",
        "Simple.sjsir",
        "Simple$.sjsir"
      )
    }
  }

  test("ignore files if wrong Scala version requirement via in clause") {
    val testInputs = TestInputs(
      os.rel / "Simple.scala" ->
        """//> using target.scala.== "2.12" in "my-scala-2.12/"
          |object Simple {
          |  def main(args: Array[String]): Unit =
          |    println("Hello")
          |}
          |""".stripMargin,
      os.rel / "my-scala-2.12" / "Ignored.scala" ->
        """object Ignored {
          |  def foo = 2
          |}
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      maybeBuild.orThrow.assertGeneratedEquals(
        "Simple.class",
        "Simple$.class"
      )
    }
  }
  test("ignore files if wrong Scala target requirement via in clause") {
    val testInputs = TestInputs(
      os.rel / "Simple.scala" ->
        """//> using target.platform "scala.js" in "js-sources/"
          |object Simple {
          |  def main(args: Array[String]): Unit =
          |    println("Hello")
          |}
          |""".stripMargin,
      os.rel / "js-sources" / "Ignored.scala" ->
        """object Ignored {
          |  def foo = 2
          |}
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      maybeBuild.orThrow.assertGeneratedEquals(
        "Simple.class",
        "Simple$.class"
      )
    }
  }

  test("Pass files with only commented directives as is to scalac") {
    val testInputs = TestInputs(
      os.rel / "Simple.scala" ->
        """//> using lib "com.lihaoyi::pprint:0.6.6"
          |object Simple {
          |  def main(args: Array[String]): Unit =
          |    pprint.log("Hello " + "from tests")
          |}
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      val sources = maybeBuild.toOption.get.successfulOpt.get.sources
      expect(sources.inMemory.isEmpty)
      expect(sources.paths.lengthCompare(1) == 0)
    }
  }

  test("Ignore malformed import $ivy") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """#!/usr/bin/env scala-cli
          |import $ivy"com.lihaoyi::os-lib:0.7.8"
          |""".stripMargin
    )
    val buildOptions = defaultOptions.copy(
      internal = defaultOptions.internal.copy(
        keepDiagnostics = true
      )
    )
    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      val diagnostics = maybeBuild.orThrow.diagnostics.getOrElse(Nil).map(_._2)
      expect(
        diagnostics.exists(_.getMessage.contains("identifier expected but string literal found"))
      )
    }
  }

  test("Compiler plugins from using directives") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using scala "2.13"
          |//> using plugins "com.olegpy::better-monadic-for:0.3.1"
          |
          |def getCounts: Either[String, (Int, Int)] = ???
          |
          |for {
          |  (x, y) <- getCounts
          |} yield x + y
          |""".stripMargin,
      os.rel / "p2.sc" ->
        """//> using
          |//  scala "2.13"
          |//  plugins "com.olegpy::better-monadic-for:0.3.1"
          |
          |def getCounts: Either[String, (Int, Int)] = ???
          |
          |for {
          |  (x, y) <- getCounts
          |} yield x + y
          |""".stripMargin
    )
    val buildOptions = defaultOptions.copy(
      internal = defaultOptions.internal.copy(
        keepDiagnostics = true
      )
    )
    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(clue(maybeBuild.orThrow.diagnostics).toSeq.flatten.isEmpty)
    }
  }
  test("ScalaNativeOptions for native-gc with no values") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """  using `native-gc`
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    val buildOptions = defaultOptions.copy(
      internal = defaultOptions.internal.copy(
        keepDiagnostics = true
      )
    )
    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(maybeBuild.isLeft)
      assert(maybeBuild.left.get == SingleValueExpected("native-gc", Seq()))
    }
  }

  test("ScalaNativeOptions for native-gc with multiple values") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-gc` 78, 12
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    val buildOptions = defaultOptions.copy(
      internal = defaultOptions.internal.copy(
        keepDiagnostics = true
      )
    )
    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(maybeBuild.isLeft)
      assert(maybeBuild.left.get == SingleValueExpected("native-gc", Seq("78", "12")))
    }

  }

  test("ScalaNativeOptions for native-gc") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-gc` 78
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    val buildOptions: BuildOptions = defaultOptions.copy(
      internal = defaultOptions.internal.copy(
        keepDiagnostics = true
      )
    )

    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(maybeBuild.toOption.get.options.scalaNativeOptions.gcStr.get == "78")
    }
  }

  test("ScalaNativeOptions for native-version with multiple values") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-version` "0.4.0", "0.3.3"
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    val buildOptions = defaultOptions.copy(
      internal = defaultOptions.internal.copy(
        keepDiagnostics = true
      )
    )
    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(maybeBuild.isLeft)
      assert(maybeBuild.left.get == SingleValueExpected("native-version", Seq("0.4.0", "0.3.3")))
    }

  }

  test("ScalaNativeOptions for native-version") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-version` "0.4.0"
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    val buildOptions: BuildOptions = defaultOptions.copy(
      internal = defaultOptions.internal.copy(
        keepDiagnostics = true
      )
    )

    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(maybeBuild.toOption.get.options.scalaNativeOptions.version.get == "0.4.0")
    }
  }

  test("ScalaNativeOptions for native-compile") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-compile` "compileOption1", "compileOption2"
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    val buildOptions: BuildOptions = defaultOptions.copy(
      internal = defaultOptions.internal.copy(
        keepDiagnostics = true
      )
    )

    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(
        maybeBuild.toOption.get.options.scalaNativeOptions.compileOptions(0) == "compileOption1"
      )
      assert(
        maybeBuild.toOption.get.options.scalaNativeOptions.compileOptions(1) == "compileOption2"
      )
    }
  }

  test("ScalaNativeOptions for native-linking and no value") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-linking`
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    val buildOptions: BuildOptions = defaultOptions.copy(
      internal = defaultOptions.internal.copy(
        keepDiagnostics = true
      )
    )

    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(maybeBuild.toOption.get.options.scalaNativeOptions.linkingOptions.isEmpty)
    }
  }

  test("ScalaNativeOptions for native-linking") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-linking` "linkingOption1", "linkingOption2"
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    val buildOptions: BuildOptions = defaultOptions.copy(
      internal = defaultOptions.internal.copy(
        keepDiagnostics = true
      )
    )

    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(
        maybeBuild.toOption.get.options.scalaNativeOptions.linkingOptions(0) == "linkingOption1"
      )
      assert(
        maybeBuild.toOption.get.options.scalaNativeOptions.linkingOptions(1) == "linkingOption2"
      )
    }
  }

  test("ScalaNativeOptions for native-linking and no value") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-linking`
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    val buildOptions: BuildOptions = defaultOptions.copy(
      internal = defaultOptions.internal.copy(
        keepDiagnostics = true
      )
    )

    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(maybeBuild.toOption.get.options.scalaNativeOptions.linkingOptions.isEmpty)
    }
  }

  test("Scala Native working with Scala 3.1") {
    val testInputs = TestInputs(
      os.rel / "Simple.scala" ->
        """//> using platform "scala-native"
          |//> using nativeVersion "0.4.3-RC2"
          |//> using scala "3.1.0"
          |def foo(): String = "foo"
          |""".stripMargin
    )
    val buildOptions = defaultOptions.copy(
      scalaOptions = defaultOptions.scalaOptions.copy(
        scalaVersion = None
      )
    )
    testInputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(maybeBuild.isRight)
    }
  }

  test("Scala Native not working with Scala 3.0") {
    val testInputs = TestInputs(
      os.rel / "Simple.scala" ->
        """//> using platform "scala-native"
          |//> using nativeVersion "0.4.3-RC2"
          |//> using scala "3.0.2"
          |def foo(): String = "foo"
          |""".stripMargin
    )
    val buildOptions = defaultOptions.copy(
      scalaOptions = defaultOptions.scalaOptions.copy(
        scalaVersion = None
      )
    )
    testInputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(maybeBuild.isLeft)
      assert(maybeBuild.left.get.isInstanceOf[ScalaNativeCompatibilityError])
    }
  }

  test("cli dependency options shadowing using directives") {
    val usingDependency = "org.scalameta::munit::1.0.0-M1"
    val cliDependency   = "org.scalameta::munit::0.7.29"

    val inputs = TestInputs(
      os.rel / "foo.scala" ->
        s"""//> using lib "$usingDependency"
           |def foo = "bar"
           |""".stripMargin
    )

    val parsedCliDependency = DependencyParser.parse(cliDependency).getOrElse(
      throw new DependencyFormatError(cliDependency, "")
    )

    // Emulates options derived from cli
    val buildOptions: BuildOptions = defaultOptions.copy(
      internal = defaultOptions.internal.copy(
        keepDiagnostics = true
      ),
      classPathOptions = defaultOptions.classPathOptions.copy(
        extraDependencies = ShadowingSeq(Seq(Positioned.none(parsedCliDependency)))
      )
    )

    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(maybeBuild.isRight)
      val build     = maybeBuild.right.get
      val artifacts = build.options.classPathOptions.extraDependencies.values.toSeq
      assert(artifacts.exists(_.value.toString() == cliDependency))
    }
  }

  test("cli scalac options shadowing using directives") {
    val cliScalacOptions            = Seq("-Xmaxwarns", "4", "-g:source")
    val usingDirectiveScalacOptions = Seq("-nobootcp", "-Xmaxwarns", "5", "-g:none")

    val expectedOptions = Seq("-Xmaxwarns", "4", "-g:source", "-nobootcp")

    val inputs = TestInputs(
      os.rel / "foo.scala" ->
        s"""//> using options "${usingDirectiveScalacOptions.mkString("\", \"")}"
           |def foo = "bar"
           |""".stripMargin
    )

    // Emulates options derived from cli
    val buildOptions: BuildOptions = defaultOptions.copy(
      internal = defaultOptions.internal.copy(
        keepDiagnostics = true
      ),
      scalaOptions = defaultOptions.scalaOptions.copy(
        scalacOptions = ShadowingSeq(
          ScalacOpt.fromPositionedStringSeq(cliScalacOptions.map(Positioned.commandLine(_)))
        )
      )
    )

    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(maybeBuild.isRight)
      val build         = maybeBuild.right.get
      val scalacOptions = ScalacOpt.toStringSeq(build.options.scalaOptions.scalacOptions.values)
      assert(scalacOptions == expectedOptions)
    }
  }

  test("cli java options shadowing using directives") {
    val cliJavaOptions            = Seq("-proc:only", "-JflagB", "-Xmx2G")
    val usingDirectiveJavaOptions = Seq("-proc:none", "-parameters", "-JflagA", "-Xmx4G")

    val expectedJavaOptions =
      Seq("-proc:only", "-JflagB", "-Xmx2G", "-parameters", "-JflagA")

    val inputs = TestInputs(
      os.rel / "foo.scala" ->
        s"""//> using javaOpt "${usingDirectiveJavaOptions.mkString("\", \"")}"
           |def foo = "bar"
           |""".stripMargin
    )

    // Emulates options derived from cli
    val buildOptions: BuildOptions = defaultOptions.copy(
      internal = defaultOptions.internal.copy(
        keepDiagnostics = true
      ),
      javaOptions = defaultOptions.javaOptions.copy(
        javaOpts = ShadowingSeq(
          JavaOpt.fromPositionedStringSeq(cliJavaOptions.map(Positioned.commandLine(_)))
        )
      )
    )

    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      val build       = maybeBuild.orThrow
      val javaOptions = JavaOpt.toStringSeq(build.options.javaOptions.javaOpts.values)
      assert(javaOptions == expectedJavaOptions)
    }
  }

  // Issue #607
  test("-source:future not internally duplicating") {
    val inputs = TestInputs(
      os.rel / "foo.scala" ->
        """//> using option "-source:future"
          |def foo = "bar"
          |""".stripMargin
    )

    val buildOptions: BuildOptions = defaultOptions.copy(
      internal = defaultOptions.internal.copy(
        keepDiagnostics = true
      )
    )

    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      val expectedOptions = Seq("-source:future")
      val scalacOptions =
        ScalacOpt.toStringSeq(maybeBuild.orThrow.options.scalaOptions.scalacOptions.values)
      expect(scalacOptions == expectedOptions)
    }

  }

  // Issue #525
  test("scalac options not spuriously duplicating") {
    val inputs = TestInputs(
      os.rel / "foo.scala" ->
        """//> using scala "2.13"
          |//> using options "-deprecation", "-feature", "-Xmaxwarns", "1"
          |//> using option "-Xdisable-assertions"
          |
          |def foo = "bar"
          |""".stripMargin
    )

    val buildOptions: BuildOptions = defaultOptions.copy(
      internal = defaultOptions.internal.copy(
        keepDiagnostics = true
      )
    )

    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      val expectedOptions =
        Seq("-deprecation", "-feature", "-Xmaxwarns", "1", "-Xdisable-assertions")
      val scalacOptions = ScalacOpt.toStringSeq(
        maybeBuild.toOption.get.options.scalaOptions.scalacOptions.values
      )
      expect(scalacOptions == expectedOptions)
    }
  }
}
