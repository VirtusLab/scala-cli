package scala.cli.commands

object WatchUtil {

  private lazy val isDevMode: Boolean =
    Option(getClass.getProtectionDomain.getCodeSource)
      .exists(_.getLocation.toExternalForm.endsWith("classes/"))

  def printWatchMessage(): Unit = {
    // Both short cuts actually always work, but Ctrl+C also exits mill in mill watch mode.
    val shortCut = if (isDevMode) "Ctrl+D" else "Ctrl+C"
    val gray = "\u001b[90m"
    val reset = Console.RESET
    System.err.println(s"${gray}Watching sources, press $shortCut to exit.$reset")
  }

  def waitForCtrlC(): Unit =
    while (System.in.read() != -1) {}

}
