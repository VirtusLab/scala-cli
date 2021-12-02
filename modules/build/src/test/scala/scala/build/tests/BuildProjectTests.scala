package scala.build.tests

import scala.build.errors.Severity
import com.eed3si9n.expecty.Expecty.expect
import scala.build.options.{BuildOptions, InternalOptions, JavaOptions, Scope}
import scala.build.{Build, Inputs, LocalRepo, Positioned, Sources}
import scala.build.options.ScalaOptions
import scala.build.Logger
import scala.build.blooprifle.BloopRifleLogger
import scala.build.errors.BuildException
import scala.build.errors.Diagnostic
import java.io.PrintStream
import coursier.cache.CacheLogger
import scala.build.Position

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

    override def coursierLogger: CacheLogger = CacheLogger.nop

    override def bloopRifleLogger: BloopRifleLogger = BloopRifleLogger.nop

    override def scalaNativeLogger: scala.scalanative.build.Logger =
      scala.scalanative.build.Logger.nullLogger

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
        bloopJvmVersion =
          Some(Positioned(bloopJavaPath, bloopJvmVersion)),
        javaHomeOpt = Some(Positioned.none(os.Path(javaHome)))
      ),
      scalaOptions = ScalaOptions(scalacOptions = scalacOptions)
    )

    val inputs  = new Inputs(Nil, None, os.pwd, "project", false)
    val sources = Sources(Nil, Nil, None, Nil, options)
    val logger  = new LoggerMock()
    val res     = Build.buildProject(inputs, sources, Nil, options, Scope.Test, logger)

    val scalaCompilerOptions = res.right.get.scalaCompiler.scalacOptions
    (scalaCompilerOptions, res.right.get.javacOptions, logger.diagnostics)
  }

  def jvm(v: Int) = os.proc("cs", "java-home", "--jvm", s"zulu:$v").call().out.text().trim()

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
}
