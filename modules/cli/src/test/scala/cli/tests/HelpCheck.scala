package cli.tests

import scala.cli.ScalaCliCommands

class HelpCheck extends munit.FunSuite {

  test("help message should be shorter then 80 lines") {
    val scalaCli = new ScalaCliCommands("scala-cli", "scala-cli", "Scala CLI", isSipScala = false)
    val helpMessage = scalaCli.help.help(scalaCli.helpFormat)

    val lines = helpMessage.split("\r\n|\r|\n").length
    assert(lines <= 80)
  }
}
