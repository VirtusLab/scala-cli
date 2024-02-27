package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.{Properties, Try}

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

  if (!Properties.isMac || !TestUtil.isNativeCli || !TestUtil.isCI)
    // TODO make this pass reliably on Mac CI
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
          os.proc(
            TestUtil.cli,
            "--power",
            "publish",
            "--offline",
            "--publish-repo",
            testRepo,
            "lib"
          )
            .call(cwd = root)

        publishLib()

        val proc = os.proc(
          TestUtil.cli,
          "--power",
          "run",
          "--offline",
          "app",
          "-w",
          "-r",
          testRepo.toNIO.toUri.toASCIIString
        )
          .spawn(cwd = root)

        try
          TestUtil.withThreadPool("watch-artifacts-test", 2) { pool =>
            val timeout = Duration("90 seconds")
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

  test("watch test - no infinite loop") {

    val fileName = "watch.scala"

    val inputs = TestInputs(
      os.rel / fileName ->
        """//> using lib "org.scalameta::munit::0.7.29"
          |
          |class MyTests extends munit.FunSuite {
          |    test("is true true") { assert(true) }
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val proc = os.proc(TestUtil.cli, "test", "-w", "watch.scala")
        .spawn(cwd = root, mergeErrIntoOut = true)

      val watchingMsg = "Watching sources, press Ctrl+C to exit, or press Enter to re-run."
      val testingMsg  = "MyTests:"

      try
        TestUtil.withThreadPool("watch-test-test", 2) { pool =>
          val timeout     = Duration("10 seconds")
          implicit val ec = ExecutionContext.fromExecutorService(pool)

          def lineReadIter = Iterator.continually(Try(TestUtil.readLine(proc.stdout, ec, timeout)))
            .takeWhile(_.isSuccess)
            .map(_.get)

          val beforeAppendOut = lineReadIter.toSeq
          expect(beforeAppendOut.count(_.contains(testingMsg)) == 1)
          expect(beforeAppendOut.count(_.contains(watchingMsg)) == 1)
          expect(beforeAppendOut.last.contains(watchingMsg))

          os.write.append(root / fileName, "\n//comment")

          val afterAppendOut = lineReadIter.toSeq
          expect(afterAppendOut.count(_.contains(testingMsg)) == 1)
          expect(afterAppendOut.count(_.contains(watchingMsg)) == 1)
          expect(afterAppendOut.last.contains(watchingMsg))
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
