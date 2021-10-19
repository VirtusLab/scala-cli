package scala.cli.commands

import caseapp._
import coursier.cache.{CacheLogger, FileCache}
import upickle.default.{ReadWriter, macroRW}

import scala.concurrent.duration.Duration

// format: off
final case class CoursierOptions(
  @Group("Dependency")
  @HelpMessage("Specify a TTL for changing dependencies, such as snapshots")
  @ValueDescription("duration|Inf")
  @Hidden
    ttl: Option[String] = None
) {
  // format: on
  def coursierCache(logger: CacheLogger) = {
    val baseCache = FileCache()
    val ttl0      = ttl.map(_.trim).filter(_.nonEmpty).map(Duration(_)).orElse(baseCache.ttl)
    baseCache
      .withTtl(ttl0)
      .withLogger(logger)
  }
}

object CoursierOptions {
  lazy val parser: Parser[CoursierOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[CoursierOptions, parser.D] = parser
  implicit lazy val help: Help[CoursierOptions]                      = Help.derive
  implicit lazy val jsonCodec: ReadWriter[CoursierOptions]           = macroRW
}
