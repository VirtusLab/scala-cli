package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

// format: off
class ReplTestsDefault extends ReplTestDefinitions(
  scalaVersionOpt = None
) {
  // format: on

  test("as jar") {
    val inputs = TestInputs(
      os.rel / "CheckCp.scala" ->
        """//> using lib "com.lihaoyi::os-lib:0.9.1"
          |package checkcp
          |object CheckCp {
          |  def hasDir: Boolean =
          |    sys.props("java.class.path")
          |      .split(java.io.File.pathSeparator)
          |      .toVector
          |      .map(os.Path(_, os.pwd))
          |      .exists(os.isDir(_))
          |}
          |""".stripMargin
    )

    inputs.fromRoot { root =>
      val ammArgs = Seq(
        "-c",
        """println("hasDir=" + checkcp.CheckCp.hasDir)"""
      )
        .map {
          if (Properties.isWin)
            a => if (a.contains(" ")) "\"" + a.replace("\"", "\\\"") + "\"" else a
          else
            identity
        }
        .flatMap(arg => Seq("--ammonite-arg", arg))

      val output =
        os.proc(TestUtil.cli, "--power", "repl", ".", TestUtil.extraOptions, "--ammonite", ammArgs)
          .call(cwd = root)
          .out.trim()
      expect(output == "hasDir=true")

      val asJarOutput = os.proc(
        TestUtil.cli,
        "--power",
        "repl",
        ".",
        TestUtil.extraOptions,
        "--ammonite",
        ammArgs,
        "--as-jar"
      )
        .call(cwd = root)
        .out.trim()
      expect(asJarOutput == "hasDir=false")
    }
  }
}
