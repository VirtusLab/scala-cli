package scala.cli

import scala.cli.commands._
import scala.util.Properties

object ScalaCli {
  def main(args: Array[String]): Unit = {

    if (Properties.isWin && System.console() != null)
      // Enable ANSI output in Windows terminal
      coursier.jniutils.WindowsAnsiTerminal.enableAnsiOutput()

    args.headOption match {
      case Some("about") =>
        About.main(args.tail)
      case Some("compile") =>
        Compile.main(args.tail)
      case Some("repl") | Some("console") =>
        Repl.main(args.tail)
      case Some("package") =>
        Package.main(args.tail)
      case Some("run") =>
        Run.main(args.tail)
      case Some("test") =>
        Test.main(args.tail)
      case _ =>
        Default.main(args)
    }
  }
}
