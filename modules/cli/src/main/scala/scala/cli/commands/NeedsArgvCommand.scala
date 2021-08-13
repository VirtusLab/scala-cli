package scala.cli.commands

trait NeedsArgvCommand {
  def setArgv(argv: Array[String]): Unit
}
