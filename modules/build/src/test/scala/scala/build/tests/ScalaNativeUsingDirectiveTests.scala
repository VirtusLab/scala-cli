package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.build.errors.{NoValueProvidedError, SingleValueExpectedError}
import scala.build.options.{BuildOptions, InternalOptions}
import scala.build.tests.util.BloopServer
import scala.build.{BuildThreads, Directories, LocalRepo}

class ScalaNativeUsingDirectiveTests extends munit.FunSuite {

  val buildThreads = BuildThreads.create()
  def bloopConfig  = Some(BloopServer.bloopConfig)

  val extraRepoTmpDir = os.temp.dir(prefix = "scala-cli-tests-extra-repo-")
  val directories     = Directories.under(extraRepoTmpDir)

  val buildOptions = BuildOptions(
    internal = InternalOptions(
      localRepository = LocalRepo.localRepo(directories.localRepoDir),
      keepDiagnostics = true
    )
  )

  test("ScalaNativeOptions for native-gc with no values") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-gc`
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      expect(
        maybeBuild.left.exists { case _: NoValueProvidedError => true; case _ => false }
      )
    }
  }

  test("ScalaNativeOptions for native-gc with multiple values") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-gc` 78, 12
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(
        maybeBuild.left.exists { case _: SingleValueExpectedError => true; case _ => false }
      )
    }

  }

  test("ScalaNativeOptions for native-gc") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-gc` 78
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    inputs.withLoadedBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(maybeBuild.options.scalaNativeOptions.gcStr.get == "78")
    }
  }

  test("ScalaNativeOptions for native-mode with no values") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-mode`
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      expect(
        maybeBuild.left.exists { case _: NoValueProvidedError => true; case _ => false }
      )
    }
  }

  test("ScalaNativeOptions for native-mode with multiple values") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-mode` "debug", "release-full"
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(
        maybeBuild.left.exists { case _: SingleValueExpectedError => true; case _ => false }
      )
    }
  }

  test("ScalaNativeOptions for native-mode") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-mode` "release-full"
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    inputs.withLoadedBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(maybeBuild.options.scalaNativeOptions.modeStr.get == "release-full")
    }
  }

  test("ScalaNativeOptions for native-version with multiple values") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-version` "0.4.0", "0.3.3"
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(
        maybeBuild.left.exists { case _: SingleValueExpectedError => true; case _ => false }
      )
    }

  }

  test("ScalaNativeOptions for native-version") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-version` "0.4.0"
          |def foo() = println("hello foo")
          |""".stripMargin
    )

    inputs.withLoadedBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(maybeBuild.options.scalaNativeOptions.version.get == "0.4.0")
    }
  }

  test("ScalaNativeOptions for native-compile") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-compile` "compileOption1", "compileOption2"
          |def foo() = println("hello foo")
          |""".stripMargin
    )

    inputs.withLoadedBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(
        maybeBuild.options.scalaNativeOptions.compileOptions(0) == "compileOption1"
      )
      assert(
        maybeBuild.options.scalaNativeOptions.compileOptions(1) == "compileOption2"
      )
    }
  }

  test("ScalaNativeOptions for native-linking and no value") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-linking`
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    inputs.withLoadedBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(maybeBuild.options.scalaNativeOptions.linkingOptions.isEmpty)
    }
  }

  test("ScalaNativeOptions for native-linking") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-linking` "linkingOption1", "linkingOption2"
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    inputs.withLoadedBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(
        maybeBuild.options.scalaNativeOptions.linkingOptions(0) == "linkingOption1"
      )
      assert(
        maybeBuild.options.scalaNativeOptions.linkingOptions(1) == "linkingOption2"
      )
    }
  }

  test("ScalaNativeOptions for native-clang") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-clang` "clang/path"
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    inputs.withLoadedBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(
        maybeBuild.options.scalaNativeOptions.clang.get == "clang/path"
      )
    }
  }

  test("ScalaNativeOptions for native-clang and multiple values") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-clang` "path1", "path2"
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(
        maybeBuild.left.exists { case _: SingleValueExpectedError => true; case _ => false }
      )
    }
  }

  test("ScalaNativeOptions for native-clang-pp") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-clang-pp` "clangpp/path"
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    inputs.withLoadedBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(
        maybeBuild.options.scalaNativeOptions.clangpp.get == "clangpp/path"
      )
    }
  }

  test("ScalaNativeOptions for native-clang-pp and multiple values") {
    val inputs = TestInputs(
      os.rel / "p.sc" ->
        """//> using `native-clang-pp` "path1", "path2"
          |def foo() = println("hello foo")
          |""".stripMargin
    )
    inputs.withBuild(buildOptions, buildThreads, bloopConfig) { (_, _, maybeBuild) =>
      assert(
        maybeBuild.left.exists { case _: SingleValueExpectedError => true; case _ => false }
      )
    }
  }
}
