package scala.cli.commands.tests

import com.eed3si9n.expecty.Expecty.{assert => expect}

import scala.cli.commands.run.Run
import scala.cli.commands.{RunOptions, SharedPythonOptions, SharedRunOptions}

class RunOptionsTests extends munit.FunSuite {

  test("ScalaPy version") {
    val ver = "X.Y.Z"
    val runOptions = RunOptions(
      sharedRun = SharedRunOptions(
        sharedPython = SharedPythonOptions(
          scalaPyVersion = Some(ver)
        )
      )
    )
    val buildOptions = Run.buildOptions(runOptions).value
    expect(buildOptions.notForBloopOptions.scalaPyVersion.contains(ver))
  }

}
