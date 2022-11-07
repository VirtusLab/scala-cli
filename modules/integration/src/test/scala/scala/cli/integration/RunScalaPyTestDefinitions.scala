package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

trait RunScalaPyTestDefinitions { _: RunTestDefinitions =>
  private def maybeScalapyPrefix =
    if (actualScalaVersion.startsWith("2.13.")) ""
    else "import me.shadaj.scalapy.py" + System.lineSeparator()

  def scalapyTest(useDirective: Boolean): Unit = {
    val maybeDirective =
      if (useDirective)
        """//> using python
          |""".stripMargin
      else
        ""
    val maybeCliArg =
      if (useDirective) Nil
      else Seq("--python")
    val inputs = TestInputs(
      os.rel / "helloscalapy.sc" -> {
        maybeDirective +
          s"""$maybeScalapyPrefix
             |import py.SeqConverters
             |val len = py.Dynamic.global.len(List(0, 2, 3).toPythonProxy)
             |println(s"Length is $$len")
             |""".stripMargin
      }
    )

    inputs.fromRoot { root =>
      val res    = os.proc(TestUtil.cli, "run", extraOptions, ".", maybeCliArg).call(cwd = root)
      val output = res.out.trim()
      val expectedOutput = "Length is 3"
      expect(output == expectedOutput)
    }
  }

  test("scalapy from CLI") {
    scalapyTest(useDirective = false)
  }
  test("scalapy via directive") {
    scalapyTest(useDirective = true)
  }

  def scalapyNativeTest(): Unit = {
    val inputs = TestInputs(
      os.rel / "helloscalapy.sc" ->
        s"""$maybeScalapyPrefix
           |import py.SeqConverters
           |py.local {
           |  val len = py.Dynamic.global.len(List(0, 2, 3).toPythonProxy)
           |  println(s"Length is $$len")
           |}
           |""".stripMargin
    )

    inputs.fromRoot { root =>
      val res =
        os.proc(TestUtil.cli, "run", extraOptions, ".", "--python", "--native").call(cwd = root)
      val output = res.out.trim()
        .linesIterator
        .filter { l =>
          // filter out scala-native-cli garbage output
          !l.startsWith("[info] ")
        }
        .mkString(System.lineSeparator())
      val expectedOutput = "Length is 3"
      expect(output == expectedOutput)
    }
  }

  // disabled on Windows for now, for context, see
  // https://github.com/VirtusLab/scala-cli/pull/1270#issuecomment-1237904394
  if (!Properties.isWin)
    test("scalapy native") {
      scalapyNativeTest()
    }
}
