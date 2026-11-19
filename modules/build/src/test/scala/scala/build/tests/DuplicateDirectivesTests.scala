package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect
import coursier.cache.Cache.Fetch
import coursier.cache.{ArchiveCache, ArtifactError, Cache}
import coursier.util.{Artifact, EitherT, Task}

import java.io.File

import scala.build.Ops.EitherThrowOps
import scala.build.errors.{Diagnostic, Severity}
import scala.build.input.ScalaCliInvokeData
import scala.build.options.{BuildOptions, InternalOptions, JmhOptions, SuppressWarningOptions}
import scala.build.preprocessing.directives.StrictDirective
import scala.build.preprocessing.{
  DirectivesPreprocessor,
  DuplicateDirectives,
  Preprocessor,
  ScalaPreprocessor,
  ScopePath
}
import scala.build.{BuildThreads, CrossSources, Directories, LocalRepo, Position, Sources}
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext

class DuplicateDirectivesTests extends TestUtil.ScalaCliBuildSuite {

  private val preprocessors: Seq[Preprocessor] = Sources.defaultPreprocessors(
    ArchiveCache().copy(cache =
      new Cache[Task] {
        def fetch: Fetch[Task] = _ => sys.error("shouldn't be used")
        def file(artifact: Artifact): EitherT[Task, ArtifactError, File] =
          sys.error("shouldn't be used")
        def ec: ExecutionContext = sys.error("shouldn't be used")
      }
    ),
    javaClassNameVersionOpt = None,
    javaCommand = () => sys.error("shouldn't be used")
  )

  private class DiagnosticCapturingLogger extends TestLogger() {
    val diagnostics: ListBuffer[Diagnostic]        = ListBuffer.empty
    override def log(diags: Seq[Diagnostic]): Unit =
      diagnostics ++= diags
  }

  private given ScalaCliInvokeData = ScalaCliInvokeData.dummy

  private def preprocessScala(
    content: String,
    suppressWarningOptions: SuppressWarningOptions = SuppressWarningOptions()
  ): Seq[Diagnostic] = {
    val logger = new DiagnosticCapturingLogger()
    val path   = Left("Main.scala")
    ScalaPreprocessor.process(
      content = content,
      path = path,
      scopeRoot = ScopePath.fromPath(os.pwd),
      logger = logger,
      maybeRecoverOnError = e => Some(e),
      allowRestrictedFeatures = true,
      suppressWarningOptions = suppressWarningOptions
    ).orThrow
    logger.diagnostics.toSeq
  }

  private def preprocessMarkdown(mdContent: String): Seq[Diagnostic] = {
    val logger = new DiagnosticCapturingLogger()
    TestInputs(os.rel / "README.md" -> mdContent).withInputs { (_, inputs) =>
      CrossSources.forInputs(
        inputs,
        preprocessors,
        logger,
        SuppressWarningOptions()
      ).orThrow
    }
    logger.diagnostics.toSeq
  }

  private def filePositions(diag: Diagnostic): Seq[Position.File] =
    diag.positions.collect { case p: Position.File => p }

  test("warns once on exact duplicate with same key, pointing at the second occurrence") {
    val content =
      """//> using dep com.lihaoyi::os-lib:0.9.1
        |//> using dep com.lihaoyi::os-lib:0.9.1
        |object Main
        |""".stripMargin
    val diags = preprocessScala(content)
    expect(diags.length == 1)
    val diag = diags.head
    expect(diag.severity == Severity.Warning)
    expect(diag.message ==
      "Duplicated using directive: //> using dep com.lihaoyi::os-lib:0.9.1. It can be removed.")
    val lines = filePositions(diag).map(_.startPos._1)
    expect(lines == Seq(1))
  }

  test("triple duplicate emits 2 warnings in ascending source line order") {
    val content =
      """//> using dep com.lihaoyi::os-lib:0.9.1
        |//> using dep com.lihaoyi::os-lib:0.9.1
        |//> using dep com.lihaoyi::os-lib:0.9.1
        |object Main
        |""".stripMargin
    val diags = preprocessScala(content)
    expect(diags.length == 2)
    expect(diags.forall(_.severity == Severity.Warning))
    val lines = diags.flatMap(filePositions).map(_.startPos._1)
    expect(lines == Seq(1, 2))
  }

