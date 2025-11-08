package scala.build.tests
import coursier.cache.FileCache

import scala.build.errors.ScalaVersionError
import scala.build.options.{BuildOptions, InternalOptions}
import scala.build.tests.Constants
import scala.build.{BuildThreads, Directories}

class OfflineTests extends TestUtil.ScalaCliBuildSuite {
  val extraRepoTmpDir: os.Path  = os.temp.dir(prefix = "scala-cli-tests-offline-")
  val directories: Directories  = Directories.under(extraRepoTmpDir)
  val baseOptions: BuildOptions = BuildOptions(
    internal = InternalOptions(
      cache = Some(FileCache()
        .withLocation(directories.cacheDir.toString)
        .withCachePolicies(Seq(coursier.cache.CachePolicy.LocalOnly)))
    )
  )

  val buildThreads: BuildThreads = BuildThreads.create()

  for (
    defaultVersion <- Seq(
      Constants.defaultScalaVersion,
      Constants.defaultScala212Version,
      Constants.defaultScala213Version
    )
  )
    test(s"Default versions of Scala should pass without validation for $defaultVersion") {
      val testInputs = TestInputs(
        os.rel / "script.sc" ->
          s"""//> using scala $defaultVersion
             |def msg: String = "Hello"
             |
             |println(msg)
             |""".stripMargin
      )

      testInputs.withBuild(baseOptions, buildThreads, None) {
        (_, _, maybeBuild) =>
          maybeBuild match {
            case Left(e: ScalaVersionError) =>
              munit.Assertions.fail(
                s"Validation Failed with:${System.lineSeparator()} ${e.getMessage}"
              )
            case _ => ()
          }
      }
    }
}
