package scala.build.input

/** Stores information about how the program has been evoked
  *
  * @param progName
  *   the actual Scala CLI program name which was run
  * @param subCommandName
  *   the name of the sub-command that was invoked by user
  * @param subCommand
  *   the type of the sub-command that was invoked by user
  * @param isShebangCapableShell
  *   does the host shell support shebang headers
  */

case class ScalaCliInvokeData(
  progName: String,
  subCommandName: String,
  subCommand: SubCommand,
  isShebangCapableShell: Boolean
) {

  /** [[progName]] with [[subCommandName]] if any */
  def invocationString: String =
    subCommand match
      case SubCommand.Default => progName
      case _                  => s"$progName $subCommandName"
}

enum SubCommand:
  case Default extends SubCommand
  case Shebang extends SubCommand
  case Other   extends SubCommand
