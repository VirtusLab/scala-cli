package scala.cli.commands

object WatchUtil {

  lazy val isDevMode: Boolean =
    Option(getClass.getProtectionDomain.getCodeSource)
      .exists(_.getLocation.toExternalForm.endsWith("classes/"))

  def waitMessage(message: String): String = {
    // Both short cuts actually always work, but Ctrl+C also exits mill in mill watch mode.
    val shortCut = if (isDevMode) "Ctrl+D" else "Ctrl+C"
    val gray     = "\u001b[90m"
    val reset    = Console.RESET
    s"$gray$message, press $shortCut to exit.$reset"
  }

  def printWatchMessage(): Unit =
    System.err.println(waitMessage("Watching sources"))

  def waitForCtrlC(onPressEnter: () => Unit = () => ()): Unit = {
    var readKey = -1
    while ({
      readKey = System.in.read()
      readKey != -1
    })
      if (readKey == '\n')
        onPressEnter()
  }

}
