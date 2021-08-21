package scala.cli.integration

import scala.util.Properties

import com.eed3si9n.expecty.Expecty.expect

class ReplTests213 extends ReplTestDefinitions(
  scalaVersionOpt = Some(Constants.scala213)
) {

  private lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

  test("ammonite with extra JAR") {
    TestInputs(Nil).fromRoot { root =>
      val ammArgs = Seq("-c", """import shapeless._; println("Here's an HList: " + (2 :: true :: "a" :: HNil))""")
        .map {
          if (Properties.isWin)
            a => if (a.contains(" ")) "\"" + a.replace("\"", "\\\"") + "\"" else a
          else
            identity
        }
        .flatMap(arg => Seq("--ammonite-arg", arg))
      val shapelessJar = os.proc("cs", "fetch", "--intransitive", "com.chuusai:shapeless_2.13:2.3.7")
        .call()
        .out
        .text()
        .trim
      val res = os.proc(TestUtil.cli, "repl", extraOptions, "--jar", shapelessJar, "--ammonite", ammArgs).call(cwd = root)
      val output = res.out.text().trim
      expect(output == "Here's an HList: 2 :: true :: a :: HNil")
    }
  }

}
