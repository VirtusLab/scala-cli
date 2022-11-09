package scala.cli.commands.shared

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.cli.commands.tags

// format: off
final case class CoursierOptions(
  @Group("Dependency")
  @HelpMessage("Specify a TTL for changing dependencies, such as snapshots")
  @ValueDescription("duration|Inf")
  @Tag(tags.implementation)
  @Hidden
    ttl: Option[String] = None,
  @Group("Dependency")
  @HelpMessage("Set the coursier cache location")
  @ValueDescription("path")
  @Tag(tags.implementation)
  @Hidden
    cache: Option[String] = None,
  @Group("Dependency")
  @HelpMessage("Enable checksum validation of artifacts downloaded by coursier")
  @Tag(tags.implementation)
  @Hidden
    coursierValidateChecksums: Option[Boolean] = None
)
// format: on

object CoursierOptions {
  implicit lazy val parser: Parser[CoursierOptions]            = Parser.derive
  implicit lazy val help: Help[CoursierOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[CoursierOptions] = JsonCodecMaker.make
}
