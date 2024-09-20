package scala.cli.integration

import scala.util.Properties

class ArgsFileTests extends ScalaCliSuite {

  val forOption = List(true, false)

  for (useServer <- forOption) test(
    s"pass scalac options using arguments file ${if (useServer) "with bloop" else "without bloop"}"
  ) {
    val fileName   = "Simple.sc"
    val serverArgs = if (useServer) Nil else List("--server=false")
    val inputs = TestInputs(
      os.rel / "args.txt" -> """|-release
                                |8""".stripMargin,
      os.rel / fileName ->
        s"""|
            |println("Hello :)".repeat(11))
            |""".stripMargin
    )

    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, serverArgs, "@args.txt", fileName).call(
        cwd = root,
        check = false,
        stderr = os.Pipe
      )
      assert(res.exitCode == 1)

      val compilationError = res.err.text()
      assert(compilationError.contains("Compilation failed"))
    }
  }

  if (!Properties.isWin)
    test("pass scalac options using arguments file in shebang script") {
      val inputs = TestInputs(
        os.rel / "args.txt" -> """|-release 8""".stripMargin,
        os.rel / "script-with-shebang" ->
          s"""|#!/usr/bin/env -S ${TestUtil.cli.mkString(" ")} shebang @args.txt
              |
              |println("Hello :)".repeat(11))
              |""".stripMargin
      )

      inputs.fromRoot { root =>
        os.perms.set(root / "script-with-shebang", os.PermSet.fromString("rwx------"))
        val res = os.proc("./script-with-shebang").call(cwd = root, check = false, stderr = os.Pipe)
        assert(res.exitCode == 1)

        val compilationError = res.err.text()
        assert(compilationError.contains("Compilation failed"))
      }
    }

  test("multiple args files") {
    val preCompileDir = "PreCompileDir"
    val runDir        = "RunDir"

    val preCompiledInput = "Message.scala"
    val mainInput        = "Main.scala"

    val expectedOutput = "Hello"

    val outputDir = os.rel / "out"

    TestInputs(
      os.rel / preCompileDir / preCompiledInput -> "case class Message(value: String)",
      os.rel / runDir / mainInput -> s"""object Main extends App { println(Message("$expectedOutput").value) }""",
      os.rel / runDir / "args.txt" -> s"""|-d
                                          |$outputDir""".stripMargin,
      os.rel / runDir / "args2.txt" -> s"""|-cp
                                           |${os.rel / os.up / preCompileDir / outputDir}""".stripMargin
    ).fromRoot { (root: os.Path) =>

      os.proc(
        TestUtil.cli,
        "compile",
        "--scala-opt",
        "-d",
        "--scala-opt",
        outputDir.toString,
        preCompiledInput
      ).call(cwd = root / preCompileDir, stderr = os.Pipe)
      assert((root / preCompileDir / outputDir / "Message.class").toNIO.toFile().exists())

      val compileOutput = root / runDir / outputDir
      os.makeDir.all(compileOutput)
      val runRes = os.proc(
        TestUtil.cli,
        "run",
        "@args.txt",
        "--server=false",
        "@args2.txt",
        mainInput
      ).call(cwd = root / runDir, stderr = os.Pipe)
      assert(runRes.out.trim() == expectedOutput)
      assert((compileOutput / "Main.class").toNIO.toFile().exists())
    }
  }

}
