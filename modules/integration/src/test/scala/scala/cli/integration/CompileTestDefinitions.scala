package scala.cli.integration

abstract class CompileTestDefinitions(val scalaVersionOpt: Option[String])
    extends munit.FunSuite with TestScalaVersionArgs {

  protected lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

  val simpleInputs = TestInputs(
    Seq(
      os.rel / "MyTests.scala" ->
        """using com.lihaoyi::utest::0.7.10
          |import utest._
          |
          |object MyTests extends TestSuite {
          |  val tests = Tests {
          |    test("foo") {
          |      assert(2 + 2 == 4)
          |      println("Hello from " + "tests")
          |    }
          |  }
          |}
          |""".stripMargin
    )
  )

  test("no arg") {
    simpleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "compile", extraOptions, ".").call(cwd = root).out.text
    }
  }

  test("__init__ file".only) {
    val simpleInputs2 = TestInputs(
      Seq(
        os.rel / "Color.scala" ->
          """
            |object A{
            |  val x = 1
            |}
            |enum Color:
            |  case Red, Green
            |
            |""".stripMargin,
        os.rel / "__init__.scala" -> "require scala == 3"
      )
    )

    simpleInputs2.fromRoot { root =>
      val t = os.proc(TestUtil.cli, "compile", extraOptions, ".").call(
        cwd = root,
        stdout = os.Inherit
      ).err.text
      println(t)
    }
  }
}
