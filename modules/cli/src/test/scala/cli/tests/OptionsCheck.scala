package scala.cli.tests

import scala.cli.ScalaCliCommands

class OptionsCheck extends munit.FunSuite {

  private val helper = new ScalaCliCommands("scala-cli", isSipScala = false)
  val commands = helper.defaultCommand.toSeq ++ helper.commands

  for (command <- commands)
    test(s"No duplicated options in ${command.names.head.mkString(" ")}") {
      command.ensureNoDuplicates()
    }

}
