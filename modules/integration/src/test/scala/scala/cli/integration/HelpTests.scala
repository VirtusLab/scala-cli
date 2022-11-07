package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class HelpTests extends ScalaCliSuite {
  test("help command") {
    for { helpOption <- Seq("help", "-help", "--help") } {
      val help = os.proc(TestUtil.cli, helpOption).call(check = false)
      assert(help.exitCode == 0, clues(helpOption, help.out.text(), help.err.text(), help.exitCode))
      expect(help.out.text().contains("Usage:"))
    }
  }
}
