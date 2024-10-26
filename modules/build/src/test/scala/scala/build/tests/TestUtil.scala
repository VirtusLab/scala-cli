package scala.build.tests

import scala.build.{Build, Positioned}
import scala.build.options.{BuildOptions, Platform}
import munit.Assertions.assertEquals

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

object TestUtil {

  abstract class ScalaCliBuildSuite extends munit.FunSuite {
    extension (munitContext: BeforeEach | AfterEach) {
      def locationAbsolutePath: os.Path =
        (munitContext match {
          case beforeEach: BeforeEach => beforeEach.test
          case afterEach: AfterEach   => afterEach.test
        }).location.path
    }
    override def munitTimeout = new FiniteDuration(120, TimeUnit.SECONDS)
    val testStartEndLogger = new Fixture[Unit]("files") {
      def apply(): Unit = ()

      override def beforeEach(context: BeforeEach): Unit = {
        val fileName = context.locationAbsolutePath.baseName
        System.err.println(
          s">==== ${Console.CYAN}Running '${context.test.name}' from $fileName${Console.RESET}"
        )
      }

      override def afterEach(context: AfterEach): Unit = {
        val fileName = context.locationAbsolutePath.baseName
        System.err.println(
          s"X==== ${Console.CYAN}Finishing '${context.test.name}' from $fileName${Console.RESET}"
        )
      }
    }
    override def munitFixtures = List(testStartEndLogger)
  }

  val isCI = System.getenv("CI") != null

  implicit class TestBuildOps(private val build: Build) extends AnyVal {
    private def successfulBuild: Build.Successful =
      build.successfulOpt.getOrElse {
        sys.error("Compilation failed")
      }
    def generated(): Seq[os.RelPath] =
      os.walk(successfulBuild.output)
        .filter(os.isFile(_))
        .map(_.relativeTo(successfulBuild.output))
    def assertGeneratedEquals(expected: String*): Unit = {
      val generated0 = generated()
      assert(
        generated0.map(_.toString).toSet == expected.toSet, {
          pprint.log(generated0.map(_.toString).sorted)
          pprint.log(expected.sorted)
          ""
        }
      )
    }

    def assertNoDiagnostics = assertEquals(build.diagnostics.toSeq.flatten, Nil)

  }

  implicit class TestBuildOptionsOps(private val options: BuildOptions) extends AnyVal {
    def enableJs =
      options.copy(
        scalaOptions = options.scalaOptions.copy(
          platform = Some(Positioned.none(Platform.JS))
        )
      )
    def enableNative =
      options.copy(
        scalaOptions = options.scalaOptions.copy(
          platform = Some(Positioned.none(Platform.Native))
        )
      )
  }

  implicit class TestAnyOps[T](private val x: T) extends AnyVal {
    def is(expected: T): Unit =
      assert(
        x == expected, {
          pprint.log(x)
          pprint.log(expected)
          "Assertion failed"
        }
      )
  }

  lazy val cs = Constants.cs
}
