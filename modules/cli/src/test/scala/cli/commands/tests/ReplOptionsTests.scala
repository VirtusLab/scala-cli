package scala.cli.commands.tests

import com.eed3si9n.expecty.Expecty.assert as expect

import scala.cli.commands.repl.{Repl, ReplOptions, SharedReplOptions}
import scala.cli.commands.shared.{SharedOptions, SharedPythonOptions}

class ReplOptionsTests extends munit.FunSuite {
  test("ScalaPy version") {
    val ver         = "X.Y.Z"
    val replOptions = ReplOptions(
      shared = SharedOptions(
        sharedPython = SharedPythonOptions(
          scalaPyVersion = Some(ver)
        )
      )
    )
    val buildOptions = Repl.buildOptions(replOptions).value
    expect(buildOptions.notForBloopOptions.scalaPyVersion.contains(ver))
  }

  test("Propagate --jshell to build options") {
    val replOptions = ReplOptions(
      sharedRepl = SharedReplOptions(
        jshell = Some(true)
      )
    )
    val buildOptions = Repl.buildOptions(replOptions).value
    expect(buildOptions.notForBloopOptions.replOptions.useJshellOpt.contains(true))
  }

  test("Read --repl-init-script-file contents") {
    val initScriptFile = os.temp(prefix = "scala-cli-repl-options-init-", suffix = ".sc")
    val initScript     = """println("from shared repl options")"""
    os.write.over(initScriptFile, initScript)
    val resolved = Repl.readInitScriptFile(initScriptFile.toString).toOption.get
    expect(resolved == initScript)
  }
}
