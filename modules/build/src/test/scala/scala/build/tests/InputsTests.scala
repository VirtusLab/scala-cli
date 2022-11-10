package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.build.Build
import scala.build.blooprifle.BloopRifleConfig
import scala.build.input.Inputs
import scala.build.input.ElementsUtils.*
import scala.build.options.{BuildOptions, InternalOptions, MaybeScalaVersion}
import scala.build.tests.util.BloopServer
import scala.build.{BuildThreads, Directories, LocalRepo}
import scala.build.internal.Constants

class InputsTests extends munit.FunSuite {
  val buildThreads: BuildThreads               = BuildThreads.create()
  val extraRepoTmpDir: os.Path                 = os.temp.dir(prefix = "scala-cli-tests-extra-repo-")
  val directories: Directories                 = Directories.under(extraRepoTmpDir)
  def bloopConfigOpt: Option[BloopRifleConfig] = Some(BloopServer.bloopConfig)
  val buildOptions: BuildOptions = BuildOptions(
    internal = InternalOptions(
      localRepository = LocalRepo.localRepo(directories.localRepoDir),
      keepDiagnostics = true
    )
  )

  test("forced workspace") {
    val testInputs = TestInputs(
      os.rel / "Foo.scala" ->
        """object Foo {
          |  def main(): Unit = {
          |    println("Hello")
          |  }
          |}
          |""".stripMargin
    )
    val forcedWorkspace = os.rel / "workspace"
    testInputs.withCustomInputs(viaDirectory = false, forcedWorkspaceOpt = Some(forcedWorkspace)) {
      (root, inputs) =>
        expect(inputs.workspace == root / forcedWorkspace)
    }
  }

  test("project.scala file") {
    val testInputs = TestInputs(
      files = Seq(
        os.rel / "custom-dir" / "project.scala" -> "",
        os.rel / "project.scala"                -> s"//> using javaProp \"foo=bar\"".stripMargin,
        os.rel / "foo.scala" ->
          s"""object Foo {
             |  def main(args: Array[String]): Unit =
             |    println("Foo")
             |}
             |""".stripMargin
      ),
      inputArgs = Seq("foo.scala", "custom-dir", "project.scala")
    )
    testInputs.withBuild(buildOptions, buildThreads, bloopConfigOpt) {
      (root, _, buildMaybe) =>
        val javaOptsCheck = buildMaybe match {
          case Right(build: Build.Successful) =>
            build.options.javaOptions.javaOpts.toSeq.head.value.value == "-Dfoo=bar"
          case _ => false
        }
        assert(javaOptsCheck)
        assert(os.exists(root / "custom-dir" / Constants.workspaceDirName))
        assert(!os.exists(root / Constants.workspaceDirName))
    }
  }

  test("setting root dir without project settings file") {
    val testInputs = TestInputs(
      files = Seq(
        os.rel / "custom-dir" / "foo.scala" ->
          s"""object Foo {
             |  def main(args: Array[String]): Unit =
             |    println("Foo")
             |}
             |""".stripMargin,
        os.rel / "bar.scala" -> ""
      ),
      inputArgs = Seq("custom-dir", "bar.scala")
    )
    testInputs.withBuild(buildOptions, buildThreads, bloopConfigOpt) {
      (root, _, _) =>
        assert(os.exists(root / "custom-dir" / Constants.workspaceDirName))
        assert(!os.exists(root / Constants.workspaceDirName))
    }
  }

  test("passing project.scala and its parent directory") {
    val testInputs = TestInputs(
      files = Seq(
        os.rel / "foo.scala" ->
          s"""object Foo {
             |  def main(args: Array[String]): Unit =
             |    println("Foo")
             |}
             |""".stripMargin,
        os.rel / "project.scala" -> ""
      ),
      inputArgs = Seq(".", "project.scala")
    )
    testInputs.withBuild(buildOptions, buildThreads, bloopConfigOpt) {
      (root, inputs, _) =>
        assert(os.exists(root / Constants.workspaceDirName))
        assert(inputs.elements.projectSettingsFiles.length == 1)
    }
  }
}
