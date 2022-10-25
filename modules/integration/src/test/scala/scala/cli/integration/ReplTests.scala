package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class ReplTests extends ScalaCliSuite {

  test("calling repl with -Xshow-phases flag") {
    val cmd = Seq[os.Shellable](
      TestUtil.cli,
      "repl",
      "-Xshow-phases"
    )

    val res = os.proc(cmd).call(mergeErrIntoOut = true)
    expect(res.exitCode == 0)
    val output = res.out.text()
    expect(output.contains("parser") && output.contains("typer"))
  }

}
