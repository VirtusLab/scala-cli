package scala.cli.commands

import caseapp._

// format: off
final case class BenchmarkingOptions(
  @Group("Benchmarking")
  @HelpMessage("Run JMH benchmarks")
    jmh: Option[Boolean] = None,
  @Group("Benchmarking")
  @HelpMessage("Set JMH version")
  @ValueDescription("version")
    jmhVersion: Option[String] = None
)
// format: on

object BenchmarkingOptions {
  implicit lazy val parser                          = Parser[BenchmarkingOptions]
  implicit lazy val help: Help[BenchmarkingOptions] = Help.derive
}
