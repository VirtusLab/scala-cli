package scala.cli.doc

import caseapp.HelpMessage

import scala.cli.commands.util.ConsoleUtils.*

object ReferenceDocUtils {
  extension (helpMessage: HelpMessage) {
    def referenceDocMessage: String = helpMessage.message.noConsoleKeys
  }

}
