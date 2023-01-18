package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect
import scala.build.options.{BuildOptions, InternalOptions}
import scala.build.Ops._
import scala.build.{BuildThreads, Directories, LocalRepo}
import scala.build.actionable.ActionablePreprocessor
import scala.build.actionable.ActionableDiagnostic._
import coursier.core.Version

class ActionableDiagnosticTests extends munit.FunSuite {

  val extraRepoTmpDir = os.temp.dir(prefix = "scala-cli-tests-actionable-diagnostic-")
  val directories     = Directories.under(extraRepoTmpDir)
  val baseOptions = BuildOptions(
    internal = InternalOptions(
      localRepository = LocalRepo.localRepo(directories.localRepoDir)
    )
  )
  val buildThreads = BuildThreads.create()

  test("update os-lib") {
    val dependencyOsLib = "com.lihaoyi::os-lib:0.7.8"
    val testInputs = TestInputs(
      os.rel / "Foo.scala" ->
        s"""//> using lib "$dependencyOsLib"
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

  test("update ivy dependence upickle") {
    val dependencyOsLib = "com.lihaoyi::upickle:1.4.0"
    val testInputs = TestInputs(
      os.rel / "Foo.scala" ->
        s"""import $$ivy.`$dependencyOsLib`
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

  test("update dep dependence upickle") {
    val dependencyOsLib = "com.lihaoyi::upickle:1.4.0"
    val testInputs = TestInputs(
      os.rel / "Foo.scala" ->
        s"""import $$dep.`$dependencyOsLib`
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

  test("actionable actions suggest update only to stable version") {
    val testInputs = TestInputs(
      os.rel / "Foo.scala" ->
        s"""//> using lib "test-org::test-name-1:1.0.6"
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
}
