package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.collection.mutable

class ReplTests extends ScalaCliSuite {

  test("calling repl with -Xshow-phases flag") {
    val cmd = Seq[os.Shellable](
      TestUtil.cli,
      "repl",
      "-Xshow-phases"
    )

    val builder   = new mutable.StringBuilder
    val readLines = os.ProcessOutput.Readlines(s => builder.append(s))
    val res       = os.proc(cmd).call(stdout = readLines, stderr = readLines)
    expect(res.exitCode == 0)
    val output = builder.mkString
    expect(output.contains("parser") && output.contains("typer"))
  }

}
