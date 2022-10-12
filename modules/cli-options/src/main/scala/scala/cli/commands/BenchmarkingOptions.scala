package scala.cli.commands

import caseapp.*

// format: off
final case class BenchmarkingOptions(
  @Group("Benchmarking")
  @Tag(tags.experimental)
  @HelpMessage("Run JMH benchmarks")
    jmh: Option[Boolean] = None,
  @Group("Benchmarking")
  @Tag(tags.experimental)
  @HelpMessage("Set JMH version")
  @ValueDescription("version")
    jmhVersion: Option[String] = None
)
// format: on

object BenchmarkingOptions {
  lazy val parser: Parser[BenchmarkingOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[BenchmarkingOptions, parser.D] = parser
  implicit lazy val help: Help[BenchmarkingOptions]                      = Help.derive
}
