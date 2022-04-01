package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect
import scala.util.Properties

class TestNativeImageOnScala3 extends munit.FunSuite {
  test("lazy vals") {
    val dest =
      if (Properties.isWin) "add1.exe"
      else "add1"
    val inputs = TestInputs(
      Seq(
        os.rel / "Hello.scala" ->
          """//> using scala "3.1.1"
            |class A(a: String) { lazy val b = a.toInt + 1 }
            |@main def add1(i: String) = println(A(i).b)
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "package",
        ".",
        "--native-image",
        "-o",
        dest,
        "--",
        "--no-fallback"
      ).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      expect(os.isFile(root / dest))

      // FIXME Check that dest is indeed a binary?

      val res    = os.proc(root / dest, "1").call(cwd = root)
      val output = res.out.text().trim
      expect(output == "2")
    }
  }
}
