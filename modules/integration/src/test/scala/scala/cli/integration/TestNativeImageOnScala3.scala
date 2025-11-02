package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class TestNativeImageOnScala3 extends ScalaCliSuite {
  override def group: ScalaCliSuite.TestGroup = ScalaCliSuite.TestGroup.First

  def runTest(args: String*)(expectedLines: String*)(code: String): Unit = {
    val dest =
      if (Properties.isWin) "testApp.exe"
      else "testApp"

    val inputs = TestInputs(os.rel / "Hello.scala" -> code)
    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "--power",
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

      val res         = os.proc(root / dest, args).call(cwd = root)
      val outputLines = res.out.trim().linesIterator.to(Seq)
      expect(expectedLines == outputLines)
    }
  }

  for {
    scalaVersion <- TestUtil.legacyScalaVersionsOnePerMinor.sorted ++ Seq(
      Constants.scala3Lts,
      Constants.scala3Next,
      Constants.scala3NextRc
    )
  }
    test(s"lazy vals ($scalaVersion)") {
      runTest("1")("2") {
        s"""//> using scala $scalaVersion
           |class A(a: String) { lazy val b = a.toInt + 1 }
           |@main def add1(i: String) = println(A(i).b)
           |""".stripMargin
      }
    }

  test("lazy vals and enums with default scala version") {
    runTest("1")("2", "A") {
      """class A(a: String) { lazy val b = a.toInt + 1 }
        |enum Ala:
        |  case A
        |  case B
        |@main def add1(i: String) =
        | println(A(i).b)
        | println(Ala.valueOf("A"))
        |""".stripMargin
    }
  }
}
