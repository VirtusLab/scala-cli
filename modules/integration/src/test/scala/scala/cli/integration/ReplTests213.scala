package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class ReplTests213 extends ReplTestDefinitions with Test213 {
  test("repl custom repositories work") {
    TestInputs.empty.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "repl",
        "--repl-dry-run",
        "--scala",
        Constants.scalaSnapshot213,
        "--repository",
        "https://scala-ci.typesafe.com/artifactory/scala-integration"
      ).call(cwd = root)
    }
  }

  def ammoniteWithExtraJarTest(): Unit = {
    TestInputs.empty.fromRoot { root =>
      val ammArgs = Seq(
        "-c",
        """import shapeless._; println("Here's an HList: " + (2 :: true :: "a" :: HNil))"""
      )
        .map {
          if (Properties.isWin)
            a => if (a.contains(" ")) "\"" + a.replace("\"", "\\\"") + "\"" else a
          else
            identity
        }
        .flatMap(arg => Seq("--ammonite-arg", arg))
      val shapelessJar =
        os.proc(TestUtil.cs, "fetch", "--intransitive", "com.chuusai:shapeless_2.13:2.3.7")
          .call()
          .out
          .text()
          .trim
      val cmd = Seq[os.Shellable](
        TestUtil.cli,
        "--power",
        "repl",
        ammoniteExtraOptions,
        "--jar",
        shapelessJar,
        "--ammonite",
        ammArgs
      )
      val res    = os.proc(cmd).call(cwd = root)
      val output = res.out.trim()
      expect(output == "Here's an HList: 2 :: true :: a :: HNil")
    }
  }

  test(s"ammonite with extra JAR$ammoniteMaxVersionString") {
    ammoniteWithExtraJarTest()
  }

}
