package scala.cli.commands.tests

import com.eed3si9n.expecty.Expecty.assert as expect

import scala.build.internal.Constants
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

  test("Downgrade Scala version if needed") {
    val replOptions = ReplOptions(
      sharedRepl = SharedReplOptions(
        ammonite = Some(true)
      )
    )
    val maxVersion    = "3.1.3"
    val maxLtsVersion = Constants.scala3Lts
    val buildOptions  = Repl.buildOptions0(replOptions, maxVersion, maxLtsVersion)
    expect(buildOptions.scalaOptions.scalaVersion.flatMap(_.versionOpt).contains(maxVersion))
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

  test("Reject --jshell with --ammonite") {
    val replOptions = ReplOptions(
      sharedRepl = SharedReplOptions(
        jshell = Some(true),
        ammonite = Some(true)
      )
    )
    intercept[Repl.ConflictingReplBackendsError] {
      Repl.buildOptions(replOptions)
    }
  }
}
