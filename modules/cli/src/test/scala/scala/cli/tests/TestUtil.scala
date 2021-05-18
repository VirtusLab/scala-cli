package scala.cli.tests

import scala.cli.Build

object TestUtil {

  implicit class TestBuildOps(private val build: Build) extends AnyVal {
    def generated(): Seq[os.RelPath] =
      os.walk(build.output)
        .filter(os.isFile(_))
        .map(_.relativeTo(build.output))
    def assertGeneratedEquals(expected: String*): Unit = {
      val generated0 = generated()
      assert(
        generated0.map(_.toString).toSet == expected.toSet,
        {
          pprint.log(generated0)
          pprint.log(expected)
          ""
        }
      )
    }
  }

  implicit class TestBuildOptionsOps(private val options: Build.Options) extends AnyVal {
    def enableJs = options.copy(
      scalaJsOptions = Some(Build.scalaJsOptions(options.scalaVersion, options.scalaBinaryVersion))
    )
    def enableNative = options.copy(
      scalaNativeOptions = Some(Build.scalaNativeOptions(options.scalaVersion, options.scalaBinaryVersion))
    )
  }

  implicit class TestAnyOps[T](private val x: T) extends AnyVal {
    def is(expected: T): Unit =
      assert(
        x == expected,
        {
          pprint.log(x)
          pprint.log(expected)
          "Assertion failed"
        }
      )
  }
}
