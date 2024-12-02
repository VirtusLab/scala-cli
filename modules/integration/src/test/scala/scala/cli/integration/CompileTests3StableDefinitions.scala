package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File

trait CompileTests3StableDefinitions { _: CompileTestDefinitions =>
  test(s"TASTY processor does not warn about Scala $actualScalaVersion") {
    TestInputs(os.rel / "simple.sc" -> s"""println("Hello")""")
      .fromRoot { root =>
        val result =
          os.proc(TestUtil.cli, "compile", ".", extraOptions)
            .call(cwd = root, stderr = os.Pipe)
        expect(result.exitCode == 0)
        expect(!result.err.text().contains("cannot post process TASTY files"))
      }
  }

  test("render explain message") {
    val fileName = "Hello.scala"
    val inputs = TestInputs(
      os.rel / fileName -> // should be dump to 3.3.1 after release
        s"""//> using scala 3.3.1-RC1-bin-20230203-3ef1e73-NIGHTLY
           |//> using options --explain
           |
           |class A
           |val i: Int = A()
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val out = os.proc(TestUtil.cli, "compile", extraOptions, fileName)
        .call(cwd = root, check = false, mergeErrIntoOut = true).out.trim()

      expect(out.contains("Explanation"))
    }
  }

  test("as jar") {
    val inputs = TestInputs(
      os.rel / "Foo.scala" ->
        """object Foo {
          |  def n = 2
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val out = os.proc(TestUtil.cli, "compile", extraOptions, ".", "--print-class-path")
        .call(cwd = root)
        .out.trim()
      val cp = out.split(File.pathSeparator).toVector.map(os.Path(_, root))
      expect(cp.headOption.exists(os.isDir(_)))
      expect(cp.drop(1).forall(os.isFile(_)))

      val asJarOut = os.proc(
        TestUtil.cli,
        "--power",
        "compile",
        extraOptions,
        ".",
        "--print-class-path",
        "--as-jar"
      )
        .call(cwd = root)
        .out.trim()
      val asJarCp = asJarOut.split(File.pathSeparator).toVector.map(os.Path(_, root))
      expect(asJarCp.forall(os.isFile(_)))
    }
  }

}
