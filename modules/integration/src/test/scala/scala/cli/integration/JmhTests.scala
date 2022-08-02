package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.nio.charset.Charset

class JmhTests extends ScalaCliSuite {

  override def group: ScalaCliSuite.TestGroup = ScalaCliSuite.TestGroup.First

  test("simple") {
    val inputs = TestInputs(
      os.rel / "benchmark.scala" ->
        s"""package bench
           |
           |import java.util.concurrent.TimeUnit
           |import org.openjdk.jmh.annotations._
           |
           |@BenchmarkMode(Array(Mode.AverageTime))
           |@OutputTimeUnit(TimeUnit.NANOSECONDS)
           |@Warmup(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
           |@Measurement(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
           |@Fork(0)
           |class Benchmarks {
           |
           |  @Benchmark
           |  def foo(): Unit = {
           |    (1L to 10000000L).sum
           |  }
           |
           |}
           |""".stripMargin
    )
    val expectedInOutput = """Result "bench.Benchmarks.foo":"""
    inputs.fromRoot { root =>
      val res    = os.proc(TestUtil.cli, TestUtil.extraOptions, ".", "--jmh").call(cwd = root)
      val output = res.out.text(Charset.defaultCharset())
      expect(output.contains(expectedInOutput))
    }
  }

}
