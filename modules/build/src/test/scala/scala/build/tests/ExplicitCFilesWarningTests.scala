package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import scala.build.*
import scala.build.bsp.{Bsp, BspReloadableOptions, BspThreads}
import scala.build.compiler.SimpleScalaCompilerMaker
import scala.build.errors.Severity
import scala.build.input.{CFile, ScalaCliInvokeData}
import scala.build.internal.util.WarningMessages
import scala.build.options.*
import scala.build.tests.TestUtil.*
import scala.build.tests.util.BloopServer
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ExplicitCFilesWarningTests extends TestUtil.ScalaCliBuildSuite, BloopServer.Suite {

  private val buildThreads: BuildThreads = BuildThreads.create()
  private val extraRepoTmpDir: os.Path   = os.temp.dir(prefix = "scala-cli-tests-c-warnings-")
  private val directories: Directories   = Directories.under(extraRepoTmpDir)

  override def afterAll(): Unit =
    try TestInputs.tryRemoveAll(extraRepoTmpDir)
    finally
      try buildThreads.shutdown()
      finally super.afterAll()

  private val defaultScala3Options: BuildOptions = BuildOptions(
    scalaOptions = ScalaOptions(
      scalaVersion = Some(MaybeScalaVersion(Constants.defaultScalaVersion))
    ),
    internal = InternalOptions(
      localRepository = LocalRepo.localRepo(directories.localRepoDir, TestLogger()),
      keepDiagnostics = true
    )
  )

  private def cFilePositionWarnings(logger: RecordingLogger) =
    logger.recordedDiagnostics.filter { diag =>
      diag.severity == Severity.Warning &&
      diag.message.startsWith("C/header file will be ignored: the target platform is ")
    }

  private def cFileSummaryWarnings(logger: RecordingLogger) =
    logger.recordedDiagnostics.filter { diag =>
      diag.severity == Severity.Warning &&
      diag.message.contains("explicitly passed C/header ") &&
      diag.message.contains("will be ignored")
    }

  private def cFileWarnings(logger: RecordingLogger) =
    cFilePositionWarnings(logger) ++ cFileSummaryWarnings(logger)

  test(
    "WarningMessages formats per-file and summary warnings for singular, plural, and cross"
  ) {
    val perFileJvm = WarningMessages.explicitCFileIgnored("JVM")
    assertEquals(
      perFileJvm,
      "C/header file will be ignored: the target platform is JVM, not Scala Native."
    )

    val single = WarningMessages.explicitCFilesOutsideNative(1)
    assertEquals(
      single,
      "1 explicitly passed C/header file will be ignored. To use it, build for Scala Native: `--platform scala-native` or `//> using platform scala-native`."
    )

    val multiple = WarningMessages.explicitCFilesOutsideNative(5)
    assertEquals(
      multiple,
      "5 explicitly passed C/header files will be ignored. To use them, build for Scala Native: `--platform scala-native` or `//> using platform scala-native`."
    )

    val crossMsg = WarningMessages.explicitCFilesOutsideNative(
      2,
      nativeConfiguredInCross = true
    )
    expect(crossMsg.contains("2 explicitly passed C/header files will be ignored"))
    expect(crossMsg.contains("Scala Native is only configured as a secondary platform"))
    expect(crossMsg.contains("pass `--cross` or `--platform scala-native`"))
  }

  test("warn once when explicit .c file is passed on JVM with main and test scopes") {
    val logger = new RecordingLogger()
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """@main def main() = println("hello")
          |""".stripMargin,
      os.rel / "Main.test.scala" ->
        """class MainTests
          |""".stripMargin,
      os.rel / "bindings.c" ->
        """void hello(void) {}
          |""".stripMargin
    )
    inputs.withBuild(defaultScala3Options, buildThreads, None, logger = Some(logger)) {
      (root, _, maybeBuild) =>
        assert(maybeBuild.isRight)
        val posWarnings     = cFilePositionWarnings(logger)
        val summaryWarnings = cFileSummaryWarnings(logger)
        assertEquals(posWarnings.length, 1)
        assertEquals(summaryWarnings.length, 1)
        expect(posWarnings.head.message.contains("the target platform is JVM, not Scala Native."))
        assertEquals(
          posWarnings.head.positions,
          Seq(Position.File(Right(root / "bindings.c"), (0, 0), (0, 0)))
        )
        expect(summaryWarnings.head.message.contains(
          "1 explicitly passed C/header file will be ignored."
        ))
        assertEquals(summaryWarnings.head.positions, Nil)
    }
  }

  test("warn when explicit .h file is passed on JVM") {
    val logger = new RecordingLogger()
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """@main def main() = println("hello")
          |""".stripMargin,
      os.rel / "bindings.h" ->
        """void hello(void);
          |""".stripMargin
    )
    inputs.withBuild(defaultScala3Options, buildThreads, None, logger = Some(logger)) {
      (root, _, maybeBuild) =>
        assert(maybeBuild.isRight)
        val posWarnings     = cFilePositionWarnings(logger)
        val summaryWarnings = cFileSummaryWarnings(logger)
        assertEquals(posWarnings.length, 1)
        assertEquals(summaryWarnings.length, 1)
        assertEquals(
          posWarnings.head.positions,
          Seq(Position.File(Right(root / "bindings.h"), (0, 0), (0, 0)))
        )
    }
  }

  test("warn once for all explicit .c and .h files on Scala.js") {
    val logger = new RecordingLogger()
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """@main def main() = println("hello")
          |""".stripMargin,
      os.rel / "bindings.c" ->
        """void hello(void) {}
          |""".stripMargin,
      os.rel / "bindings.h" ->
        """void hello(void);
          |""".stripMargin
    )
    inputs.withBuild(defaultScala3Options.enableJs, buildThreads, None, logger = Some(logger)) {
      (root, _, maybeBuild) =>
        assert(maybeBuild.isRight)
        val posWarnings     = cFilePositionWarnings(logger)
        val summaryWarnings = cFileSummaryWarnings(logger)
        assertEquals(posWarnings.length, 1)
        assertEquals(summaryWarnings.length, 1)
        expect(posWarnings.head.message.contains("the target platform is JS, not Scala Native."))
        assertEquals(
          posWarnings.head.positions,
          Seq(
            Position.File(Right(root / "bindings.c"), (0, 0), (0, 0)),
            Position.File(Right(root / "bindings.h"), (0, 0), (0, 0))
          )
        )
        expect(summaryWarnings.head.message.contains(
          "2 explicitly passed C/header files will be ignored."
        ))
    }
  }

  test("do not warn when .c and .h files come from a directory input on JVM") {
    val logger = new RecordingLogger()
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """@main def main() = println("hello")
          |""".stripMargin,
      os.rel / "bindings.c" ->
        """void hello(void) {}
          |""".stripMargin,
      os.rel / "bindings.h" ->
        """void hello(void);
          |""".stripMargin
    )
    inputs.withBuild(
      defaultScala3Options,
      buildThreads,
      None,
      fromDirectory = true,
      logger = Some(logger)
    ) { (_, _, maybeBuild) =>
      assert(maybeBuild.isRight)
      assertEquals(cFileWarnings(logger).length, 0)
    }
  }

  test("do not warn when explicit .c file is excluded") {
    val logger = new RecordingLogger()
    val inputs = TestInputs(
      os.rel / "project.scala" ->
        """//> using exclude bindings.c
          |""".stripMargin,
      os.rel / "Main.scala" ->
        """@main def main() = println("hello")
          |""".stripMargin,
      os.rel / "bindings.c" ->
        """void hello(void) {}
          |""".stripMargin
    )
    inputs.withBuild(defaultScala3Options, buildThreads, None, logger = Some(logger)) {
      (_, _, maybeBuild) =>
        assert(maybeBuild.isRight)
        assertEquals(cFileWarnings(logger).length, 0)
    }
  }

  test("do not warn when explicit .c file is passed for Scala Native") {
    val logger = new RecordingLogger()
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using platform scala-native
          |@main def main() = println("hello")
          |""".stripMargin,
      os.rel / "bindings.c" ->
        """void hello(void) {}
          |""".stripMargin
    )
    inputs.withInputs { (_, loadedInputs) =>
      given ScalaCliInvokeData    = ScalaCliInvokeData.dummy
      val (crossSources, inputs0) = CrossSources.forInputs(
        loadedInputs,
        Sources.defaultPreprocessors(
          defaultScala3Options.archiveCache,
          defaultScala3Options.internal.javaClassNameVersionOpt,
          () => defaultScala3Options.javaHome().value.javaCommand
        ),
        logger,
        defaultScala3Options.suppressWarningOptions
      ).toTry.get
      val sharedOptions = crossSources.sharedOptions(defaultScala3Options)
      val scopedSources = crossSources.scopedSources(defaultScala3Options).toTry.get
      val mainSources   =
        scopedSources.sources(Scope.Main, sharedOptions, inputs0.workspace, logger).toTry.get
      assertEquals(mainSources.buildOptions.platform.value, Platform.Native)
      val testSources =
        scopedSources.sources(Scope.Test, sharedOptions, inputs0.workspace, logger).toTry.get
      Build.warnIfExplicitCFilesOutsideNative(
        inputs0,
        Seq(mainSources.buildOptions, testSources.buildOptions),
        logger
      )
      assertEquals(cFileWarnings(logger).length, 0)
    }
  }

  test("warn with cross hint when multi-platform directive includes Native as secondary platform") {
    val logger = new RecordingLogger()
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using platform jvm scala-native
          |@main def main() = println("hello")
          |""".stripMargin,
      os.rel / "bindings.c" ->
        """void hello(void) {}
          |""".stripMargin
    )
    inputs.withBuild(defaultScala3Options, buildThreads, None, logger = Some(logger)) {
      (root, _, maybeBuild) =>
        assert(maybeBuild.isRight)
        val posWarnings     = cFilePositionWarnings(logger)
        val summaryWarnings = cFileSummaryWarnings(logger)
        assertEquals(posWarnings.length, 1)
        assertEquals(summaryWarnings.length, 1)
        expect(
          summaryWarnings.head.message.contains(
            "Scala Native is only configured as a secondary platform and cross-building is disabled. To use it, pass `--cross` or `--platform scala-native`."
          )
        )
        assertEquals(
          posWarnings.head.positions,
          Seq(Position.File(Right(root / "bindings.c"), (0, 0), (0, 0)))
        )
    }
  }

  test("do not warn when suppressExplicitCFilesOutsideNativeWarning is enabled") {
    val logger  = new RecordingLogger()
    val options = defaultScala3Options.copy(
      suppressWarningOptions = SuppressWarningOptions(
        suppressExplicitCFilesOutsideNativeWarning = Some(true)
      )
    )
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """@main def main() = println("hello")
          |""".stripMargin,
      os.rel / "bindings.c" ->
        """void hello(void) {}
          |""".stripMargin
    )
    inputs.withBuild(options, buildThreads, None, logger = Some(logger)) {
      (_, _, maybeBuild) =>
        assert(maybeBuild.isRight)
        assertEquals(cFileWarnings(logger).length, 0)
    }
  }

  test("deduplicate duplicate explicit .c inputs and attach file positions") {
    val logger = new RecordingLogger()
    val inputs = TestInputs(
      os.rel / "bindings.c" -> "void hello(void) {}\n"
    )
    inputs.withInputs { (root, inputs0) =>
      val duplicateCInputs =
        inputs0.copy(elements = inputs0.elements ++ inputs0.elements.collect { case c: CFile => c })
      val warningOpt =
        Build.explicitCFilesOutsideNativeWarning(duplicateCInputs, Seq(defaultScala3Options))
      expect(warningOpt.exists(_.summaryWarning.contains("1 explicitly passed C/header file ")))
      assertEquals(
        warningOpt.map(_.positions),
        Some(Seq(Position.File(Right(root / "bindings.c"), (0, 0), (0, 0))))
      )

      expect(
        Build.explicitCFilesOutsideNativeWarning(
          inputs0,
          Seq(defaultScala3Options, defaultScala3Options.enableNative)
        ).isEmpty
      )

      Build.warnIfExplicitCFilesOutsideNative(
        inputs0,
        Seq(defaultScala3Options, defaultScala3Options.enableNative),
        logger
      )
      assertEquals(cFileWarnings(logger).length, 0)
    }
  }

  test("emit warning diagnostic with file position in BSP prepareBuild") {
    val logger = new RecordingLogger()
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """@main def main() = println("hello")
          |""".stripMargin,
      os.rel / "bindings.c" ->
        """void hello(void) {}
          |""".stripMargin
    )
    inputs.withInputs { (root, inputs0) =>
      given ScalaCliInvokeData = ScalaCliInvokeData.dummy
      BspThreads.withThreads { threads =>
        val bspReloadableOptions = BspReloadableOptions(
          buildOptions = defaultScala3Options,
          bloopRifleConfig = BloopServer.bloopConfig,
          logger = logger,
          verbosity = 0
        )
        val bsp = Bsp.create(
          argsToInputs = _ => Right(inputs0),
          bspReloadableOptionsReference =
            BspReloadableOptions.Reference(() => bspReloadableOptions),
          threads = threads,
          in = new ByteArrayInputStream(Array.emptyByteArray),
          out = new ByteArrayOutputStream(),
          actionableDiagnostics = Some(false)
        )
        try {
          val future = bsp.run(inputs0, bspReloadableOptions)
          Await.ready(future, 30.seconds)
        }
        finally bsp.shutdown()
      }
      val posWarnings = cFilePositionWarnings(logger)
      assertEquals(posWarnings.length, 1)
      expect(posWarnings.head.message.contains("the target platform is JVM, not Scala Native."))
      assertEquals(
        posWarnings.head.positions,
        Seq(Position.File(Right(root / "bindings.c"), (0, 0), (0, 0)))
      )
    }
  }

  test(
    "mixed directory and explicit .c warns only for explicit .c and not passive .h in directory"
  ) {
    val logger = new RecordingLogger()
    val inputs = TestInputs(
      files = Seq(
        os.rel / "src" / "Main.scala" ->
          """@main def main() = println("hello")
            |""".stripMargin,
        os.rel / "src" / "passive.h" ->
          """void passive(void);
            |""".stripMargin,
        os.rel / "bindings.c" ->
          """void hello(void) {}
            |""".stripMargin
      ),
      inputArgs = Seq("src", "bindings.c")
    )
    inputs.withBuild(defaultScala3Options, buildThreads, None, logger = Some(logger)) {
      (root, _, maybeBuild) =>
        assert(maybeBuild.isRight)
        val posWarnings     = cFilePositionWarnings(logger)
        val summaryWarnings = cFileSummaryWarnings(logger)
        assertEquals(posWarnings.length, 1)
        assertEquals(summaryWarnings.length, 1)
        assertEquals(
          posWarnings.head.positions,
          Seq(Position.File(Right(root / "bindings.c"), (0, 0), (0, 0)))
        )
    }
  }

  test("subdirectory explicit .c warns with file position and honors wildcard exclude") {
    val loggerWithWarning = new RecordingLogger()
    val subdirInputs      = TestInputs(
      os.rel / "Main.scala" ->
        """@main def main() = println("hello")
          |""".stripMargin,
      os.rel / "csrc" / "bindings.c" ->
        """void hello(void) {}
          |""".stripMargin
    )
    subdirInputs.withBuild(
      defaultScala3Options,
      buildThreads,
      None,
      logger = Some(loggerWithWarning)
    ) { (root, _, maybeBuild) =>
      assert(maybeBuild.isRight)
      val posWarnings     = cFilePositionWarnings(loggerWithWarning)
      val summaryWarnings = cFileSummaryWarnings(loggerWithWarning)
      assertEquals(posWarnings.length, 1)
      assertEquals(summaryWarnings.length, 1)
      assertEquals(
        posWarnings.head.positions,
        Seq(Position.File(Right(root / "csrc" / "bindings.c"), (0, 0), (0, 0)))
      )
    }

    val loggerExcluded = new RecordingLogger()
    val excludedInputs = TestInputs(
      os.rel / "project.scala" ->
        """//> using exclude csrc/*
          |""".stripMargin,
      os.rel / "Main.scala" ->
        """@main def main() = println("hello")
          |""".stripMargin,
      os.rel / "csrc" / "bindings.c" ->
        """void hello(void) {}
          |""".stripMargin
    )
    excludedInputs.withBuild(
      defaultScala3Options,
      buildThreads,
      None,
      logger = Some(loggerExcluded)
    ) { (_, _, maybeBuild) =>
      assert(maybeBuild.isRight)
      assertEquals(cFileWarnings(loggerExcluded).length, 0)
    }
  }

  test("java-only project with default keepDiagnostics=false still warns on explicit .h") {
    val logger  = new RecordingLogger()
    val options = defaultScala3Options.copy(
      scalaOptions = ScalaOptions(),
      internal = InternalOptions(localRepository = defaultScala3Options.internal.localRepository)
    )
    assert(!options.internal.keepDiagnostics)
    val inputs = TestInputs(
      os.rel / "Main.java" ->
        """public class Main {
          |  public static void main(String[] args) {}
          |}
          |""".stripMargin,
      os.rel / "bindings.h" ->
        """void hello(void);
          |""".stripMargin
    )
    inputs.withBuild(options, buildThreads, None, logger = Some(logger)) { (root, _, maybeBuild) =>
      assert(maybeBuild.isRight)
      val posWarnings     = cFilePositionWarnings(logger)
      val summaryWarnings = cFileSummaryWarnings(logger)
      assertEquals(posWarnings.length, 1)
      assertEquals(summaryWarnings.length, 1)
      assertEquals(
        posWarnings.head.positions,
        Seq(Position.File(Right(root / "bindings.h"), (0, 0), (0, 0)))
      )
    }
  }

  test("empty Scala/Java sources with only explicit .c warns on JVM and is silent on Native") {
    val inputs = TestInputs(
      os.rel / "bindings.c" ->
        """void hello(void) {}
          |""".stripMargin
    )
    inputs.withInputs { (root, inputs0) =>
      val jvmLogger = new RecordingLogger()
      val jvmBuilds = Build.build(
        inputs0,
        defaultScala3Options,
        SimpleScalaCompilerMaker("java", Nil),
        None,
        jvmLogger,
        crossBuilds = false,
        buildTests = true,
        partial = None,
        actionableDiagnostics = None
      )(using ScalaCliInvokeData.dummy)
      assert(jvmBuilds.isRight)
      val posWarnings     = cFilePositionWarnings(jvmLogger)
      val summaryWarnings = cFileSummaryWarnings(jvmLogger)
      assertEquals(posWarnings.length, 1)
      assertEquals(summaryWarnings.length, 1)
      assertEquals(
        posWarnings.head.positions,
        Seq(Position.File(Right(root / "bindings.c"), (0, 0), (0, 0)))
      )

      val stubNativeOptions = defaultScala3Options.enableNative.copy(
        scalaNativeOptions = ScalaNativeOptions(
          clang = Some("clang"),
          clangpp = Some("clangpp"),
          linkingDefaults = Some(false),
          compileDefaults = Some(false)
        )
      )
      val nativeLogger = new RecordingLogger()
      val nativeBuilds = Build.build(
        inputs0,
        stubNativeOptions,
        SimpleScalaCompilerMaker("java", Nil),
        None,
        nativeLogger,
        crossBuilds = false,
        buildTests = true,
        partial = None,
        actionableDiagnostics = None
      )(using ScalaCliInvokeData.dummy)
      assert(nativeBuilds.isRight)
      assertEquals(cFileWarnings(nativeLogger).length, 0)
    }
  }

  test("multi-version JVM crossBuilds=true warns once instead of per cross-build") {
    val logger  = new RecordingLogger()
    val options = defaultScala3Options.copy(scalaOptions = ScalaOptions())
    val inputs  = TestInputs(
      os.rel / "Main.scala" ->
        s"""//> using scala ${Constants.defaultScalaVersion} ${Constants.defaultScala213Version}
           |object Main {
           |  def main(args: Array[String]): Unit = println("hello")
           |}
           |""".stripMargin,
      os.rel / "bindings.c" ->
        """void hello(void) {}
          |""".stripMargin
    )
    inputs.withInputs { (root, inputs0) =>
      val maybeBuilds = Build.build(
        inputs0,
        options,
        SimpleScalaCompilerMaker("java", Nil),
        None,
        logger,
        crossBuilds = true,
        buildTests = true,
        partial = None,
        actionableDiagnostics = None
      )(using ScalaCliInvokeData.dummy)
      assert(maybeBuilds.isRight)
      val builds = maybeBuilds.toOption.get
      expect(builds.crossBuilds.nonEmpty)
      val posWarnings     = cFilePositionWarnings(logger)
      val summaryWarnings = cFileSummaryWarnings(logger)
      assertEquals(posWarnings.length, 1)
      assertEquals(summaryWarnings.length, 1)
      assertEquals(
        posWarnings.head.positions,
        Seq(Position.File(Right(root / "bindings.c"), (0, 0), (0, 0)))
      )
    }
  }
}
