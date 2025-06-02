package scala.cli.commands.shared

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

object ArgSplitter {
  def splitToArgs(input: String) = {
    val iter        = input.iterator
    val accumulator = new ListBuffer[String]

    @tailrec
    def takeWhile(
      test: Char => Boolean,
      acc: List[Char] = Nil,
      prevWasEscape: Boolean = false
    ): String =
      iter.nextOption() match
        case Some(c) if !prevWasEscape && test(c) => acc.reverse.mkString
        case None                                 => acc.reverse.mkString
        case Some('\\')                           =>
          takeWhile(test, '\\' :: acc, prevWasEscape = true)
        case Some(c) =>
          takeWhile(test, c :: acc, prevWasEscape = false)

    while (iter.hasNext)
      iter.next() match
        case c if c.isSpaceChar || c == '\n' || c == '\r' =>
        case c @ ('\'' | '"') => accumulator += s"$c${takeWhile(_ == c)}$c"
        case c                =>
          accumulator += s"$c${takeWhile(c => c.isSpaceChar || c == '\n' || c == '\r')}"

    accumulator.result()
  }

}
