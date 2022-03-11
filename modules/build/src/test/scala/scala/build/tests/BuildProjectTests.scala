package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect
import coursier.cache.CacheLogger
import org.scalajs.logging.{Logger => ScalaJsLogger, NullLogger}

import java.io.PrintStream

import scala.build.Ops._
import scala.build.blooprifle.BloopRifleLogger
import scala.build.errors.{BuildException, Diagnostic, Severity}
import scala.build.options.{
  BuildOptions,
  InternalOptions,
  JavaOptions,
  ScalacOpt,
  ScalaOptions,
  Scope,
  ShadowingSeq
}
import scala.build.{Build, Inputs, LocalRepo, Logger, Position, Positioned, Sources}

class BuildProjectTests extends munit.FunSuite {

  class LoggerMock extends Logger {

    var diagnostics: List[Diagnostic] = Nil

    override def message(message: => String): Unit = ???

    override def log(s: => String): Unit = ???

    override def log(s: => String, debug: => String): Unit = ???

    override def debug(s: => String): Unit = {}

    override def log(diagnostics: Seq[Diagnostic]): Unit = {
      this.diagnostics = this.diagnostics ++ diagnostics
    }

    override def log(ex: BuildException): Unit = {}

    override def exit(ex: BuildException): Nothing = ???

    override def coursierLogger(message: String): CacheLogger = CacheLogger.nop

    override def bloopRifleLogger: BloopRifleLogger = BloopRifleLogger.nop
    override def scalaJsLogger: ScalaJsLogger       = NullLogger

    override def scalaNativeTestLogger: scala.scalanative.build.Logger =
      scala.scalanative.build.Logger.nullLogger

    override def scalaNativeCliInternalLoggerOptions: List[String] =
      List()

    override def compilerOutputStream: PrintStream = ???

  }

  val bloopJavaPath = Position.Bloop("/home/empty/jvm/8/")

  def testJvmReleaseIsSetCorrectly(
    javaHome: String,
    bloopJvmVersion: Int,
    scalacOptions: Seq[String] = Nil
  ) = {
    val options = BuildOptions(
      internal = InternalOptions(localRepository =
        LocalRepo.localRepo(scala.build.Directories.default().localRepoDir)
      ),
      javaOptions = JavaOptions(
        javaHomeOpt = Some(Positioned.none(os.Path(javaHome)))
      ),
      scalaOptions = ScalaOptions(
        scalacOptions = ShadowingSeq.from(
          scalacOptions.map(ScalacOpt(_)).map(Positioned.commandLine(_))
        )
      )
    )

    val inputs  = Inputs(Nil, None, os.pwd, "project", false, None)
    val sources = Sources(Nil, Nil, None, Nil, options)
    val logger  = new LoggerMock()
    val res = Build.buildProject(
      inputs,
      sources,
      Nil,
      options,
      Some(Positioned(bloopJavaPath, bloopJvmVersion)),
      Scope.Test,
      logger
    )

    val scalaCompilerOptions = res.fold(throw _, identity).scalaCompiler.scalacOptions
    (scalaCompilerOptions, res.fold(throw _, identity).javacOptions, logger.diagnostics)
  }

  def jvm(v: Int) = os.proc(TestUtil.cs, "java-home", "--jvm", s"zulu:$v").call().out.text().trim()

  test("Compiler options contain target JVM release") {
    val javaHome        = jvm(8)
    val bloopJvmVersion = 11
    val (scalacOptions, javacOptions, diagnostics) =
      testJvmReleaseIsSetCorrectly(javaHome, bloopJvmVersion)
    expect(scalacOptions.containsSlice(Seq("-release", "8")))
    expect(javacOptions.containsSlice(Seq("--release", "8")))
    expect(diagnostics.isEmpty)

  }

  test("Empty BuildOptions is actually empty 2 ") {
    val javaHome        = jvm(8)
    val bloopJvmVersion = 8
    val (scalacOptions, javacOptions, diagnostics) =
      testJvmReleaseIsSetCorrectly(javaHome, bloopJvmVersion)
    expect(!scalacOptions.containsSlice(Seq("-release")))
    expect(!javacOptions.containsSlice(Seq("--release")))
    expect(diagnostics.isEmpty)
  }

  test("Empty BuildOptions is actually empty 2 ") {
    val javaHome        = jvm(11)
    val bloopJvmVersion = 17
    val (scalacOptions, javacOptions, diagnostics) =
      testJvmReleaseIsSetCorrectly(javaHome, bloopJvmVersion)
    expect(scalacOptions.containsSlice(Seq("-release", "11")))
    expect(javacOptions.containsSlice(Seq("--release", "11")))
    expect(diagnostics.isEmpty)
  }

  lazy val expectedDiagnostic = Diagnostic(
    Diagnostic.Messages.bloopTooOld,
    Severity.Warning,
    List(bloopJavaPath)
  )

  test("Compiler options contain target JVM release") {
    val javaHome        = jvm(17)
    val bloopJvmVersion = 11
    val (scalacOptions, javacOptions, diagnostics) =
      testJvmReleaseIsSetCorrectly(javaHome, bloopJvmVersion)
    expect(!scalacOptions.containsSlice(Seq("-release")))
    expect(!javacOptions.containsSlice(Seq("--release")))
    expect(diagnostics == List(expectedDiagnostic))
  }

  test("Empty BuildOptions is actually empty 2 ") {
    val javaHome        = jvm(11)
    val bloopJvmVersion = 8
    val (scalacOptions, javacOptions, diagnostics) =
      testJvmReleaseIsSetCorrectly(javaHome, bloopJvmVersion, List("-release", "17"))
    expect(scalacOptions.containsSlice(Seq("-release", "17")))
    expect(!javacOptions.containsSlice(Seq("--release")))
    expect(diagnostics == List(expectedDiagnostic))
  }

  test("workspace for bsp") {
    val workspacePath = os.pwd
    val options = BuildOptions(
      internal = InternalOptions(localRepository =
        LocalRepo.localRepo(scala.build.Directories.default().localRepoDir)
      )
    )
    val inputs  = Inputs(Nil, None, workspacePath, "project", false, None)
    val sources = Sources(Nil, Nil, None, Nil, options)
    val logger  = new LoggerMock()

    val project =
      Build.buildProject(inputs, sources, Nil, options, None, Scope.Main, logger).orThrow

    expect(project.workspace == workspacePath)
  }
}
