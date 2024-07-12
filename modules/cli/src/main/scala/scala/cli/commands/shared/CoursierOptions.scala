package scala.cli.commands.shared

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import coursier.cache.{CacheLogger, CachePolicy, FileCache}

import scala.build.Logger
import scala.build.internals.EnvVar
import scala.cli.commands.tags
import scala.cli.config.Keys
import scala.cli.util.ConfigDbUtils
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

  def coursierCache(cacheLogger: CacheLogger, cliLogger: Logger) = {
    var baseCache = FileCache().withLogger(cacheLogger)
    if (!validateChecksums)
      baseCache = baseCache.withChecksums(Nil)
    val ttlOpt = ttl.map(_.trim).filter(_.nonEmpty).map(Duration(_))
    for (ttl0 <- ttlOpt)
      baseCache = baseCache.withTtl(ttl0)
    for (loc <- cache.filter(_.trim.nonEmpty))
      baseCache = baseCache.withLocation(loc)
    for (isOffline <- getOffline(cliLogger) if isOffline)
      baseCache = baseCache.withCachePolicies(Seq(CachePolicy.LocalOnly))

    baseCache
  }

  def getOffline(logger: Logger): Option[Boolean] = offline
    .orElse(EnvVar.Coursier.coursierMode.valueOpt.map(_ == "offline"))
    .orElse(Option(System.getProperty("coursier.mode")).map(_ == "offline"))
    .orElse(ConfigDbUtils.getConfigDbOpt(logger).flatMap(_.get(Keys.offline).toOption.flatten))
}

object CoursierOptions {
  implicit lazy val parser: Parser[CoursierOptions]            = Parser.derive
  implicit lazy val help: Help[CoursierOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[CoursierOptions] = JsonCodecMaker.make
}
