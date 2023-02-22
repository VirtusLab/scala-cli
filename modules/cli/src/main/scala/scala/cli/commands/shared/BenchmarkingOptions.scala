package scala.cli.commands.shared

import caseapp.*

import scala.cli.commands.tags

// format: off
final case class BenchmarkingOptions(
  @Group(HelpGroup.Benchmarking.toString)
  @Tag(tags.experimental)
  @HelpMessage("Run JMH benchmarks")
    jmh: Option[Boolean] = None,
  @Group(HelpGroup.Benchmarking.toString)
  @Tag(tags.experimental)
  @HelpMessage("Set JMH version")
  @ValueDescription("version")
    jmhVersion: Option[String] = None
)
// format: on

object BenchmarkingOptions {
  implicit lazy val parser: Parser[BenchmarkingOptions] = Parser.derive
  implicit lazy val help: Help[BenchmarkingOptions]     = Help.derive
}
