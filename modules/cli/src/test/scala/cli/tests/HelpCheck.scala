package cli.tests

import scala.cli.ScalaCli

class HelpCheck extends munit.FunSuite {

  test("help message should be shorter then 80 lines") {
    val helpMessage = ScalaCli.help.help(ScalaCli.helpFormat)

    val lines = helpMessage.split("\r\n|\r|\n").length
    assert(lines <= 80)
  }
}
