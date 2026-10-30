package scala.build.tests
import com.eed3si9n.expecty.Expecty.expect
import coursier.cache.FileCache

import scala.build.compiler.SimpleScalaCompilerMaker
import scala.build.errors.ScalaVersionError
import scala.build.input.{Inputs, ScalaCliInvokeData}
import scala.build.options.{BuildOptions, InternalOptions}
import scala.build.tests.Constants
import scala.build.{Build, BuildThreads, Directories}

class OfflineTests extends TestUtil.ScalaCliBuildSuite {
  val extraRepoTmpDir: os.Path  = os.temp.dir(prefix = "scala-cli-tests-offline-")
  val directories: Directories  = Directories.under(extraRepoTmpDir)
  val baseOptions: BuildOptions = BuildOptions(
    internal = InternalOptions(
      cache = Some(FileCache()
        .withLocation(directories.cacheDir.toString)
        .copy(cachePolicies = Seq(coursier.cache.CachePolicy.LocalOnly)))
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

  test("no Scala compiler is needed when there is nothing to compile and Scala library is off") {
    TestInputs.withTmpDir("scala-cli-tests-offline-no-sources-") { root =>
      val options = baseOptions.copy(
        scalaOptions = baseOptions.scalaOptions.copy(addScalaLibrary = Some(false))
      )
      val maybeBuilds = Build.build(
        inputs = Inputs.empty(root, enableMarkdown = false),
        options = options,
        compilerMaker = SimpleScalaCompilerMaker("java", Nil),
        docCompilerMakerOpt = None,
        logger = TestLogger(),
        crossBuilds = false,
        buildTests = false,
        partial = None,
        actionableDiagnostics = None
      )(using ScalaCliInvokeData.dummy)
      maybeBuilds match {
        case Left(e) =>
          munit.Assertions.fail(s"Build failed with:${System.lineSeparator()} ${e.getMessage}")
        case Right(builds) =>
          val build = builds.main.successfulOpt
          expect(build.nonEmpty)
          expect(build.exists(_.artifacts.scalaOpt.isEmpty))
      }
    }
  }
}
