package scala.build.tests

import ch.epfl.scala.bsp4j
import com.eed3si9n.expecty.Expecty.expect
import com.google.gson.Gson
import coursier.cache.ArtifactError
import dependency.parser.DependencyParser

import java.io.IOException
import scala.build.Ops.*
import scala.build.errors.{
  DependencyFormatError,
  InvalidBinaryScalaVersionError,
  ScalaNativeCompatibilityError
}
import scala.build.options.{
  BuildOptions,
  InternalOptions,
  JavaOpt,
  MaybeScalaVersion,
  ScalacOpt,
  ScriptOptions,
  ShadowingSeq
}
import scala.build.tastylib.TastyData
import scala.build.tests.TestUtil.*
import scala.build.tests.util.BloopServer
import scala.build.{BuildThreads, Directories, LocalRepo, Positioned}
import scala.meta.internal.semanticdb.TextDocuments
import scala.util.Properties
import scala.jdk.CollectionConverters.*

abstract class BuildTests(server: Boolean) extends munit.FunSuite {

  private def hasDiagnostics = server

  val buildThreads = BuildThreads.create()
  def bloopConfigOpt =
    if (server) Some(BloopServer.bloopConfig)
    else None

  val extraRepoTmpDir = os.temp.dir(prefix = "scala-cli-tests-extra-repo-")
  val directories     = Directories.under(extraRepoTmpDir)

  override def afterAll(): Unit = {
    TestInputs.tryRemoveAll(extraRepoTmpDir)
    buildThreads.shutdown()
  }

  val baseOptions = BuildOptions(
    internal = InternalOptions(
      localRepository = LocalRepo.localRepo(directories.localRepoDir),
      keepDiagnostics = true
    )
  )

  def sv2 = "2.13.5"
  val defaultOptions = baseOptions.copy(
    scalaOptions = baseOptions.scalaOptions.copy(
      scalaVersion = Some(MaybeScalaVersion(sv2)),
      scalaBinaryVersion = None
    ),
    scriptOptions = ScriptOptions(Some(true))
  )

  def sv3 = "3.0.0"
  val defaultScala3Options = defaultOptions.copy(
    scalaOptions = defaultOptions.scalaOptions.copy(
      scalaVersion = Some(MaybeScalaVersion(sv3)),
      scalaBinaryVersion = None
    ),
    scriptOptions = ScriptOptions(None)
  )

