package scala.build.input

/** Stores information about how the program has been evoked
  *
  * @param progName
  *   the actual Scala CLI program name which was run
  * @param subCommand
  *   the sub-command that was invoked by user
  */

case class ScalaCliInvokeData(
  progName: String,
  subCommandName: String,
  subCommand: SubCommand,
  isShebangCabaleShell: Boolean
)

enum SubCommand:
  case Default extends SubCommand
  case Shebang extends SubCommand
  case Other   extends SubCommand
