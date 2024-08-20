package scala.build.tests

import bloop.rifle.BloopRifleLogger
import com.eed3si9n.expecty.Expecty.expect
import coursier.cache.CacheLogger
import org.scalajs.logging.{NullLogger, Logger as ScalaJsLogger}

import java.io.PrintStream
import scala.build.Ops.*
import scala.build.errors.{BuildException, Diagnostic, Severity}
import scala.build.input.Module
import scala.build.internals.FeatureType
import scala.build.options.{
  BuildOptions,
  InternalOptions,
  JavaOptions,
  ScalaOptions,
  ScalacOpt,
  Scope,
  ShadowingSeq
}
import scala.build.{Build, LocalRepo, Logger, Position, Positioned, Sources}

class BuildProjectTests extends TestUtil.ScalaCliBuildSuite {

  class LoggerMock extends Logger {

    var diagnostics: List[Diagnostic] = Nil

    override def error(message: String): Unit = ???

    override def message(message: => String): Unit = ???

    override def log(s: => String): Unit = ???

    override def log(s: => String, debug: => String): Unit = ???

    override def debug(s: => String): Unit = {}

    override def log(diagnostics: Seq[Diagnostic]): Unit = {
      this.diagnostics = this.diagnostics ++ diagnostics
    }

    override def log(ex: BuildException): Unit   = {}
    override def debug(ex: BuildException): Unit = {}

    override def exit(ex: BuildException): Nothing = ???

    override def coursierLogger(message: String): CacheLogger = CacheLogger.nop

    override def bloopRifleLogger: BloopRifleLogger = BloopRifleLogger.nop
    override def scalaJsLogger: ScalaJsLogger       = NullLogger

    override def scalaNativeTestLogger: scala.scalanative.build.Logger =
      scala.scalanative.build.Logger.nullLogger

    override def scalaNativeCliInternalLoggerOptions: List[String] =
      List()

    override def compilerOutputStream: PrintStream = ???

    override def verbosity = ???

    override def experimentalWarning(featureName: String, featureType: FeatureType): Unit = ???
    override def flushExperimentalWarnings: Unit                                          = ???
  }

  test("workspace for bsp") {
    val options = BuildOptions(
      internal = InternalOptions(localRepository =
        LocalRepo.localRepo(scala.build.Directories.default().localRepoDir, TestLogger())
      )
    )
    val inputs    = Module.empty("project")
    val sources   = Sources(Nil, Nil, None, Nil, options)
    val logger    = new LoggerMock()
    val artifacts = options.artifacts(logger, Scope.Test).orThrow

    val project =
      Build.buildProject(inputs, sources, Nil, options, None, Scope.Main, logger, artifacts).orThrow

    expect(project.workspace == inputs.workspace)
  }
}
