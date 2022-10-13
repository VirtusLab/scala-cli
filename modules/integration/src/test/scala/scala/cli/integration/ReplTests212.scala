package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

// format: off
class ReplTests212 extends ReplTestDefinitions(
  scalaVersionOpt = Some(Constants.scala212)
) {
  // format: on

  private lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

  test("ammonite --spark") {
    TestInputs.empty.fromRoot { root =>
      val ammArgs = Seq(
        "-c",
        """val accum = sc.longAccumulator
          |sc.parallelize(1 to 10).foreach(x => accum.add(x))
          |println(s"Result is ${accum.value}")
          |""".stripMargin
      )
      val wrappedAmmArgs = ammArgs.map {
        if (Properties.isWin)
          a => if (a.contains(" ")) "\"" + a.replace("\"", "\\\"") + "\"" else a
        else
          identity
      }
        .flatMap(arg => Seq("--ammonite-arg", arg))

      val cmd = Seq[os.Shellable](
        TestUtil.cli,
        "--power",
        "repl",
        extraOptions,
        "--ammonite",
        "--standalone-spark",
        "--dependency",
        "org.apache.spark::spark-sql:3.2.0",
        wrappedAmmArgs
      )
      // format: on
      val res         = os.proc(cmd).call(cwd = root)
      val output      = res.out.trim()
      val lines       = output.linesIterator.toVector
      val resultLines = lines.filter(_.startsWith("Result is "))
      expect(resultLines == Seq("Result is 55"))
    }
  }

}
