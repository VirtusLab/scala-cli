package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect
import coursier.cache.CacheLogger
import org.scalajs.logging.{NullLogger, Logger as ScalaJsLogger}

import java.util.concurrent.TimeUnit
import scala.build.Ops.*
import scala.build.{Build, BuildThreads, Directories, GeneratedSource, LocalRepo}
import scala.build.options.{BuildOptions, InternalOptions, Scope}
import scala.build.bsp.{
  BspServer,
  ScalaScriptBuildServer,
  WrappedSourceItem,
  WrappedSourcesItem,
  WrappedSourcesParams,
  WrappedSourcesResult
}
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.*
import scala.build.bsp.{WrappedSourcesItem, WrappedSourcesResult}
import scala.build.internal.ClassCodeWrapper

class BspServerTests extends munit.FunSuite {
  val extraRepoTmpDir = os.temp.dir(prefix = "scala-cli-tests-actionable-diagnostic-")
  val directories     = Directories.under(extraRepoTmpDir)
  val baseOptions = BuildOptions(
    internal = InternalOptions(
      localRepository = LocalRepo.localRepo(directories.localRepoDir)
    )
  )
  val buildThreads = BuildThreads.create()

  def getScriptBuildServer(
    generatedSources: Seq[GeneratedSource],
    workspace: os.Path,
    scope: Scope = Scope.Main
  ): ScalaScriptBuildServer = {
    val bspServer = new BspServer(null, null, null)
    bspServer.setGeneratedSources(Scope.Main, generatedSources)
    bspServer.setProjectName(workspace, "test", scope)

    bspServer
  }

  test("correct topWrapper and bottomWrapper for wrapped scripts") {
    val testInputs = TestInputs(
      os.rel / "script.sc" ->
        s"""def msg: String = "Hello"
           |
           |println(msg)
           |""".stripMargin
    )

    testInputs.withBuild(baseOptions, buildThreads, None) {
      (root, _, maybeBuild) =>
        val build: Build = maybeBuild.orThrow

        build match {
          case success: Build.Successful =>
            val generatedSources = success.generatedSources
            expect(generatedSources.size == 1)
            val wrappedScript     = generatedSources.head
            val wrappedScriptCode = os.read(wrappedScript.generated)
            val topWrapper = wrappedScriptCode
              .linesIterator
              .take(wrappedScript.topWrapperLineCount)
              .mkString("", System.lineSeparator(), System.lineSeparator())

            val bspServer = getScriptBuildServer(generatedSources, root)

            val wrappedSourcesResult: WrappedSourcesResult = bspServer
              .buildTargetWrappedSources(new WrappedSourcesParams(ArrayBuffer.empty.asJava))
              .get(10, TimeUnit.SECONDS)
            val wrappedItems = wrappedSourcesResult.getItems.asScala

            expect(wrappedItems.size == 1)
            expect(wrappedItems.head.getSources().asScala.size == 1)

            expect(wrappedItems.head.getSources().asScala.head.getTopWrapper == topWrapper)

          case _ => munit.Assertions.fail("Build Failed")
        }

    }
  }
}
