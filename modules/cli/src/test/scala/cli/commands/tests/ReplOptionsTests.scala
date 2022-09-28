package scala.cli.commands.tests

import com.eed3si9n.expecty.Expecty.{assert => expect}

import scala.cli.commands.{Repl, ReplOptions, SharedPythonOptions, SharedReplOptions}

class ReplOptionsTests extends munit.FunSuite {

  test("ScalaPy version") {
    val ver = "X.Y.Z"
    val replOptions = ReplOptions(
      sharedRepl = SharedReplOptions(
        sharedPython = SharedPythonOptions(
          scalaPyVersion = Some(ver)
        )
      )
    )
    val buildOptions = Repl.buildOptions(replOptions)
    expect(buildOptions.notForBloopOptions.scalaPyVersion.contains(ver))
  }

}
