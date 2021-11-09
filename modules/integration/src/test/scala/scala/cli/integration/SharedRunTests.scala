package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class SharedRunTests extends munit.FunSuite {

  val printScalaVersionInputs = TestInputs(
    Seq(
      os.rel / "print.sc" ->
        s"""println(scala.util.Properties.versionNumberString)
           |""".stripMargin
    )
  )
  val printScalaVersionInputs3 = TestInputs(
    Seq(
      os.rel / "print.sc" ->
        s"""def printStuff(): Unit =
           |  val toPrint = scala.util.Properties.versionNumberString
           |  println(toPrint)
           |printStuff()
           |""".stripMargin
    )
  )
  test("Scala version 2.12") {
    printScalaVersionInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, ".", "--scala", "2.12")
        .call(cwd = root)
        .out.text().trim
      expect(output.startsWith("2.12."))
    }
  }
  test("Scala version 2.13") {
    printScalaVersionInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, ".", "--scala", "2.13")
        .call(cwd = root)
        .out.text().trim
      expect(output.startsWith("2.13."))
    }
  }
  test("Scala version 2") {
    printScalaVersionInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, ".", "--scala", "2")
        .call(cwd = root)
        .out.text().trim
      expect(output.startsWith("2.13."))
    }
  }
  test("Scala version 3") {
    printScalaVersionInputs3.fromRoot { root =>
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, ".", "--scala", "3.0.2")
        .call(cwd = root)
        .out.text().trim
      // Scala 3.0 uses the 2.13 standard library
      expect(output.startsWith("2.13."))
    }
  }

  test("Scala version in config file") {
    val confSv = "2.13.1"
    val inputs = TestInputs(
      Seq(
        os.rel / "test.sc" ->
          s"""@using scala "$confSv"
             |println(scala.util.Properties.versionNumberString)
             |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output =
        os.proc(TestUtil.cli, TestUtil.extraOptions, ".").call(cwd = root).out.text().trim
      expect(output == confSv)
    }
  }

}
