package scala.cli.integration

trait JmhSuite { _: ScalaCliSuite =>
  protected def simpleBenchmarkingInputs(directivesString: String = ""): TestInputs = TestInputs(
    os.rel / "benchmark.scala" ->
      s"""$directivesString
         |package bench
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
  protected lazy val expectedInBenchmarkingOutput = """Result "bench.Benchmarks.foo":"""
  protected lazy val exampleOldJmhVersion         = "1.29"
}
