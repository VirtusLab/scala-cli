package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

abstract class WithWarmUpScalaCliSuite extends ScalaCliSuite {

  def warmUpExtraTestOptions: Seq[String]

  // warm-up run that downloads compiler bridges
  // The "Downloading compiler-bridge (from bloop?) pollute the output, and would make the first test fail.
  lazy val warmupTest: Unit = {
    System.err.println("Running warmup test…")
    warmUpTests(ignoreErrors = true)
    System.err.println("Done running warmup test.")
  }

  def warmUpTests(ignoreErrors: Boolean): Unit = {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""val msg = "$message"
           |println(msg)
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output =
        os.proc(TestUtil.cli, warmUpExtraTestOptions, fileName).call(cwd = root).out.trim()
      if (!ignoreErrors)
        expect(output == message)
    }
  }

  override def test(name: String)(body: => Any)(implicit loc: munit.Location): Unit =
    super.test(name) { warmupTest; body }(loc)

  override def test(name: munit.TestOptions)(body: => Any)(implicit loc: munit.Location): Unit =
    super.test(name) { warmupTest; body }(loc)
}
