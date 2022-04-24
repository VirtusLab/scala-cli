package scala.build.preprocessing.directives

import scala.cli.commands.SharedOptions

object SharedOptionsDirectiveHandler extends PrefixedDirectiveGroup[Shar]("", "", SharedOptions.help) {
  val p
  
}
