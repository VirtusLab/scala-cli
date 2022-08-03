package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import java.io.IOException
import scala.build.{BuildThreads, Directories, LocalRepo}
import scala.build.options.{BuildOptions, InternalOptions, MaybeScalaVersion}
import scala.build.tests.util.BloopServer
import build.Ops.EitherThrowOps
import scala.build.Position

class DirectiveTests extends munit.FunSuite {

  val buildThreads = BuildThreads.create()

  def bloopConfigOpt = Some(BloopServer.bloopConfig)

  val extraRepoTmpDir = os.temp.dir(prefix = "scala-cli-tests-extra-repo-")
  val directories = Directories.under(extraRepoTmpDir)

  override def afterAll(): Unit = {
    TestInputs.tryRemoveAll(extraRepoTmpDir)
    buildThreads.shutdown()
  }

  val baseOptions = BuildOptions(
    internal = InternalOptions(
      localRepository = LocalRepo.localRepo(directories.localRepoDir),
      keepDiagnostics = true
    )
  )

  test("resolving position of lib directive ") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """//> using lib "com.lihaoyi::utest:0.7.10"
          |""".stripMargin
    )
    testInputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) {
      (_, _, maybeBuild) =>
        val build       = maybeBuild.orThrow
        val dep         = build.options.classPathOptions.extraDependencies.toSeq.headOption
        assert(dep.nonEmpty)

        val position = dep.get.positions.headOption
        assert(position.nonEmpty)

        val (startPos, endPos) = position.get match {
          case Position.File(_, startPos, endPos) => (startPos, endPos)
          case _ => sys.error("cannot happen")
        }

        expect(startPos == (0,15))
        expect(endPos == (0,40))
    }
  }

}