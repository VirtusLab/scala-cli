package scala.cli.commands.shared

import caseapp.*
import caseapp.core.help.Help
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

@HelpMessage("Print help message")
// this is an aggregate for all external and internal help options
case class AllExternalHelpOptions(
  @Recurse
  scalacExtra: ScalacExtraOptions = ScalacExtraOptions(),
  @Recurse
  helpGroups: HelpGroupOptions = HelpGroupOptions()
)

object AllExternalHelpOptions {
  implicit lazy val parser: Parser[AllExternalHelpOptions]            = Parser.derive
  implicit lazy val help: Help[AllExternalHelpOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[AllExternalHelpOptions] = JsonCodecMaker.make
}
