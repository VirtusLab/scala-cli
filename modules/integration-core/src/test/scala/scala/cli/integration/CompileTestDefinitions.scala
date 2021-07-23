package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

abstract class CompileTestDefinitions(val scalaVersionOpt: Option[String]) extends munit.FunSuite with TestScalaVersionArgs {

  private lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

  val simpleInputs = TestInputs(
    Seq(
      os.rel / "MyTests.scala" ->
        """import $ivy.`com.lihaoyi::utest::0.7.10`, utest._
          |
          |object MyTests extends TestSuite {
          |  val tests = Tests {
          |    test("foo") {
          |      assert(2 + 2 == 4)
          |      println("Hello from " + "tests")
          |    }
          |  }
          |}
          |""".stripMargin,
      os.rel / "scala.conf" -> ""
    )
  )

  test("no arg") {
    simpleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "compile", extraOptions).call(cwd = root).out.text
    }
  }

}
