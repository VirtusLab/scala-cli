package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class RunTestsDefault extends RunTestDefinitions
    with RunWithWatchTestDefinitions
    with TestDefault {
  def archLinuxTest(): Unit = {
    val message = "Hello from Scala CLI on Arch Linux"
    val inputs = TestInputs(
      os.rel / "hello.sc" ->
        s"""println("$message")
           |""".stripMargin
    )
    val extraOptsStr = extraOptions.mkString(" ") /* meh escaping */
    inputs.fromRoot { root =>
      os.copy(os.Path(TestUtil.cli.head, os.pwd), root / "scala")
      val script =
        s"""#!/usr/bin/env sh
           |set -e
           |./scala --server=false $extraOptsStr . | tee -a output
           |""".stripMargin
      os.write(root / "script.sh", script)
      os.perms.set(root / "script.sh", "rwxr-xr-x")
      val termOpt = if (System.console() == null) Nil else Seq("-t")
      val cmd = Seq[os.Shellable](
        "docker",
        "run",
        "--rm",
        termOpt,
        "-e",
        "SCALA_CLI_VENDORED_ZIS=true",
        "-v",
        s"$root:/data",
        "-w",
        "/data",
        ciOpt,
        Constants.dockerArchLinuxImage,
        "/data/script.sh"
      )
      val res = os.proc(cmd).call(cwd = root)
      System.err.println(res.out.text())
      val output = os.read(root / "output").trim
      expect(output == message)
    }
  }

  if (Properties.isLinux && TestUtil.isNativeCli)
    test("arch linux") {
      archLinuxTest()
    }

  test("3.nightly") { // should run code using scala 3 nightly version
    TestInputs(os.rel / "sample.sc" -> """println("Hello World")""").fromRoot {
      root =>
        val res =
          os.proc(
            TestUtil.cli,
            "run",
            ".",
            "-S",
            "3.nightly",
            TestUtil.extraOptions
          )
            .call(cwd = root)
        expect(res.out.trim() == "Hello World")
    }
  }

  test("as jar") {
    val inputs = TestInputs(
      os.rel / "CheckCp.scala" ->
        """//> using lib "com.lihaoyi::os-lib:0.9.1"
          |object CheckCp {
          |  def main(args: Array[String]): Unit = {
          |    val cp = sys.props("java.class.path")
          |      .split(java.io.File.pathSeparator)
          |      .toVector
          |      .map(os.Path(_, os.pwd))
          |    assert(cp.forall(os.isFile(_)), "Not only files")
          |  }
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "run", extraOptions, ".")
        .call(cwd = root, mergeErrIntoOut = true, check = false)
      expect(res.exitCode != 0)
      val output = res.out.text()
      expect(output.contains("java.lang.AssertionError: assertion failed: Not only files"))

      os.proc(TestUtil.cli, "--power", "run", extraOptions, ".", "--as-jar")
        .call(cwd = root)
    }
  }
}
