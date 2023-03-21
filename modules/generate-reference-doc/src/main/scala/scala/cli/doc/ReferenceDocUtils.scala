package scala.cli.doc

import caseapp.HelpMessage

import java.util.stream.IntStream

import scala.annotation.tailrec
import scala.build.internal.util.ConsoleUtils.*

object ReferenceDocUtils {
  extension (s: String) {
    def consoleToFence: String = {
      @tailrec
      def consoleToFenceRec(
        remainingLines: Seq[String],
        fenceOpen: Boolean = false,
        acc: String = ""
      ): String =
        remainingLines.headOption match
          case None => acc
          case Some(line) =>
            val openFenceString =
              if line.contains(Console.BOLD) then
                """```sh
                  |""".stripMargin
              else if line.contains(ScalaCliConsole.GRAY) then
                """```scala
                  |""".stripMargin
              else ""
            val currentFenceOpen = fenceOpen || openFenceString.nonEmpty
            val closeFenceString =
              if currentFenceOpen && line.contains(Console.RESET) then
                """
                  |```""".stripMargin
              else ""
            val newFenceOpen = currentFenceOpen && closeFenceString.isEmpty
            val newLine      = s"$openFenceString${line.noConsoleKeys}$closeFenceString"
            val newAcc =
              if acc.isEmpty then newLine
              else
                s"""$acc
                   |$newLine""".stripMargin
            consoleToFenceRec(remainingLines.tail, newFenceOpen, newAcc)
      consoleToFenceRec(s.linesIterator.toSeq)
    }
    def filterOutHiddenStrings: String =
      s
        .replace(s"${ScalaCliConsole.GRAY}(hidden)${Console.RESET} ", "")
        .replace(s"${ScalaCliConsole.GRAY}(experimental)${Console.RESET} ", "")
        .replace(s"${ScalaCliConsole.GRAY}(power)${Console.RESET} ", "")
    def consoleYellowToMdBullets: String = s.replace(Console.YELLOW, "- ")
    def consoleToMarkdown: String = s.filterOutHiddenStrings.consoleYellowToMdBullets.consoleToFence
  }
  extension (helpMessage: HelpMessage) {
    def referenceDocMessage: String = helpMessage.message.consoleToMarkdown
    def referenceDocDetailedMessage: String = {
      val msg =
        if helpMessage.detailedMessage.nonEmpty then helpMessage.detailedMessage
        else helpMessage.message
      msg.consoleToMarkdown
    }
  }

}