  def simple(checkResults: Boolean = true): Unit = {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """val n = 2
          |println(s"n=$n")
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      if (checkResults)
        maybeBuild.orThrow.assertGeneratedEquals(
          "simple.class",
          "simple$.class",
          "simple$delayedInit$body.class"
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
    testInputs.withBuild(defaultScala3Options, buildThreads, bloopConfigOpt) {
      (_, _, maybeBuild) =>
        maybeBuild.orThrow.assertGeneratedEquals(
          "simple$_.class",
          "simple_sc.class",
          "simple_sc.tasty",
          "simple$_.tasty",
          "simple_sc$.class",
          "simple$package$.class",
          "simple$package.class",
          "simple$package.tasty"
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
    testInputs.withBuild(defaultScala3Options, buildThreads, bloopConfigOpt) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow
        build.assertGeneratedEquals(
          "other$_$A.class",
          "other$_.tasty",
          "other$_.class",
          "other_sc$.class",
          "other_sc.class",
          "other_sc.tasty",
          "other$package$.class",
          "other$package.class",
          "other$package.tasty"
        )
        maybeBuild.orThrow.assertNoDiagnostics
    }
  }

  test("semantic DB") {
    import scala.meta.internal.semanticdb.*

    val scriptContents =
      """val n = 2
        |println(s"n=$n")
        |""".stripMargin

    val expectedSymbolOccurences = Seq(
      SymbolOccurrence(
        Some(Range(0, 4, 0, 5)),
        "_empty_/simple.n.",
        SymbolOccurrence.Role.DEFINITION
      ),
      SymbolOccurrence(
        Some(Range(1, 8, 1, 9)),
        "scala/StringContext#s().",
        SymbolOccurrence.Role.REFERENCE
      ),
      SymbolOccurrence(
        Some(Range(1, 0, 1, 7)),
        "scala/Predef.println(+1).",
        SymbolOccurrence.Role.REFERENCE
      ),
      SymbolOccurrence(
        Some(Range(1, 13, 1, 14)),
        "_empty_/simple.n.",
        SymbolOccurrence.Role.REFERENCE
      )
    )

    val testInputs = TestInputs(os.rel / "simple.sc" -> scriptContents)
    val buildOptions = defaultOptions.copy(
      scalaOptions = defaultOptions.scalaOptions.copy(
        generateSemanticDbs = Some(true)
      )
    )
    testInputs.withBuild(buildOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      val build = maybeBuild.orThrow
      build.assertGeneratedEquals(
        "simple$delayedInit$body.class",
        "simple$.class",
        "simple.class",
        "META-INF/semanticdb/simple.sc.semanticdb"
      )
      maybeBuild.orThrow.assertNoDiagnostics

      val outputDir = build.outputOpt.getOrElse(sys.error("no build output???"))
      val semDb     = os.read.bytes(outputDir / "META-INF" / "semanticdb" / "simple.sc.semanticdb")
      val doc       = TextDocuments.parseFrom(semDb)
      val uris      = doc.documents.map(_.uri)
      expect(uris == Seq("simple.sc"))

      val occurences = doc.documents.flatMap(_.occurrences)
      expect(occurences.forall(_.range.isDefined))

      val sortedOccurences = doc.documents.flatMap(_.occurrences)
        .sortBy(s =>
          s.range.map(r => (r.startLine, r.startCharacter)).getOrElse((Int.MaxValue, Int.MaxValue))
        )
      val sortedExpectedOccurences = expectedSymbolOccurences
        .sortBy(s =>
          s.range.map(r => (r.startLine, r.startCharacter)).getOrElse((Int.MaxValue, Int.MaxValue))
        )

      munit.Assertions.assert(
        sortedOccurences == sortedExpectedOccurences,
        clue = doc.documents.flatMap(_.occurrences)
      )
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
    testInputs.withBuild(buildOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      val build = maybeBuild.orThrow
      build.assertGeneratedEquals(
        "simple$_.class",
        "simple_sc.class",
        "simple_sc.tasty",
        "simple$_.tasty",
        "simple_sc$.class",
        "simple$package$.class",
        "simple$package.class",
        "simple$package.tasty",
        "META-INF/semanticdb/simple.sc.semanticdb"
      )
      maybeBuild.orThrow.assertNoDiagnostics
      val outputDir = build.outputOpt.getOrElse(sys.error("no build output???"))
      val tastyData = TastyData.read(os.read.bytes(outputDir / "simple$_.tasty")).orThrow
      val names     = tastyData.names.simpleNames
      expect(names.contains("simple.sc"))
    }
  }

  test("simple JS") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """val n = 2
          |println(s"n=$n")
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions.enableJs, buildThreads, bloopConfigOpt) {
      (_, _, maybeBuild) =>
        maybeBuild.orThrow.assertGeneratedEquals(
          "simple$.class",
          "simple$.sjsir",
          "simple$delayedInit$body.class",
          "simple$delayedInit$body.sjsir",
          "simple.class",
          "simple.sjsir"
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
    testInputs.withBuild(defaultOptions.enableNative, buildThreads, bloopConfigOpt) {
      (_, _, maybeBuild) =>
        maybeBuild.orThrow.assertGeneratedEquals(
          "simple$.class",
          "simple$.nir",
          "simple$delayedInit$body.class",
          "simple$delayedInit$body.nir",
          "simple.class",
          "simple.nir"
        )
        maybeBuild.orThrow.assertNoDiagnostics
    }
  }
  if (!Properties.isWin)
    test("simple native") {
      simpleNativeTest()
    }

  test("dependencies - using") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """//> using dep com.lihaoyi::geny:0.6.5
          |import geny.Generator
          |val g = Generator("Hel", "lo")
          |println(g.mkString)
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      maybeBuild.orThrow.assertGeneratedEquals(
        "simple.class",
        "simple$.class",
        "simple$delayedInit$body.class"
      )
      maybeBuild.orThrow.assertNoDiagnostics
    }
  }

  test("several dependencies - using") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """//> using dep com.lihaoyi::geny:0.6.5
          |//> using dep com.lihaoyi::pprint:0.6.6
          |import geny.Generator
          |val g = Generator("Hel", "lo")
          |pprint.log(g)
          |""".stripMargin,
      os.rel / "simple2.sc" ->
        """//> using dep com.lihaoyi::geny:0.6.5, "com.lihaoyi::pprint:0.6.6"
          |import geny.Generator
          |val g = Generator("Hel", "lo")
          |pprint.log(g)
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      maybeBuild.orThrow.assertGeneratedEquals(
        "simple$.class",
        "simple$delayedInit$body.class",
        "simple.class",
        "simple2$.class",
        "simple2$delayedInit$body.class",
        "simple2.class"
      )
      maybeBuild.orThrow.assertNoDiagnostics
    }
  }

  if (hasDiagnostics)
    test("diagnostics") {
      diagnosticsTest()
    }
  def diagnosticsTest(): Unit = {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """val n = 2
          |println(s"n=$n")
          |zz
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfigOpt) { (root, _, maybeBuild) =>
      val expectedDiag = {
        val start = new bsp4j.Position(2, 0)
        val end   = new bsp4j.Position(2, 2)
        val range = new bsp4j.Range(start, end)
        val d     = new bsp4j.Diagnostic(range, "not found: value zz")
        d.setSource("bloop")
        d.setSeverity(bsp4j.DiagnosticSeverity.ERROR)
        val bScalaDiagnostic = new bsp4j.ScalaDiagnostic
        bScalaDiagnostic.setActions(List().asJava)
        d.setData(new Gson().toJsonTree(bScalaDiagnostic))
        d
      }
      val diagnostics = maybeBuild.orThrow.diagnostics
      val expected    = Some(Seq(Right(root / "simple.sc") -> expectedDiag))
      expect(diagnostics == expected)
    }
  }

  if (hasDiagnostics)
    test("diagnostics Scala 3") {
      scala3DiagnosticsTest()
    }
  def scala3DiagnosticsTest(): Unit = {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """val n = 2
          |println(s"n=$n")
          |zz
          |""".stripMargin
    )
    testInputs.withBuild(defaultScala3Options, buildThreads, bloopConfigOpt) {
      (root, _, maybeBuild) =>
        val expectedDiag = {
          val start = new bsp4j.Position(2, 0)
          val end   = new bsp4j.Position(2, 0) // would have expected (2, 2) here :|
          val range = new bsp4j.Range(start, end)
          val d     = new bsp4j.Diagnostic(range, "Not found: zz")
          d.setSource("bloop")
          d.setSeverity(bsp4j.DiagnosticSeverity.ERROR)
          val bScalaDiagnostic = new bsp4j.ScalaDiagnostic
          bScalaDiagnostic.setActions(List().asJava)
          d.setData(new Gson().toJsonTree(bScalaDiagnostic))
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
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
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
        """//> using target.platform scala.js
          |object Ignored {
          |  def foo = 2
          |}
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
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
        """//> using target.platform jvm
          |object Ignored {
          |  def foo = 2
          |}
          |""".stripMargin,
      os.rel / "IgnoredToo.scala" ->
        """//> using target.platform native
          |object IgnoredToo {
          |  def foo = 2
          |}
          |""".stripMargin
    )
    val options = defaultOptions.enableJs
    testInputs.withBuild(options, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      maybeBuild.orThrow.assertGeneratedEquals(
        "Simple.class",
        "Simple$.class",
        "Simple.sjsir",
        "Simple$.sjsir"
      )
    }
  }

  test("Pass files with only commented directives as is to scalac") {
    val testInputs = TestInputs(
      os.rel / "Simple.scala" ->
        """//> using dep com.lihaoyi::pprint:0.6.6
          |object Simple {
          |  def main(args: Array[String]): Unit =
          |    pprint.log("Hello " + "from tests")
          |}
          |""".stripMargin
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      val sources = maybeBuild.toOption.get.successfulOpt.get.sources
      expect(sources.inMemory.isEmpty)
      expect(sources.paths.lengthCompare(1) == 0)
    }
  }

  test("Compiler plugins from using directives") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using scala 2.13
          |//> using plugins com.olegpy::better-monadic-for:0.3.1
          |
          |def getCounts: Either[String, (Int, Int)] = ???
          |
          |for {
          |  (x, y) <- getCounts
          |} yield x + y
          |""".stripMargin
    )
    inputs.withBuild(defaultOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      assert(clue(maybeBuild.orThrow.diagnostics).toSeq.flatten.isEmpty)
    }
  }

  test("Scala Native working with Scala 3.1") {
    val testInputs = TestInputs(
      os.rel / "Simple.scala" ->
        """//> using platform scala-native
          |//> using nativeVersion 0.4.3-RC2
          |//> using scala 3.1.0
          |def foo(): String = "foo"
          |""".stripMargin
    )
    val buildOptions = defaultOptions.copy(
      scalaOptions = defaultOptions.scalaOptions.copy(
        scalaVersion = None
      )
    )
    testInputs.withBuild(buildOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      assert(maybeBuild.isRight)
    }
  }

  test("Scala Native not working with Scala 3.0") {
    val testInputs = TestInputs(
      os.rel / "Simple.scala" ->
        """//> using platform scala-native
          |//> using nativeVersion 0.4.3-RC2
          |//> using scala 3.0.2
          |def foo(): String = "foo"
          |""".stripMargin
    )
    val buildOptions = defaultOptions.copy(
      scalaOptions = defaultOptions.scalaOptions.copy(
        scalaVersion = None
      )
    )
    testInputs.withBuild(buildOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      assert(maybeBuild.isLeft)
      assert(
        maybeBuild.swap.toOption.exists {
          case _: ScalaNativeCompatibilityError => true
          case _                                => false
        }
      )
    }
  }

  test(s"Scala 3.${Int.MaxValue}.3 makes the build fail with InvalidBinaryScalaVersionError") {
    val testInputs = TestInputs(
      os.rel / "Simple.scala" ->
        s""" // using scala "3.${Int.MaxValue}.3"
           |object Hello {
           |  def main(args: Array[String]): Unit =
           |    println("Hello")
           |}
           |
           |""".stripMargin
    )
    val buildOptions = baseOptions.copy(
      scalaOptions = baseOptions.scalaOptions.copy(
        scalaVersion = Some(MaybeScalaVersion(s"3.${Int.MaxValue}.3")),
        scalaBinaryVersion = None
      )
    )
    testInputs.withBuild(buildOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      assert(
        maybeBuild.swap.exists { case _: InvalidBinaryScalaVersionError => true; case _ => false },
        s"specifying Scala 3.${Int.MaxValue}.3 as version does not lead to InvalidBinaryScalaVersionError"
      )
    }
  }

  test("cli dependency options shadowing using directives") {
    val usingDependency = "org.scalameta::munit::1.0.0-M1"
    val cliDependency   = "org.scalameta::munit::0.7.29"

    val inputs = TestInputs(
      os.rel / "foo.scala" ->
        s"""//> using dep $usingDependency
           |def foo = "bar"
           |""".stripMargin
    )

    val parsedCliDependency = DependencyParser.parse(cliDependency).getOrElse(
      throw new DependencyFormatError(cliDependency, "", Nil)
    )

    // Emulates options derived from cli
    val buildOptions = defaultOptions.copy(
      classPathOptions = defaultOptions.classPathOptions.copy(
        extraDependencies = ShadowingSeq.from(Seq(Positioned.none(parsedCliDependency)))
      )
    )

    inputs.withBuild(buildOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      assert(maybeBuild.isRight)
      val build     = maybeBuild.toOption.get
      val artifacts = build.options.classPathOptions.extraDependencies.toSeq
      assert(artifacts.exists(_.value.toString() == cliDependency))
    }
  }

  test("cli scalac options shadowing using directives") {
    val cliScalacOptions            = Seq("-Xmaxwarns", "4", "-g:source")
    val usingDirectiveScalacOptions = Seq("-nobootcp", "-Xmaxwarns", "5", "-g:none")

    val expectedOptions = Seq("-Xmaxwarns", "4", "-g:source", "-nobootcp")

    val inputs = TestInputs(
      os.rel / "foo.scala" ->
        s"""//> using options ${usingDirectiveScalacOptions.mkString(" ")}
           |def foo = "bar"
           |""".stripMargin
    )

    // Emulates options derived from cli
    val buildOptions = defaultOptions.copy(
      scalaOptions = defaultOptions.scalaOptions.copy(
        scalacOptions = ShadowingSeq.from(
          cliScalacOptions.map(ScalacOpt(_)).map(Positioned.commandLine(_))
        )
      )
    )

    inputs.withBuild(buildOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      assert(maybeBuild.isRight)
      val build         = maybeBuild.toOption.get
      val scalacOptions = build.options.scalaOptions.scalacOptions.toSeq.map(_.value.value)
      expect(scalacOptions == expectedOptions)
    }
  }

  test("cli java options shadowing using directives") {
    val cliJavaOptions            = Seq("-proc:only", "-JflagB", "-Xmx2G")
    val usingDirectiveJavaOptions = Seq("-proc:none", "-parameters", "-JflagA", "-Xmx4G")

    val expectedJavaOptions =
      Seq("-proc:only", "-JflagB", "-Xmx2G", "-parameters", "-JflagA")

    val inputs = TestInputs(
      os.rel / "foo.scala" ->
        s"""//> using javaOpt ${usingDirectiveJavaOptions.mkString(" ")}
           |def foo = "bar"
           |""".stripMargin
    )

    // Emulates options derived from cli
    val buildOptions = defaultOptions.copy(
      javaOptions = defaultOptions.javaOptions.copy(
        javaOpts = ShadowingSeq.from(
          cliJavaOptions.map(JavaOpt(_)).map(Positioned.commandLine(_))
        )
      )
    )

    inputs.withBuild(buildOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      val build       = maybeBuild.orThrow
      val javaOptions = build.options.javaOptions.javaOpts.toSeq.map(_.value.value)
      assert(javaOptions == expectedJavaOptions)
    }
  }

  test("repeated Java options") {
    val inputs = TestInputs(
      os.rel / "foo.sc" ->
        """//> using javaOpt --add-opens, "foo/bar"
          |//> using javaOpt --add-opens, "other/thing"
          |//> using javaOpt --add-exports, "foo/bar"
          |//> using javaOpt --add-exports, "other/thing"
          |//> using javaOpt --add-modules, "foo/bar"
          |//> using javaOpt --add-modules, other/thing
          |//> using javaOpt --add-reads, "foo/bar"
          |//> using javaOpt --add-reads, "other/thing"
          |//> using javaOpt "--patch-module", "foo/bar"
          |//> using javaOpt "--patch-module", "other/thing"
          |
          |def foo = "bar"
          |""".stripMargin
    )

    inputs.withBuild(defaultOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      val expectedOptions =
        // format: off
        Seq(
          "--add-opens", "foo/bar",
          "--add-opens", "other/thing",
          "--add-exports", "foo/bar",
          "--add-exports", "other/thing",
          "--add-modules", "foo/bar",
          "--add-modules", "other/thing",
          "--add-reads", "foo/bar",
          "--add-reads", "other/thing",
          "--patch-module", "foo/bar",
          "--patch-module", "other/thing"
        )
        // format: on
      val javaOptions =
        maybeBuild.toOption.get.options.javaOptions.javaOpts.toSeq.map(_.value.value)
      expect(javaOptions == expectedOptions)
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

    inputs.withBuild(defaultOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      val expectedOptions = Seq("-source:future")
      val scalacOptions =
        maybeBuild.orThrow.options.scalaOptions.scalacOptions.toSeq.map(_.value.value)
      expect(scalacOptions == expectedOptions)
    }

  }

  // Issue #525
  test("scalac options not spuriously duplicating") {
    val inputs = TestInputs(
      os.rel / "foo.scala" ->
        """//> using scala 2.13
          |//> using options -deprecation, "-feature", "-Xmaxwarns", "1"
          |//> using option -Xdisable-assertions
          |
          |def foo = "bar"
          |""".stripMargin
    )

    inputs.withBuild(defaultOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      val expectedOptions =
        Seq("-deprecation", "-feature", "-Xmaxwarns", "1", "-Xdisable-assertions")
      val scalacOptions =
        maybeBuild.toOption.get.options.scalaOptions.scalacOptions.toSeq.map(_.value.value)
      expect(scalacOptions == expectedOptions)
    }
  }

  test("multiple times scalac options with -Xplugin prefix") {
    val inputs = TestInputs(
      os.rel / "foo.scala" ->
        """//> using option -Xplugin:/paradise_2.12.15-2.1.1.jar
          |//> using option -Xplugin:/semanticdb-scalac_2.12.15-4.4.31.jar
          |
          |def foo = "bar"
          |""".stripMargin
    )

    inputs.withBuild(defaultOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      val expectedOptions =
        Seq(
          "-Xplugin:/paradise_2.12.15-2.1.1.jar",
          "-Xplugin:/semanticdb-scalac_2.12.15-4.4.31.jar"
        )
      val scalacOptions =
        maybeBuild.toOption.get.options.scalaOptions.scalacOptions.toSeq.map(_.value.value)
      expect(scalacOptions == expectedOptions)
    }
  }

  test("Pin Scala 2 artifacts version") {
    val inputs = TestInputs(
      os.rel / "Foo.scala" ->
        """//> using dep com.lihaoyi:ammonite_2.13.8:2.5.1-6-5fce97fb
          |//> using scala 2.13.5
          |
          |object Foo {
          |  def main(args: Array[String]): Unit = {
          |    println(scala.util.Properties.versionNumberString)
          |  }
          |}
          |""".stripMargin
    )
    inputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      expect(maybeBuild.exists(_.success))
      val build = maybeBuild.toOption.flatMap(_.successfulOpt).getOrElse(sys.error("cannot happen"))
      val cp    = build.artifacts.classPath.map(_.last)

      val scalaLibraryJarNameOpt =
        cp.find(n => n.startsWith("scala-library-") && n.endsWith(".jar"))
      val scalaCompilerJarNameOpt =
        cp.find(n => n.startsWith("scala-compiler-") && n.endsWith(".jar"))
      val scalaReflectJarNameOpt =
        cp.find(n => n.startsWith("scala-reflect-") && n.endsWith(".jar"))
      expect(scalaLibraryJarNameOpt.contains("scala-library-2.13.5.jar"))
      expect(scalaCompilerJarNameOpt.contains("scala-compiler-2.13.5.jar"))
      expect(scalaReflectJarNameOpt.contains("scala-reflect-2.13.5.jar"))
    }
  }

  test("Pin Scala 3 artifacts version") {
    val inputs = TestInputs(
      os.rel / "Foo.scala" ->
        """//> using dep com.lihaoyi:ammonite_3.1.1:2.5.1-6-5fce97fb
          |//> using scala 3.1.0
          |
          |object Foo {
          |  def main(args: Array[String]): Unit = {
          |    println(scala.util.Properties.versionNumberString)
          |  }
          |}
          |""".stripMargin
    )
    inputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) { (_, _, maybeBuild) =>
      expect(maybeBuild.exists(_.success))
      val build = maybeBuild.toOption.flatMap(_.successfulOpt).getOrElse(sys.error("cannot happen"))
      val cp    = build.artifacts.classPath.map(_.last)

      val scalaLibraryJarNameOpt =
        cp.find(n => n.startsWith("scala3-library_3-") && n.endsWith(".jar"))
      val scalaCompilerJarNameOpt =
        cp.find(n => n.startsWith("scala3-compiler_3-") && n.endsWith(".jar"))
      expect(scalaLibraryJarNameOpt.contains("scala3-library_3-3.1.0.jar"))
      expect(scalaCompilerJarNameOpt.contains("scala3-compiler_3-3.1.0.jar"))
    }
  }

  test("Pure Java") {
    val inputs = TestInputs(
      os.rel / "Foo.java" ->
        """package foo;
          |
          |public class Foo {
          |  public static void main(String[] args) {
          |    System.out.println("Hello");
          |  }
          |}
          |""".stripMargin
    )
    inputs.withBuild(baseOptions, buildThreads, bloopConfigOpt, buildTests = false) {
      (_, _, maybeBuild) =>
        expect(maybeBuild.exists(_.success))
        val build = maybeBuild
          .toOption
          .flatMap(_.successfulOpt)
          .getOrElse(sys.error("cannot happen"))
        val cp = build.fullClassPath
        expect(cp.length == 1) // no scala-library, only the class directory
    }
  }

  test("No stubs JAR at runtime") {
    val inputs = TestInputs(
      os.rel / "Foo.scala" ->
        """package foo
          |
          |object Foo {
          |  def main(args: Array[String]): Unit =
          |    println("Hello")
          |}
          |""".stripMargin
    )

    inputs.withBuild(baseOptions, buildThreads, bloopConfigOpt, buildTests = false) {
      (_, _, maybeBuild) =>
        expect(maybeBuild.exists(_.success))
        val build = maybeBuild
          .toOption
          .flatMap(_.successfulOpt)
          .getOrElse(sys.error("cannot happen"))
        val cp = build.fullClassPath
        val coreCp = cp.filter { f =>
          val name = f.last
          !name.startsWith("scala-library") &&
          !name.startsWith("scala3-library") &&
          !name.startsWith("runner")
        }
        expect(coreCp.length == 1) // only classes directory, no stubs jar
    }
  }

  test("declared sources in using directive should be included to count project hash") {
    val helloFile = "Hello.scala"
    val inputs =
      TestInputs(
        os.rel / helloFile ->
          """|//> using file Utils.scala
             |
             |object Hello extends App {
             |   println(Utils.hello)
             |}""".stripMargin,
        os.rel / "Utils.scala" ->
          s"""|object Utils {
              |  val hello = "Hello"
              |}""".stripMargin,
        os.rel / "Helper.scala" ->
          s"""|object Helper {
              |  val hello = "Hello"
              |}""".stripMargin
      )
    inputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) { (root, _, maybeBuild) =>
      expect(maybeBuild.exists(_.success))
      val build = maybeBuild.toOption.flatMap(_.successfulOpt).getOrElse(sys.error("cannot happen"))

      // updating sources in using directive should change project name
      val updatedHelloScala =
        """|//> using file Helper.scala
           |
           |object Hello extends App {
           |   println(Helper.hello)
           |}""".stripMargin
      os.write.over(root / helloFile, updatedHelloScala)

      inputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) { (_, _, maybeUpdatedBuild) =>
        expect(maybeUpdatedBuild.exists(_.success))
        val updatedBuild =
          maybeUpdatedBuild.toOption.flatMap(_.successfulOpt).getOrElse(sys.error("cannot happen"))
        // project name should be change after updating source in using directive
        expect(build.inputs.projectName != updatedBuild.inputs.projectName)
      }
    }
  }
}
