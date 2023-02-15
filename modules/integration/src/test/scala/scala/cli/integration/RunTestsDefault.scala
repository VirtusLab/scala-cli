package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
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
        "-v", s"$root:/data",
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

  test("watch artifacts") {
    val libSourcePath = os.rel / "lib" / "Messages.scala"
    def libSource(hello: String) =
      s"""//> using publish.organization "test-org"
         |//> using publish.name "messages"
         |//> using publish.version "0.1.0"
         |
         |package messages
         |
         |object Messages {
         |  def hello(name: String) = s"$hello $$name"
         |}
         |""".stripMargin
    val inputs = TestInputs(
      libSourcePath -> libSource("Hello"),
      os.rel / "app" / "TestApp.scala" ->
        """//> using lib "test-org::messages:0.1.0"
          |
          |package testapp
          |
          |import messages.Messages
          |
          |@main
          |def run(): Unit =
          |  println(Messages.hello("user"))
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val testRepo = root / "test-repo"

      def publishLib(): Unit =
        os.proc(TestUtil.cli, "--power", "publish", "--publish-repo", testRepo, "lib")
          .call(cwd = root)

      publishLib()

      val proc = os.proc(TestUtil.cli, "run", "app", "-w", "-r", testRepo.toNIO.toUri.toASCIIString)
        .spawn(cwd = root)

      try
        TestUtil.withThreadPool("watch-artifacts-test", 2) { pool =>
          val timeout = Duration("20 seconds")
          val ec      = ExecutionContext.fromExecutorService(pool)

          val output = TestUtil.readLine(proc.stdout, ec, timeout)
          expect(output == "Hello user")

          os.write.over(root / libSourcePath, libSource("Hola"))
          publishLib()

          val secondOutput = TestUtil.readLine(proc.stdout, ec, timeout)
          expect(secondOutput == "Hola user")
        }
      finally
        if (proc.isAlive()) {
          proc.destroy()
          Thread.sleep(200L)
          if (proc.isAlive())
            proc.destroyForcibly()
        }
    }
  }

}
