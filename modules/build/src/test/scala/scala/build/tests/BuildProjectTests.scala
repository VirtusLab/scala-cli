package scala.build.tests

import bloop.rifle.BloopRifleLogger
import com.eed3si9n.expecty.Expecty.expect
import coursier.cache.CacheLogger
import org.scalajs.logging.{Logger as ScalaJsLogger, NullLogger}

import java.io.PrintStream

import scala.build.Ops.*
import scala.build.errors.{BuildException, Diagnostic}
import scala.build.input.Inputs
import scala.build.internals.FeatureType
import scala.build.options.{BuildOptions, InternalOptions, Scope}
import scala.build.{Build, LocalRepo, Logger, Sources}

class BuildProjectTests extends TestUtil.ScalaCliBuildSuite {

  class LoggerMock extends Logger {
    var diagnostics: List[Diagnostic]                      = Nil
    override def error(message: String): Unit              = sys.error(message)
    override def message(message: => String): Unit         = System.err.println(message)
    override def log(s: => String): Unit                   = System.err.println(s)
    override def log(s: => String, debug: => String): Unit = System.err.println(s)
    override def debug(s: => String): Unit                 = System.err.println(s)
    override def log(diagnostics: Seq[Diagnostic]): Unit   = {
      this.diagnostics = this.diagnostics ++ diagnostics
    }
    override def log(ex: BuildException): Unit = {
      ex.printStackTrace()
      System.err.println(ex.message)
    }
    override def debug(ex: BuildException): Unit = {
      ex.printStackTrace()
      System.err.println(ex.message)
    }
    override def exit(ex: BuildException): Nothing = {
      ex.printStackTrace()
      System.err.println(ex.message)
      sys.exit(1)
    }
    override def coursierLogger(message: String): CacheLogger          = CacheLogger.nop
    override def bloopRifleLogger: BloopRifleLogger                    = BloopRifleLogger.nop
    override def scalaJsLogger: ScalaJsLogger                          = NullLogger
    override def scalaNativeTestLogger: scala.scalanative.build.Logger =
      scala.scalanative.build.Logger.nullLogger
    override def scalaNativeCliInternalLoggerOptions: List[String] =
      List()
    override def compilerOutputStream: PrintStream = System.out
    override def verbosity: Int                    = 0
    override def experimentalWarning(featureName: String, featureType: FeatureType): Unit =
      System.err.println(s"experimental: $featureName")
    override def flushExperimentalWarnings: Unit = ()
  }

  test("workspace for bsp") {
    val options = BuildOptions(
      internal = InternalOptions(localRepository =
        LocalRepo.localRepo(scala.build.Directories.default().localRepoDir, TestLogger())
      )
    )
    val inputs    = Inputs.empty("project")
    val sources   = Sources(Nil, Nil, None, Nil, options)
    val logger    = new LoggerMock()
    val artifacts = options.artifacts(logger, Scope.Test).orThrow

    val project =
      Build.buildProject(inputs, sources, Nil, options, Scope.Main, logger, artifacts).orThrow

    expect(project.workspace == inputs.workspace)
  }
}
