package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.build.Ops._
import scala.build.options.{BuildOptions, InternalOptions, ScalaOptions}
import scala.build.tests.util.BloopServer
import scala.build.{BuildThreads, Directories, LocalRepo}

class InputsTests extends munit.FunSuite {

  val buildThreads = BuildThreads.create()
  def bloopConfig  = BloopServer.bloopConfig

  val extraRepoTmpDir = os.temp.dir(prefix = "scala-cli-tests-extra-repo-")
  val directories     = Directories.under(extraRepoTmpDir)

  override def afterAll(): Unit = {
    TestInputs.tryRemoveAll(extraRepoTmpDir)
    buildThreads.shutdown()
  }

  def sv2 = "2.13.5"
  val defaultOptions = BuildOptions(
    scalaOptions = ScalaOptions(
      scalaVersion = Some(sv2),
      scalaBinaryVersion = None
    ),
    internal = InternalOptions(
      localRepository = LocalRepo.localRepo(directories.localRepoDir),
      keepDiagnostics = true
    )
  )

  test("find common root path") {
    val testInputs = TestInputs(
      os.rel / "a" / "a.sc" -> """println("a")""",
      os.rel / "b" / "b.sc" -> """println("b")"""
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) {
      (root, _, maybeBuild) =>
        val workspacePath = maybeBuild.orThrow.successfulOpt.get.inputs.workspace
        expect(root == workspacePath)
    }
  }

  test("find nested common root path") {
    val testInputs = TestInputs(
      os.rel / "a" / "a.sc" -> """println("a")""",
      os.rel / "a" / "b.sc" -> """println("b")"""
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) {
      (root, _, maybeBuild) =>
        val workspacePath     = maybeBuild.orThrow.successfulOpt.get.inputs.workspace
        val expectedWorkspace = root / "a"
        expect(expectedWorkspace == workspacePath)
    }
  }

  test("find nested common root path with different dir") {
    val testInputs = TestInputs(
      os.rel / "a" / "b" / "a.sc"       -> """println("a")""",
      os.rel / "a" / "b" / "c" / "b.sc" -> """println("a")""",
      os.rel / "a" / "c" / "c.sc"       -> """println("b")"""
    )
    testInputs.withBuild(defaultOptions, buildThreads, bloopConfig) {
      (root, _, maybeBuild) =>
        val workspacePath     = maybeBuild.orThrow.successfulOpt.get.inputs.workspace
        val expectedWorkspace = root / "a"
        expect(expectedWorkspace == workspacePath)
    }
  }
}
