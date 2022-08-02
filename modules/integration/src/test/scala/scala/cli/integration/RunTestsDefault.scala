package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class RunTestsDefault extends RunTestDefinitions(scalaVersionOpt = None) {

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
      // format: off
      val cmd = Seq[os.Shellable](
        "docker", "run", "--rm", termOpt,
        "-e", "SCALA_CLI_VENDORED_ZIS=true",
        "-v", s"${root}:/data",
        "-w", "/data",
        ciOpt,
        Constants.dockerArchLinuxImage,
        "/data/script.sh"
      )
      // format: on
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

}
