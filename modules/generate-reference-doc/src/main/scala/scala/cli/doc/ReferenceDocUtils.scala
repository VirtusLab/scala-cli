package scala.cli.doc

import caseapp.HelpMessage

import java.util.stream.IntStream

import scala.build.internal.util.ConsoleUtils.*

object ReferenceDocUtils {
  extension (s: String) {
    def consoleToFence: String =
      s
        .linesIterator
        .fold("") { (acc, line) =>
          val maybeOpenFence =
            if line.contains(Console.BOLD) then
              """```sh
                |""".stripMargin
            else if line.contains(ScalaCliConsole.GRAY) then
              """```scala
                |""".stripMargin
            else ""
          val maybeCloseFence =
            if line.contains(Console.RESET) then
              """
                |```""".stripMargin
            else ""
          val newLine = s"$maybeOpenFence${line.noConsoleKeys}$maybeCloseFence"
          if acc.isEmpty then newLine
          else s"""$acc
                  |$newLine""".stripMargin
        }
  }
  extension (helpMessage: HelpMessage) {
    def referenceDocMessage: String = helpMessage.message.consoleToFence.noConsoleKeys
    def referenceDocDetailedMessage: String = {
      val msg =
        if helpMessage.detailedMessage.nonEmpty then helpMessage.detailedMessage
        else helpMessage.message
      msg.consoleToFence
    }
  }

}
