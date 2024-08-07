package scala.cli.commands

import scala.annotation.tailrec
import scala.build.internal.StdInConcurrentReader
import scala.build.internals.ConsoleUtils.ScalaCliConsole

object WatchUtil {

  lazy val isDevMode: Boolean =
    Option(getClass.getProtectionDomain.getCodeSource)
      .exists(_.getLocation.toExternalForm.endsWith("classes/"))

  def waitMessage(message: String): String = {
    // Both short cuts actually always work, but Ctrl+C also exits mill in mill watch mode.
    val shortCut = if (isDevMode) "Ctrl+D" else "Ctrl+C"
    gray(s"$message, press $shortCut to exit, or press Enter to re-run.")
  }

  private def gray(message: String): String = {
    val gray  = ScalaCliConsole.GRAY
    val reset = Console.RESET
    s"$gray$message$reset"
  }

  def printWatchMessage(): Unit =
    System.err.println(waitMessage("Watching sources"))

  def printWatchWhileRunningMessage(): Unit =
    System.err.println(gray("Watching sources while your program is running."))

  def waitForCtrlC(
    onPressEnter: () => Unit = () => (),
    shouldReadInput: () => Boolean = () => true
  ): Unit = synchronized {

    @tailrec
    def readNextChar(): Int =
      if (shouldReadInput())
        try StdInConcurrentReader.waitForLine().map(s => (s + '\n').head.toInt).getOrElse(-1)
        catch {
          case _: InterruptedException =>
            // Actually never called, as System.in.read isn't interruptible…
            // That means we sometimes read input when we shouldn't.
            readNextChar()
        }
      else {
        try wait()
        catch {
          case _: InterruptedException =>
        }
        readNextChar()
      }

    var readKey = -1
    while ({
      readKey = readNextChar()
      readKey != -1
    })
      if (readKey == '\n')
        onPressEnter()
  }

}
