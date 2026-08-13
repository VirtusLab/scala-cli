package scala.cli.commands.tests

import com.eed3si9n.expecty.Expecty.assert as expect

import scala.build.errors.MalformedInputError
import scala.cli.commands.run.{Run, RunOptions}
import scala.cli.commands.shared.{SharedOptions, SharedPythonOptions}

class RunOptionsTests extends munit.FunSuite {
  test("ScalaPy version") {
    val ver        = "X.Y.Z"
    val runOptions = RunOptions(
      shared = SharedOptions(
        sharedPython = SharedPythonOptions(
          scalaPyVersion = Some(ver)
        )
      )
    )
    val buildOptions = Run.buildOptions(runOptions).value
    expect(buildOptions.notForBloopOptions.scalaPyVersion.contains(ver))
  }

  test("resolve toolkit dependency") {
    val runOptions = RunOptions(
      shared = SharedOptions(
        withToolkit = Some("latest")
      )
    )
    val buildOptions = Run.buildOptions(runOptions).value
    val dep          = buildOptions.classPathOptions.extraDependencies.toSeq.headOption
    assert(dep.nonEmpty)

    val toolkitDep = dep.get.value
    expect(toolkitDep.organization == "org.scala-lang")
    expect(toolkitDep.name == "toolkit")
    expect(toolkitDep.version == "latest.release")
  }

  test("resolve typelevel toolkit dependency") {
    val runOptions = RunOptions(
      shared = SharedOptions(
        withToolkit = Some("typelevel:latest")
      )
    )
    val buildOptions = Run.buildOptions(runOptions).value
    val dep          = buildOptions.classPathOptions.extraDependencies.toSeq.headOption
    assert(dep.nonEmpty)

    val toolkitDep = dep.get.value
    expect(toolkitDep.organization == "org.typelevel")
    expect(toolkitDep.name == "toolkit")
    expect(toolkitDep.version == "latest.release")
  }

  test("reject malformed toolkit coordinates") {
    SharedOptions(withToolkit = Some("scala:")).buildOptions() match
      case Left(error: MalformedInputError) =>
        expect(error.input == "scala:")
      case result =>
        fail(s"Expected malformed toolkit coordinates error, got $result")
  }

  test("sloth option") {
    val runOptions = RunOptions(
      shared = SharedOptions(
        sloth = Some(true)
      )
    )
    val buildOptions = Run.buildOptions(runOptions).value
    expect(buildOptions.notForBloopOptions.slothOpt.contains(true))
  }

  test("sloth-agent option") {
    val runOptions = RunOptions(
      shared = SharedOptions(
        slothAgent = Some(true)
      )
    )
    val buildOptions = Run.buildOptions(runOptions).value
    expect(buildOptions.notForBloopOptions.slothAgentOpt.contains(true))
  }
}
