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
          ActionablePreprocessor.generateActionableDiagnostic(build.options).orThrow

        val osLibDiagnosticOpt = updateDiagnostics.collectFirst {
          case diagnostic: ActionableDependencyUpdateDiagnostic => diagnostic
        }

        expect(osLibDiagnosticOpt.nonEmpty)
        val osLibDiagnostic = osLibDiagnosticOpt.get

        expect(Version(osLibDiagnostic.newVersion) > Version(osLibDiagnostic.oldDependency.version))
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
          ActionablePreprocessor.generateActionableDiagnostic(build.options).orThrow

        val osLibDiagnosticOpt = updateDiagnostics.collectFirst {
          case diagnostic: ActionableDependencyUpdateDiagnostic => diagnostic
        }

        expect(osLibDiagnosticOpt.nonEmpty)
        val osLibDiagnostic = osLibDiagnosticOpt.get

        expect(osLibDiagnostic.oldDependency.render == dependencyOsLib)
        expect(Version(osLibDiagnostic.newVersion) > Version(osLibDiagnostic.oldDependency.version))
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
          ActionablePreprocessor.generateActionableDiagnostic(build.options).orThrow

        val osLibDiagnosticOpt = updateDiagnostics.collectFirst {
          case diagnostic: ActionableDependencyUpdateDiagnostic => diagnostic
        }

        expect(osLibDiagnosticOpt.nonEmpty)
        val osLibDiagnostic = osLibDiagnosticOpt.get

        expect(osLibDiagnostic.oldDependency.render == dependencyOsLib)
        expect(Version(osLibDiagnostic.newVersion) > Version(osLibDiagnostic.oldDependency.version))
    }
  }

}