  test("warns on alias duplicates (dep + deps)") {
    val content =
      """//> using dep com.lihaoyi::os-lib:0.9.1
        |//> using deps com.lihaoyi::os-lib:0.9.1
        |object Main
        |""".stripMargin
    val diags = preprocessScala(content)
    expect(diags.length == 1)
    expect(diags.head.severity == Severity.Warning)
    val lines = filePositions(diags.head).map(_.startPos._1)
    expect(lines == Seq(1))
  }

  test("warns on alias duplicates (option + scalac-option)") {
    val content =
      """//> using option -deprecation
        |//> using scalac-option -deprecation
        |object Main
        |""".stripMargin
    val diags = preprocessScala(content)
    expect(diags.length == 1)
    expect(diags.head.severity == Severity.Warning)
    val lines = filePositions(diags.head).map(_.startPos._1)
    expect(lines == Seq(1))
  }

  test("warns on value-less boolean directive duplicate (buildInfo)") {
    val content =
      """//> using buildInfo
        |//> using buildInfo
        |object Main
        |""".stripMargin
    val diags = preprocessScala(content)
    expect(diags.length == 1)
    expect(diags.head.severity == Severity.Warning)
    val lines = filePositions(diags.head).map(_.startPos._1)
    expect(lines == Seq(1))
  }

  test("no warning for same key with different values") {
    val depContent =
      """//> using dep com.lihaoyi::os-lib:0.9.1
        |//> using dep com.lihaoyi::pprint:0.9.0
        |object Main
        |""".stripMargin
    expect(preprocessScala(depContent).isEmpty)

    val scalaContent =
      """//> using scala 3.3.1
        |//> using scala 3.4.0
        |object Main
        |""".stripMargin
    expect(preprocessScala(scalaContent).isEmpty)
  }

  test("no warning across different scopes (dep vs test.dep)") {
    val content =
      """//> using dep com.lihaoyi::os-lib:0.9.1
        |//> using test.dep com.lihaoyi::os-lib:0.9.1
        |object Main
        |""".stripMargin
    expect(preprocessScala(content).isEmpty)
  }

  test("no warning for repeated target.* requirement directives") {
    val content =
      """//> using target.platform jvm
        |//> using target.platform jvm
        |object Main
        |""".stripMargin
    expect(preprocessScala(content).isEmpty)
  }

  test("no duplicate warning for repeated unknown directives") {
    val directives = Seq(
      StrictDirective("unknownDirective", Seq.empty, startColumn = 10, startLine = 0),
      StrictDirective("unknownDirective", Seq.empty, startColumn = 10, startLine = 1)
    )
    val logger = new DiagnosticCapturingLogger()
    DuplicateDirectives.issueWarnings(
      Left("Main.scala"),
      directives,
      SuppressWarningOptions(),
      logger
    )
    expect(logger.diagnostics.isEmpty)
  }

  test("suppressed when suppressDuplicatedDirectivesWarning or suppressAll is set") {
    val content =
      """//> using dep com.lihaoyi::os-lib:0.9.1
        |//> using dep com.lihaoyi::os-lib:0.9.1
        |object Main
        |""".stripMargin
    expect(
      preprocessScala(
        content,
        SuppressWarningOptions(suppressDuplicatedDirectivesWarning = Some(true))
      ).isEmpty
    )
    expect(preprocessScala(content, SuppressWarningOptions.suppressAll).isEmpty)
  }

  test("shebang script preserves original line numbers in duplicate warning position") {
    val content =
      """#!/usr/bin/env -S scala-cli shebang
        |//> using dep com.lihaoyi::os-lib:0.9.1
        |//> using dep com.lihaoyi::os-lib:0.9.1
        |println("hello")
        |""".stripMargin
    val diags = preprocessScala(content)
    expect(diags.length == 1)
    val lines = filePositions(diags.head).map(_.startPos._1)
    expect(lines == Seq(2))
  }

  test("Java file with duplicated using directive emits 1 warning at the second occurrence") {
    val content =
      """//> using dep com.lihaoyi::os-lib:0.9.1
        |//> using dep com.lihaoyi::os-lib:0.9.1
        |public class Main {}
        |""".stripMargin
    val logger = new DiagnosticCapturingLogger()
    DirectivesPreprocessor(
      path = Left("Main.java"),
      cwd = ScopePath.fromPath(os.pwd),
      logger = logger,
      allowRestrictedFeatures = true,
      suppressWarningOptions = SuppressWarningOptions(),
      maybeRecoverOnError = e => Some(e)
    ).preprocess(content).orThrow
    expect(logger.diagnostics.length == 1)
    val diag = logger.diagnostics.head
    expect(diag.severity == Severity.Warning)
    val lines = filePositions(diag).map(_.startPos._1)
    expect(lines == Seq(1))
  }

