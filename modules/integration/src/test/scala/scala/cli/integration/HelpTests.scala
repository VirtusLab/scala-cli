package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class HelpTests extends ScalaCliSuite {
  for { helpOption <- Seq("help", "-help", "--help") } {
    val help       = os.proc(TestUtil.cli, helpOption).call(check = false)
    val helpOutput = help.out.trim()
    test(s"$helpOption works correctly") {
      assert(help.exitCode == 0, clues(helpOption, help.out.text(), help.err.text(), help.exitCode))
      expect(helpOutput.contains("Usage:"))
    }

    test(s"$helpOption output command groups are ordered correctly") {
      val mainCommandsIndex  = helpOutput.indexOf("Main commands:")
      val miscellaneousIndex = helpOutput.indexOf("Miscellaneous commands:")
      expect(mainCommandsIndex < miscellaneousIndex)
      expect(miscellaneousIndex < helpOutput.indexOf("Other commands:"))
    }
  }

}
