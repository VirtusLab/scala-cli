package scala.build.tests

import scala.build.{Build, Positioned}
import scala.build.options.{BuildOptions, Platform}
import munit.Assertions.assertEquals

object TestUtil {

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
          pprint.log(generated0.map(_.toString))
          pprint.log(expected)
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
