package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect
import coursier.cache.{CacheLogger, FileCache}
import org.scalajs.logging.{NullLogger, Logger as ScalaJsLogger}

import java.util.concurrent.TimeUnit
import scala.build.Ops.*
import scala.build.bsp.{
  BspServer,
  ScalaScriptBuildServer,
  WrappedSourceItem,
  WrappedSourcesItem,
  WrappedSourcesParams,
  WrappedSourcesResult
}
import scala.build.errors.{
  InvalidBinaryScalaVersionError,
  NoValidScalaVersionFoundError,
  ScalaVersionError,
  UnsupportedScalaVersionError
}
import scala.build.internal.ClassCodeWrapper
import scala.build.options.{BuildOptions, InternalOptions, Scope}
import scala.build.tests.Constants
import scala.build.{Build, BuildThreads, Directories, GeneratedSource, LocalRepo}
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.*

class OfflineTests extends TestUtil.ScalaCliBuildSuite {
  val extraRepoTmpDir = os.temp.dir(prefix = "scala-cli-tests-offline-")
  val directories     = Directories.under(extraRepoTmpDir)
  val baseOptions     = BuildOptions(
    internal = InternalOptions(
      cache = Some(FileCache()
        .withLocation(directories.cacheDir.toString)
        .withCachePolicies(Seq(coursier.cache.CachePolicy.LocalOnly)))
    )
  )

  val buildThreads = BuildThreads.create()

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
        (root, _, maybeBuild) =>
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
