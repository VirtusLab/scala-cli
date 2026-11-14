package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.build.compiler.SimpleScalaCompilerMaker
import scala.build.errors.Severity
import scala.build.input.{CFile, ScalaCliInvokeData}
import scala.build.internal.util.WarningMessages
import scala.build.options.*
import scala.build.tests.TestUtil.*
import scala.build.{Build, BuildThreads, CrossSources, Directories, LocalRepo, Sources}

class ExplicitCFilesWarningTests extends TestUtil.ScalaCliBuildSuite {

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

  private def cFileWarnings(logger: RecordingLogger) =
    logger.recordedDiagnostics.filter { diag =>
      diag.severity == Severity.Warning &&
      diag.message.contains("will be ignored because the target platform is not Scala Native")
    }

  test(
    "WarningMessages.explicitCFilesOutsideNative handles singular, plural, truncation, and empty"
  ) {
    val single = WarningMessages.explicitCFilesOutsideNative(Seq("./bindings.c"))
    expect(single.contains("C/header file (./bindings.c)"))

    val multiple = WarningMessages.explicitCFilesOutsideNative(
      Seq("./bindings.c", "./bindings.h", "./bindings.c")
    )
    expect(multiple.contains("C/header files (./bindings.c, ./bindings.h)"))

    val many = WarningMessages.explicitCFilesOutsideNative(
      (1 to 7).map(i => s"./file$i.c")
    )
    expect(many.contains(
      "C/header files (./file1.c, ./file2.c, ./file3.c, ./file4.c, ./file5.c, and 2 more)"
    ))
    expect(!many.contains("./file6.c"))

    val empty = WarningMessages.explicitCFilesOutsideNative(Nil)
    expect(empty.contains("Explicitly passed C/header files will be ignored"))
    expect(!empty.contains("()"))
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
      (_, _, maybeBuild) =>
        assert(maybeBuild.isRight)
        val warnings = cFileWarnings(logger)
        assertEquals(warnings.length, 1)
        expect(warnings.head.message.contains("bindings.c"))
        expect(warnings.head.message.contains("C/header file "))
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
      (_, _, maybeBuild) =>
        assert(maybeBuild.isRight)
        val warnings = cFileWarnings(logger)
        assertEquals(warnings.length, 1)
        expect(warnings.head.message.contains("bindings.h"))
    }
  }

  test("warn once listing all explicit .c and .h files on Scala.js") {
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
      (_, _, maybeBuild) =>
        assert(maybeBuild.isRight)
        val warnings = cFileWarnings(logger)
        assertEquals(warnings.length, 1)
        expect(warnings.head.message.contains("bindings.c"))
        expect(warnings.head.message.contains("bindings.h"))
        expect(warnings.head.message.contains("C/header files "))
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
      val activeOptions = Build.activeScopeOptions(
        mainSources = mainSources,
        mainOptions = mainSources.buildOptions,
        testSources = testSources,
        testOptions = testSources.buildOptions,
        fallbackOptions = sharedOptions
      )
      Build.warnIfExplicitCFilesOutsideNative(inputs0, activeOptions, logger)
      assertEquals(cFileWarnings(logger).length, 0)
    }
  }

  test("warn when multi-platform directive includes Native without --cross enabled") {
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
      (_, _, maybeBuild) =>
        assert(maybeBuild.isRight)
        val warnings = cFileWarnings(logger)
        assertEquals(warnings.length, 1)
        expect(warnings.head.message.contains("bindings.c"))
    }
  }

  test("deduplicate duplicate explicit .c inputs and respect active scopes and cross builds") {
    val logger = new RecordingLogger()
    val inputs = TestInputs(
      os.rel / "bindings.c" -> "void hello(void) {}\n"
    )
    inputs.withInputs { (_, inputs0) =>
      val duplicateCInputs =
        inputs0.copy(elements = inputs0.elements ++ inputs0.elements.collect { case c: CFile => c })
      val warningOpt =
        Build.explicitCFilesOutsideNativeWarning(duplicateCInputs, Seq(defaultScala3Options))
      expect(warningOpt.exists(_.contains("C/header file (")))

      expect(
        Build.explicitCFilesOutsideNativeWarning(
          inputs0,
          Seq(defaultScala3Options, defaultScala3Options.enableNative)
        ).isEmpty
      )

      val emptyMainSources = Sources(Nil, Nil, None, Nil, defaultScala3Options)
      val testSources      = Sources(
        Seq((inputs0.workspace / "Main.test.scala", os.rel / "Main.test.scala")),
        Nil,
        None,
        Nil,
        defaultScala3Options.enableNative
      )
      val activeOptions = Build.activeScopeOptions(
        mainSources = emptyMainSources,
        mainOptions = defaultScala3Options,
        testSources = testSources,
        testOptions = defaultScala3Options.enableNative,
        fallbackOptions = defaultScala3Options
      )
      Build.warnIfExplicitCFilesOutsideNative(inputs0, activeOptions, logger)
      assertEquals(cFileWarnings(logger).length, 0)

      val fallbackOnlyOptions = Build.activeScopeOptions(
        mainSources = emptyMainSources,
        mainOptions = defaultScala3Options,
        testSources = testSources,
        testOptions = defaultScala3Options.enableNative,
        fallbackOptions = defaultScala3Options.enableNative,
        buildTests = false
      )
      expect(Build.explicitCFilesOutsideNativeWarning(inputs0, fallbackOnlyOptions).isEmpty)
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
      (_, _, maybeBuild) =>
        assert(maybeBuild.isRight)
        val warnings = cFileWarnings(logger)
        assertEquals(warnings.length, 1)
        expect(warnings.head.message.contains("bindings.c"))
        expect(!warnings.head.message.contains("passive.h"))
    }
  }

  test("subdirectory explicit .c warns with relative path and honors wildcard exclude") {
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
    ) { (_, _, maybeBuild) =>
      assert(maybeBuild.isRight)
      val warnings = cFileWarnings(loggerWithWarning)
      assertEquals(warnings.length, 1)
      val expectedRel = (os.rel / "csrc" / "bindings.c").toString
      expect(warnings.head.message.contains(expectedRel))
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
    inputs.withBuild(options, buildThreads, None, logger = Some(logger)) { (_, _, maybeBuild) =>
      assert(maybeBuild.isRight)
      val warnings = cFileWarnings(logger)
      assertEquals(warnings.length, 1)
      expect(warnings.head.message.contains("bindings.h"))
    }
  }

  test("empty Scala/Java sources with only explicit .c warns on JVM and is silent on Native") {
    val inputs = TestInputs(
      os.rel / "bindings.c" ->
        """void hello(void) {}
          |""".stripMargin
    )
    inputs.withInputs { (_, inputs0) =>
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
      val jvmWarnings = cFileWarnings(jvmLogger)
      assertEquals(jvmWarnings.length, 1)
      expect(jvmWarnings.head.message.contains("bindings.c"))

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
    inputs.withInputs { (_, inputs0) =>
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
      val warnings = cFileWarnings(logger)
      assertEquals(warnings.length, 1)
      expect(warnings.head.message.contains("bindings.c"))
    }
  }
}
