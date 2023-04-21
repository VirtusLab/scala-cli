package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import java.io.IOException
import scala.build.{Build, BuildThreads, Directories, LocalRepo, Position, Positioned}
import scala.build.options.{BuildOptions, InternalOptions, MaybeScalaVersion, ScalacOpt, Scope}
import scala.build.tests.util.BloopServer
import build.Ops.EitherThrowOps
import dependency.AnyDependency

class DirectiveTests extends munit.FunSuite {

  val buildThreads = BuildThreads.create()

  def bloopConfigOpt = Some(BloopServer.bloopConfig)

  val extraRepoTmpDir = os.temp.dir(prefix = "scala-cli-tests-extra-repo-")
  val directories     = Directories.under(extraRepoTmpDir)

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

  test("resolving position of lib directive") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """//> using dep "com.lihaoyi::utest:0.7.10"
          |""".stripMargin
    )
    testInputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow
        val dep   = build.options.classPathOptions.extraDependencies.toSeq.headOption
        assert(dep.nonEmpty)

        val position = dep.get.positions.headOption
        assert(position.nonEmpty)

        val (startPos, endPos) = position.get match {
          case Position.File(_, startPos, endPos) => (startPos, endPos)
          case _                                  => sys.error("cannot happen")
        }

        expect(startPos == (0, 15))
        expect(endPos == (0, 40))
    }
  }

  test("should parse javac options") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """//> using javacOpt "source", "1.8", "target", "1.8"
          |""".stripMargin
    )
    testInputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) {
      (_, _, maybeBuild) =>
        val build    = maybeBuild.orThrow
        val javacOpt = build.options.javaOptions.javacOptions

        val expectedJavacOpt = Seq("source", "1.8", "target", "1.8")
        expect(javacOpt.map(_.value) == expectedJavacOpt)
    }
  }

  test("should parse graalVM args") {
    val expectedGraalVMArgs @ Seq(noFallback, enableUrl) =
      Seq("--no-fallback", "--enable-url-protocols=http,https")
    TestInputs(
      os.rel / "simple.sc" ->
        s"""//> using packaging.graalvmArgs "$noFallback", "$enableUrl"
           |""".stripMargin
    ).withBuild(baseOptions, buildThreads, bloopConfigOpt) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow
        val graalvmArgs =
          build.options.notForBloopOptions.packageOptions.nativeImageOptions.graalvmArgs
        expect(graalvmArgs.map(_.value) == expectedGraalVMArgs)
    }
  }

  test("resolve toolkit dependency") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """//> using toolkit "latest"
          |""".stripMargin
    )
    testInputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow
        val dep   = build.options.classPathOptions.extraDependencies.toSeq.headOption
        assert(dep.nonEmpty)

        val toolkitDep = dep.get.value
        expect(toolkitDep.organization == "org.scala-lang")
        expect(toolkitDep.name == "toolkit")
        expect(toolkitDep.version == "latest.release")
    }
  }
  for (scope <- Scope.all) {
    def withProjectFile[T](projectFileContent: String)(f: (Build, Boolean) => T): T = TestInputs(
      os.rel / "project.scala" -> projectFileContent,
      os.rel / "Tests.test.scala" ->
        """class Tests extends munit.FunSuite {
          |  test("foo") {
          |    println("foo")
          |  }
          |}
          |""".stripMargin
    ).withBuild(baseOptions, buildThreads, bloopConfigOpt, scope = scope) { (_, _, maybeBuild) =>
      f(maybeBuild.orThrow, scope == Scope.Test)
    }

    test(s"resolve test scope dependencies correctly when building for ${scope.name} scope") {
      withProjectFile(projectFileContent = """//> using dep "com.lihaoyi::os-lib:0.9.1"
                                             |//> using test.dep "org.scalameta::munit::0.7.29"
                                             |""".stripMargin) { (build, isTestScope) =>
        val deps = build.options.classPathOptions.extraDependencies.toSeq.map(_.value)
        expect(deps.nonEmpty)
        val hasMainDeps = deps.exists(d =>
          d.organization == "com.lihaoyi" && d.name == "os-lib" && d.version == "0.9.1"
        )
        val hasTestDeps = deps.exists(d =>
          d.organization == "org.scalameta" && d.name == "munit" && d.version == "0.7.29"
        )
        expect(hasMainDeps)
        expect(if isTestScope then hasTestDeps else !hasTestDeps)
      }
    }
    test(s"resolve test scope javacOpts correctly when building for ${scope.name} scope") {
      withProjectFile(projectFileContent =
        """//> using javacOpt "source", "1.8"
          |//> using test.javacOpt "target", "1.8"
          |//> using test.dep "org.scalameta::munit::0.7.29"
          |""".stripMargin
      ) { (build, isTestScope) =>
        val javacOpts = build.options.javaOptions.javacOptions.map(_.value)
        expect(javacOpts.contains("source"))
        val hasTestJavacOpts = javacOpts.contains("target")
        expect(if isTestScope then hasTestJavacOpts else !hasTestJavacOpts)
      }
    }
    test(s"resolve test scope scalac opts correctly when building for ${scope.name} scope") {
      withProjectFile(projectFileContent =
        """//> using option "--explain"
          |//> using test.option "-deprecation"
          |//> using test.dep "org.scalameta::munit::0.7.29"
          |""".stripMargin
      ) { (build, isTestScope) =>
        val scalacOpts = build.options.scalaOptions.scalacOptions.toSeq.map(_.value.value)
        expect(scalacOpts.contains("--explain"))
        val hasTestScalacOpts = scalacOpts.contains("-deprecation")
        expect(if isTestScope then hasTestScalacOpts else !hasTestScalacOpts)
      }
    }
    test(s"resolve test scope javaOpts correctly when building for ${scope.name} scope") {
      withProjectFile(projectFileContent =
        """//> using javaOpt "-Xmx2g"
          |//> using test.javaOpt "-Dsomething=a"
          |//> using test.dep "org.scalameta::munit::0.7.29"
          |""".stripMargin
      ) { (build, isTestScope) =>
        val javaOpts = build.options.javaOptions.javaOpts.toSeq.map(_.value.value)
        expect(javaOpts.contains("-Xmx2g"))
        val hasTestJavaOpts = javaOpts.contains("-Dsomething=a")
        expect(if isTestScope then hasTestJavaOpts else !hasTestJavaOpts)
      }
    }
    test(s"resolve test scope javaProps correctly when building for ${scope.name} scope") {
      withProjectFile(projectFileContent =
        """//> using javaProp "foo=1"
          |//> using test.javaProp "bar=2"
          |//> using test.dep "org.scalameta::munit::0.7.29"
          |""".stripMargin
      ) { (build, isTestScope) =>
        val javaProps = build.options.javaOptions.javaOpts.toSeq.map(_.value.value)
        expect(javaProps.contains("-Dfoo=1"))
        val hasTestJavaProps = javaProps.contains("-Dbar=2")
        expect(if isTestScope then hasTestJavaProps else !hasTestJavaProps)
      }
    }
    test(s"resolve test scope resourceDir correctly when building for ${scope.name} scope") {
      withProjectFile(projectFileContent =
        """//> using resourceDir "./mainResources"
          |//> using test.resourceDir "./testResources"
          |//> using test.dep "org.scalameta::munit::0.7.29"
          |""".stripMargin
      ) { (build, isTestScope) =>
        val resourcesDirs = build.options.classPathOptions.resourcesDir
        expect(resourcesDirs.exists(_.last == "mainResources"))
        val hasTestResources = resourcesDirs.exists(_.last == "testResources")
        expect(if isTestScope then hasTestResources else !hasTestResources)
      }
    }
  }

  test("handling special syntax for path") {
    val filePath = os.rel / "src" / "simple.scala"
    val testInputs = TestInputs(
      os.rel / filePath ->
        """//> using options "-coverage-out:${.}""""
    )
    testInputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) {
      (root, _, maybeBuild) =>
        val build = maybeBuild.orThrow
        val scalacOptions: Option[Positioned[ScalacOpt]] =
          build.options.scalaOptions.scalacOptions.toSeq.headOption
        assert(scalacOptions.nonEmpty)

        val scalacOpt            = scalacOptions.get.value.value
        val expectedCoveragePath = (root / filePath / os.up).toString
        expect(scalacOpt == s"-coverage-out:$expectedCoveragePath")
    }
  }

  test("handling special syntax for path with more dollars before") {
    val filePath = os.rel / "src" / "simple.scala"
    val testInputs = TestInputs(
      os.rel / filePath ->
        """//> using options "-coverage-out:$$${.}""""
    )
    testInputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) {
      (root, _, maybeBuild) =>
        val build = maybeBuild.orThrow
        val scalacOptions: Option[Positioned[ScalacOpt]] =
          build.options.scalaOptions.scalacOptions.toSeq.headOption
        assert(scalacOptions.nonEmpty)

        val scalacOpt            = scalacOptions.get.value.value
        val expectedCoveragePath = (root / filePath / os.up).toString
        expect(scalacOpt == s"-coverage-out:$$$expectedCoveragePath")
    }
  }

  test("skip handling special syntax for path when double dollar") {
    val filePath = os.rel / "src" / "simple.scala"
    val testInputs = TestInputs(
      os.rel / filePath ->
        """//> using options "-coverage-out:$${.}""""
    )
    testInputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow
        val scalacOptions: Option[Positioned[ScalacOpt]] =
          build.options.scalaOptions.scalacOptions.toSeq.headOption
        assert(scalacOptions.nonEmpty)

        val scalacOpt = scalacOptions.get.value.value
        expect(scalacOpt == """-coverage-out:${.}""")
    }
  }

}
