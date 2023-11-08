package scala.cli.commands.shared

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import coursier.cache.{CacheLogger, CachePolicy, FileCache}

import scala.cli.commands.tags
import scala.concurrent.duration.Duration

// format: off
final case class CoursierOptions(
  @Group(HelpGroup.Dependency.toString)
  @HelpMessage("Specify a TTL for changing dependencies, such as snapshots")
  @ValueDescription("duration|Inf")
  @Tag(tags.implementation)
  @Hidden
    ttl: Option[String] = None,
  @Group(HelpGroup.Dependency.toString)
  @HelpMessage("Set the coursier cache location")
  @ValueDescription("path")
  @Tag(tags.implementation)
  @Hidden
    cache: Option[String] = None,
  @Group(HelpGroup.Dependency.toString)
  @HelpMessage("Enable checksum validation of artifacts downloaded by coursier")
  @Tag(tags.implementation)
  @Hidden
    coursierValidateChecksums: Option[Boolean] = None,

  @Group(HelpGroup.Dependency.toString)
  @HelpMessage("Disable using the network to download artifacts, use the local cache only")
  @Tag(tags.experimental)
    offline: Option[Boolean] = None
) {
  // format: on

  private def validateChecksums =
    coursierValidateChecksums.getOrElse(true)

  def coursierCache(logger: CacheLogger) = {
    var baseCache = FileCache().withLogger(logger)
    if (!validateChecksums)
      baseCache = baseCache.withChecksums(Nil)
    val ttlOpt = ttl.map(_.trim).filter(_.nonEmpty).map(Duration(_))
    for (ttl0 <- ttlOpt)
      baseCache = baseCache.withTtl(ttl0)
    for (loc <- cache.filter(_.trim.nonEmpty))
      baseCache = baseCache.withLocation(loc)
    for (isOffline <- getOffline() if isOffline)
      baseCache = baseCache.withCachePolicies(Seq(CachePolicy.LocalOnly))

    baseCache
  }

  def getOffline(): Option[Boolean] = offline
    .orElse(Option(System.getenv("COURSIER_MODE")).map(_ == "offline"))
    .orElse(Option(System.getProperty("coursier.mode")).map(_ == "offline"))
}

object CoursierOptions {
  implicit lazy val parser: Parser[CoursierOptions]            = Parser.derive
  implicit lazy val help: Help[CoursierOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[CoursierOptions] = JsonCodecMaker.make
}
