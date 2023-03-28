package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.build.options.{BuildOptions, InternalOptions, SuppressWarningOptions}
import scala.build.Ops.*
import scala.build.{BuildThreads, Directories, LocalRepo}
import scala.build.actionable.ActionablePreprocessor
import scala.build.actionable.ActionableDiagnostic.*
import scala.build.Position.File
import coursier.core.Version

import scala.build.errors.{BuildException, CompositeBuildException, UnsupportedAmmoniteImportError}

class ActionableDiagnosticTests extends munit.FunSuite {

  val extraRepoTmpDir = os.temp.dir(prefix = "scala-cli-tests-actionable-diagnostic-")
  val directories     = Directories.under(extraRepoTmpDir)
  val baseOptions = BuildOptions(
    internal = InternalOptions(
      localRepository = LocalRepo.localRepo(directories.localRepoDir)
    )
  )
  val buildThreads = BuildThreads.create()

  test("using outdated os-lib") {
    val dependencyOsLib = "com.lihaoyi::os-lib:0.7.8"
    val testInputs = TestInputs(
      os.rel / "Foo.scala" ->
        s"""//> using dep "$dependencyOsLib"
           |
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    )
    testInputs.withBuild(baseOptions, buildThreads, None, actionableDiagnostics = true) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow
        val updateDiagnostics =
          ActionablePreprocessor.generateActionableDiagnostics(build.options).orThrow

        val osLibDiagnosticOpt = updateDiagnostics.collectFirst {
          case diagnostic: ActionableDependencyUpdateDiagnostic => diagnostic
        }

        expect(osLibDiagnosticOpt.nonEmpty)
        val osLibDiagnostic = osLibDiagnosticOpt.get

        expect(Version(osLibDiagnostic.newVersion) > Version(osLibDiagnostic.currentVersion))
    }
  }

  test("using outdated dependencies with --suppress-outdated-dependency-warning") {
    val dependencyOsLib     = "com.lihaoyi::os-lib:0.7.8"
    val dependencyPprintLib = "com.lihaoyi::pprint:0.6.6"
    val testInputs = TestInputs(
      os.rel / "Foo.scala" ->
        s"""//> using dep "$dependencyOsLib"
           |//> using dep "$dependencyPprintLib"
           |
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    )
    val optionsWithSuppress = baseOptions.copy(
      suppressWarningOptions = SuppressWarningOptions(
        suppressOutdatedDependencyWarning = Some(true)
      )
    )

    testInputs.withBuild(optionsWithSuppress, buildThreads, None, actionableDiagnostics = true) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow
        val updateDiagnostics =
          ActionablePreprocessor.generateActionableDiagnostics(build.options).orThrow

        val updateDepsDiagnostics = updateDiagnostics.collect {
          case diagnostic: ActionableDependencyUpdateDiagnostic => diagnostic
        }

        expect(updateDepsDiagnostics.isEmpty)
    }
  }

  test("actionable actions suggest update only to stable version") {
    val testInputs = TestInputs(
      os.rel / "Foo.scala" ->
        s"""//> using dep "test-org::test-name-1:1.0.6"
           |
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    )
    // create fake repository which contains hardcoded versions [1.0.6, 1.0.7, 1.0.7-M1] of test-name-1 library
    // scala-cli should skip non-stable version 1.0.7-M1 and suggest update 1.0.7
    val repoTmpDir = os.temp.dir(prefix = "scala-cli-tests-actionable-diagnostic-repo")
    os.write(
      repoTmpDir / "test-org" / "test-name-1_3" / "maven-metadata.xml",
      """<?xml version="1.0" encoding="UTF-8"?>
        |<metadata>
        |  <groupId>test-org</groupId>
        |  <artifactId>test-name-1_3</artifactId>
        |  <versioning>
        |    <latest>1.0.7-M1</latest>
        |    <release>1.0.7-M1</release>
        |    <versions>
        |      <version>1.0.6</version>
        |      <version>1.0.7</version>
        |      <version>1.0.7-M1</version>
        |    </versions>
        |  </versioning>
        |</metadata>
        |""".stripMargin,
      createFolders = true
    )
    os.write(
      repoTmpDir / "test-org" / "test-name-1_3" / "1.0.6" / "test-name-1_3-1.0.6.pom",
      """<?xml version='1.0' encoding='UTF-8'?>
        |<project>
        |    <groupId>test-org</groupId>
        |    <artifactId>test-name-1_3</artifactId>
        |    <version>1.0.6</version>
        |</project>""".stripMargin,
      createFolders = true
    )
    val withRepoBuildOptions = baseOptions.copy(
      classPathOptions =
        baseOptions.classPathOptions.copy(extraRepositories = Seq(s"file:${repoTmpDir.toString}"))
    )
    testInputs.withBuild(withRepoBuildOptions, buildThreads, None, actionableDiagnostics = true) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow

        val updateDiagnostics =
          ActionablePreprocessor.generateActionableDiagnostics(build.options).orThrow

        val testLibDiagnosticOpt = updateDiagnostics.collectFirst {
          case diagnostic: ActionableDependencyUpdateDiagnostic => diagnostic
        }
        expect(testLibDiagnosticOpt.nonEmpty)
        val testLibDiagnostic = testLibDiagnosticOpt.get

        expect(testLibDiagnostic.newVersion == "1.0.7")
    }
  }

  test("actionable actions should not suggest update to previous version") {
    val testInputs = TestInputs(
      os.rel / "Foo.scala" ->
        s"""//> using dep "test-org::test-name-1:2.0.0-M1"
           |
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    )
    // create fake repository which contains hardcoded versions [1.0.0] of test-name-1 library
    val repoTmpDir = os.temp.dir(prefix = "scala-cli-tests-actionable-diagnostic-repo")
    os.write(
      repoTmpDir / "test-org" / "test-name-1_3" / "maven-metadata.xml",
      """<?xml version="1.0" encoding="UTF-8"?>
        |<metadata>
        |  <groupId>test-org</groupId>
        |  <artifactId>test-name-1_3</artifactId>
        |  <versioning>
        |    <latest>2.0.0-M</latest>
        |    <release>2.0.0-M1</release>
        |    <versions>
        |      <version>1.0.0</version>
        |      <version>2.0.0-M1</version>
        |    </versions>
        |  </versioning>
        |</metadata>
        |""".stripMargin,
      createFolders = true
    )
    os.write(
      repoTmpDir / "test-org" / "test-name-1_3" / "2.0.0-M1" / "test-name-1_3-2.0.0-M1.pom",
      """<?xml version='1.0' encoding='UTF-8'?>
        |<project>
        |    <groupId>test-org</groupId>
        |    <artifactId>test-name-1_3</artifactId>
        |    <version>2.0.0-M1</version>
        |</project>""".stripMargin,
      createFolders = true
    )
    val withRepoBuildOptions = baseOptions.copy(
      classPathOptions =
        baseOptions.classPathOptions.copy(extraRepositories = Seq(s"file:${repoTmpDir.toString}"))
    )
    testInputs.withBuild(withRepoBuildOptions, buildThreads, None, actionableDiagnostics = true) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow

        val updateDiagnostics =
          ActionablePreprocessor.generateActionableDiagnostics(build.options).orThrow

        val testLibDiagnosticOpt = updateDiagnostics.collectFirst {
          case diagnostic: ActionableDependencyUpdateDiagnostic => diagnostic
        }
        expect(testLibDiagnosticOpt.isEmpty)
    }
  }

  test("actionable actions should not suggest update if uses version: latest") {
    val testInputs = TestInputs(
      os.rel / "Foo.scala" ->
        s"""//> using toolkit "latest"
           |
           |object Hello extends App {
           |  os.list(os.pwd).foreach(println)
           |}
           |""".stripMargin
    )
    testInputs.withBuild(baseOptions, buildThreads, None, actionableDiagnostics = true) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow

        val updateDiagnostics =
          ActionablePreprocessor.generateActionableDiagnostics(build.options).orThrow

        val testLibDiagnosticOpt = updateDiagnostics.collectFirst {
          case diagnostic: ActionableDependencyUpdateDiagnostic => diagnostic
        }
        expect(testLibDiagnosticOpt.isEmpty)
    }
  }
}
