package cli.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.commands.version.Version
import scala.cli.{ScalaCli, ScalaCliCommands}

class HelpCheck extends munit.FunSuite {

  test("help message should be shorter then 80 lines") {
    val scalaCli = new ScalaCliCommands("scala-cli", "scala-cli", "Scala CLI", isSipScala = false)
    val helpMessage = scalaCli.help.help(scalaCli.helpFormat)

    val lines = helpMessage.split("\r\n|\r|\n").length
    assert(lines <= 80)
  }

  test("version help message should only contain relevant options") { // regression test - https://github.com/VirtusLab/scala-cli/issues/1666
    val helpMessage = Version.finalHelp.help(Version.helpFormat)

    expect(helpMessage.contains("Version options:"))

    expect(!helpMessage.contains("-help-full"))
    expect(!helpMessage.contains("Logging options:"))
    expect(!helpMessage.contains("Other options:"))
  }
}
