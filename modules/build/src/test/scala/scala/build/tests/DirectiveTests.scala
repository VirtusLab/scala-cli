package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import java.io.IOException
import scala.build.{Build, BuildThreads, Directories, LocalRepo, Position, Positioned}
import scala.build.options.{BuildOptions, InternalOptions, MaybeScalaVersion, ScalacOpt, Scope}
import scala.build.tests.util.BloopServer
import build.Ops.EitherThrowOps
import dependency.AnyDependency

import scala.build.errors.{
  CompositeBuildException,
  DependencyFormatError,
  FetchingDependenciesError,
  ToolkitDirectiveMissingVersionError
}

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

  test(s"resolve toolkit & toolkit-test dependency with version passed") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        s"""//> using toolkit latest
           |""".stripMargin
    )
    testInputs.withBuilds(baseOptions, buildThreads, bloopConfigOpt) {
      (_, _, maybeBuilds) =>
        val expectedVersion = "latest.release"
        val builds          = maybeBuilds.orThrow
        val Some(mainBuild) = builds.get(Scope.Main)
        val Some(toolkitDep) =
          mainBuild.options.classPathOptions.extraDependencies.toSeq.headOption.map(_.value)
        expect(toolkitDep.organization == Constants.toolkitOrganization)
        expect(toolkitDep.name == Constants.toolkitName)
        expect(toolkitDep.version == expectedVersion)
        val Some(testBuild) = builds.get(Scope.Test)
        val Some(toolkitTestDep) =
          testBuild.options.classPathOptions.extraDependencies.toSeq.headOption.map(_.value)
        expect(toolkitTestDep.organization == Constants.toolkitOrganization)
        expect(toolkitTestDep.name == Constants.toolkitTestName)
        expect(toolkitTestDep.version == expectedVersion)
    }
  }

  for (toolkitDirectiveKey <- Seq("toolkit", "test.toolkit"))
    test(s"missing $toolkitDirectiveKey version produces an informative error message") {
      val testInputs = TestInputs(
        os.rel / "simple.sc" ->
          s"""//> using $toolkitDirectiveKey
             |""".stripMargin
      )
      testInputs.withBuilds(baseOptions, buildThreads, bloopConfigOpt) {
        (_, _, maybeBuilds) =>
          maybeBuilds match
            case Left(ToolkitDirectiveMissingVersionError(_, errorKey)) =>
              expect(errorKey == toolkitDirectiveKey)
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
    test(s"resolve test scope toolkit dependency correctly when building for ${scope.name} scope") {
      withProjectFile(
        projectFileContent =
          s"""//> using test.toolkit ${Constants.toolkitVersion}
             |""".stripMargin
      ) { (build, isTestScope) =>
        val deps = build.options.classPathOptions.extraDependencies.toSeq.map(_.value)
        if isTestScope then expect(deps.nonEmpty)
        val hasToolkitDep =
          deps.exists(d =>
            d.organization == Constants.toolkitOrganization &&
            d.name == Constants.toolkitName &&
            d.version == Constants.toolkitVersion
          )
        val hasTestToolkitDep =
          deps.exists(d =>
            d.organization == Constants.toolkitOrganization &&
            d.name == Constants.toolkitTestName &&
            d.version == Constants.toolkitVersion
          )
        expect(if isTestScope then hasToolkitDep else !hasToolkitDep)
        expect(if isTestScope then hasTestToolkitDep else !hasTestToolkitDep)
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

  test("resolve typelevel toolkit dependency") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """//> using toolkit typelevel:latest
          |""".stripMargin
    )
    testInputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow
        val dep   = build.options.classPathOptions.extraDependencies.toSeq.headOption
        assert(dep.nonEmpty)

        val toolkitDep = dep.get.value
        expect(toolkitDep.organization == Constants.typelevelToolkitOrganization)
        expect(toolkitDep.name == Constants.toolkitName)
        expect(toolkitDep.version == "latest.release")
    }
  }

  def testSourceJar(getDirectives: (String, String) => String): Unit = {
    val dummyJar        = "Dummy.jar"
    val dummySourcesJar = "Dummy-sources.jar"
    TestInputs(
      os.rel / "Main.scala" ->
        s"""${getDirectives(dummyJar, dummySourcesJar)}
           |object Main extends App {
           |  println("Hello")
           |}
           |""".stripMargin,
      os.rel / dummyJar        -> "dummy",
      os.rel / dummySourcesJar -> "dummy-sources"
    ).withBuild(baseOptions, buildThreads, bloopConfigOpt) {
      (root, _, maybeBuild) =>
        val build     = maybeBuild.orThrow
        val Some(jar) = build.options.classPathOptions.extraClassPath.headOption
        expect(jar == root / dummyJar)
        val Some(sourceJar) = build.options.classPathOptions.extraSourceJars.headOption
        expect(sourceJar == root / dummySourcesJar)
    }
  }

  test("source jar") {
    testSourceJar((dummyJar, dummySourcesJar) =>
      s"""//> using jar $dummyJar
         |//> using sourceJar $dummySourcesJar""".stripMargin
    )
  }

  test("assumed source jar") {
    testSourceJar((dummyJar, dummySourcesJar) =>
      s"//> using jars $dummyJar $dummySourcesJar"
    )
  }

  test("include test.resourceDir into sources for test scope") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """//> using test.resourceDir foo
          |""".stripMargin
    )
    testInputs.withBuild(baseOptions, buildThreads, bloopConfigOpt, scope = Scope.Test) {
      (root, _, maybeBuild) =>
        val build =
          maybeBuild.toOption.flatMap(_.successfulOpt).getOrElse(sys.error("cannot happen"))
        val resourceDirs = build.sources.resourceDirs

        expect(resourceDirs.nonEmpty)
        expect(resourceDirs.length == 1)
        expect(resourceDirs == Seq(root / "foo"))
    }
  }
  test("parse boolean for publish.doc") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """//> using publish.doc false
          |""".stripMargin
    )
    testInputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow
        val publishOptionsCI =
          build.options.notForBloopOptions.publishOptions.contextual(isCi = true)
        val publishOptionsLocal =
          build.options.notForBloopOptions.publishOptions.contextual(isCi = false)

        expect(publishOptionsCI.docJar.contains(false))
        expect(publishOptionsLocal.docJar.contains(false))
    }
  }

  test("dependency parsing error with position") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """//> using dep not-a-dep
          |""".stripMargin
    )
    testInputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) {
      (root, _, maybeBuild) =>
        expect(maybeBuild.isLeft)
        val error = maybeBuild.left.toOption.get

        error match {
          case error: DependencyFormatError =>
            expect(
              error.message == "Error parsing dependency 'not-a-dep': malformed module: not-a-dep"
            )
            expect(error.positions.length == 1)
            expect(error.positions.head == Position.File(
              Right(root / "simple.sc"),
              (0, 14),
              (0, 23)
            ))
          case _ => fail("unexpected BuildException type")
        }
    }
  }

  test("separate dependency resolution errors for each dependency") {
    val testInputs = TestInputs(
      os.rel / "simple.sc" ->
        """//> using dep org.xyz::foo:0.0.1
          |//> using dep com.lihaoyi::os-lib:0.9.1 org.qwerty::bar:0.0.1
          |""".stripMargin
    )
    testInputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) {
      (root, _, maybeBuild) =>
        expect(maybeBuild.isLeft)
        val errors = maybeBuild.left.toOption.get

        errors match {
          case error: CompositeBuildException =>
            expect(error.exceptions.length == 2)
            expect(error.exceptions.forall(_.isInstanceOf[FetchingDependenciesError]))
            expect(error.exceptions.forall(_.positions.length == 1))

            {
              val xyzError = error.exceptions.find(_.message.contains("org.xyz")).get
              expect(xyzError.message.startsWith("Error downloading org.xyz:foo"))
              expect(!xyzError.message.contains("com.lihaoyi"))
              expect(!xyzError.message.contains("org.qwerty"))
              expect(xyzError.positions.head == Position.File(
                Right(root / "simple.sc"),
                (0, 14),
                (0, 32)
              ))
            }

            {
              val qwertyError = error.exceptions.find(_.message.contains("org.qwerty")).get
              expect(qwertyError.message.contains("Error downloading org.qwerty:bar"))
              expect(!qwertyError.message.contains("com.lihaoyi"))
              expect(!qwertyError.message.contains("org.xyz"))
              expect(qwertyError.positions.head == Position.File(
                Right(root / "simple.sc"),
                (1, 40),
                (1, 61)
              ))
            }
          case _ => fail("unexpected BuildException type")
        }
    }
  }
}