  test("multi-value directive warning formats values with single spaces") {
    val content =
      """//> using options -deprecation -feature
        |//> using options -deprecation -feature
        |object Main
        |""".stripMargin
    val diags = preprocessScala(content)
    expect(diags.length == 1)
    expect(diags.head.message ==
      "Duplicated using directive: //> using options -deprecation -feature. It can be removed.")
  }

  test("Markdown Scala snippets report duplicate directive warnings at exact .md line numbers") {
    val mdContent =
      """# Title
        |
        |Some prose here.
        |
        |```scala
        |//> using option -deprecation
        |//> using option -deprecation
        |println("block 1")
        |```
        |
        |More prose here.
        |
        |```scala
        |//> using option -deprecation
        |println("block 2")
        |```
        |""".stripMargin
    val dupWarnings =
      preprocessMarkdown(mdContent).filter(_.message.startsWith("Duplicated using directive:"))
    expect(dupWarnings.length == 2)
    val lines = dupWarnings.flatMap(filePositions).map(_.startPos._1)
    expect(lines == Seq(6, 13))
  }

  test("Markdown Java snippet reports duplicate directive warning at exact .md line number") {
    val mdContent =
      """# Title
        |
        |Some prose here.
        |
        |```java
        |//> using javacOpt -Xlint:all
        |//> using javacOpt -Xlint:all
        |public class Foo {}
        |```
        |""".stripMargin
    val dupWarnings =
      preprocessMarkdown(mdContent).filter(_.message.startsWith("Duplicated using directive:"))
    expect(dupWarnings.length == 1)
    val lines = filePositions(dupWarnings.head).map(_.startPos._1)
    expect(lines == Seq(6))
  }

  test("JMH build emits duplicated directive warning only once") {
    val inputs = TestInputs(
      os.rel / "Bench.scala" ->
        """//> using option -deprecation
          |//> using option -deprecation
          |package bench
          |import org.openjdk.jmh.annotations.*
          |class Bench {
          |  @Benchmark def noop(): Int = 1 + 1
          |}
          |""".stripMargin
    )
    val extraRepoTmpDir = os.temp.dir(prefix = "scala-cli-tests-dup-dir-")
    val buildThreads    = BuildThreads.create()
    try {
      val directories = Directories.under(extraRepoTmpDir)
      val baseOptions = BuildOptions(
        internal = InternalOptions(
          localRepository = LocalRepo.localRepo(directories.localRepoDir, TestLogger()),
          keepDiagnostics = true
        )
      )
      val logger  = new DiagnosticCapturingLogger()
      val options = baseOptions.copy(
        useBuildServer = Some(false),
        jmhOptions = JmhOptions(
          enableJmh = Some(true),
          runJmh = Some(true)
        )
      )
      inputs.withBuild(options, buildThreads, bloopConfigOpt = None, logger = Some(logger)) {
        (_, _, maybeBuild) =>
          val build = maybeBuild.orThrow
          expect(build.success)
      }
      val dupWarnings =
        logger.diagnostics.filter(_.message.startsWith("Duplicated using directive:"))
      expect(dupWarnings.length == 1)
    }
    finally {
      TestInputs.tryRemoveAll(extraRepoTmpDir)
      buildThreads.shutdown()
    }
  }

  test("quoted values with whitespace preserve quotes in duplicate directive warning") {
    val content =
      """//> using javaOpt "-Dfoo=a b"
        |//> using javaOpt "-Dfoo=a b"
        |//> using javaOpt -Dfoo=a b
        |object Main
        |""".stripMargin
    val diags = preprocessScala(content)
    expect(diags.length == 1)
    expect(diags.head.message ==
      "Duplicated using directive: //> using javaOpt \"-Dfoo=a b\". It can be removed.")
    val lines = filePositions(diags.head).map(_.startPos._1)
    expect(lines == Seq(1))
  }

  test("values with embedded quotes are escaped cleanly in duplicate directive warning") {
    val content =
      """//> using javaOpt "-Dmsg=\"hi there\""
        |//> using javaOpt "-Dmsg=\"hi there\""
        |object Main
        |""".stripMargin
    val diags = preprocessScala(content)
    expect(diags.length == 1)
    expect(diags.head.message ==
      "Duplicated using directive: //> using javaOpt \"-Dmsg=\\\"hi there\\\"\". It can be removed.")
    val lines = filePositions(diags.head).map(_.startPos._1)
    expect(lines == Seq(1))
  }
}
