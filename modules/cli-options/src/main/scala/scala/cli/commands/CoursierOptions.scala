package scala.cli.commands

import caseapp._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

// format: off
final case class CoursierOptions(
  @Group("Dependency")
  @HelpMessage("Specify a TTL for changing dependencies, such as snapshots")
  @ValueDescription("duration|Inf")
  @Hidden
    ttl: Option[String] = None,
  @Group("Dependency")
  @HelpMessage("Set the coursier cache location")
  @ValueDescription("path")
  @Hidden
    cache: Option[String] = None
)
// format: on

object CoursierOptions {
  lazy val parser: Parser[CoursierOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[CoursierOptions, parser.D] = parser
  implicit lazy val help: Help[CoursierOptions]                      = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[CoursierOptions]       = JsonCodecMaker.make
}
