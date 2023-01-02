package scala.cli.commands.util

import caseapp.core.help.{Help, HelpFormat}

object HelpUtils {
  extension (help: Help[_]) {
    private def abstractHelp(
      format: HelpFormat,
      showHidden: Boolean
    )(f: (StringBuilder, HelpFormat, Boolean) => Unit): String = {
      val b = new StringBuilder
      f(b, format, showHidden)
      b.result()
    }

    def optionsHelp(format: HelpFormat, showHidden: Boolean = false): String =
      abstractHelp(format, showHidden)(help.printOptions)
  }
}
