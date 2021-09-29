package scala.cli.commands

import caseapp._
import coursier.cache.{CacheLogger, FileCache}

import scala.concurrent.duration.Duration

// format: off
final case class CoursierOptions(
  @Group("Dependency")
  @HelpMessage("Specify a TTL for changing dependencies, such as snapshots")
  @ValueDescription("duration|Inf")
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
  implicit val parser = Parser[CoursierOptions]
  implicit val help   = Help[CoursierOptions]
}
