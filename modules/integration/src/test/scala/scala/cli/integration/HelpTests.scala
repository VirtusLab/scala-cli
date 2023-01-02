package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.HelpTests.HelpVariants

class HelpTests extends ScalaCliSuite {
  for { helpOptions <- HelpVariants } {
    val help              = os.proc(TestUtil.cli, helpOptions).call(check = false)
    val helpOutput        = help.out.trim()
    val helpOptionsString = helpOptions.mkString(" ")
    test(s"$helpOptionsString works correctly") {
      assert(
        help.exitCode == 0,
        clues(helpOptions, help.out.text(), help.err.text(), help.exitCode)
      )
      expect(helpOutput.contains("Usage:"))
    }

    test(s"$helpOptionsString output command groups are ordered correctly") {
      val mainCommandsIndex  = helpOutput.indexOf("Main commands:")
      val miscellaneousIndex = helpOutput.indexOf("Miscellaneous commands:")
      expect(mainCommandsIndex < miscellaneousIndex)
      expect(miscellaneousIndex < helpOutput.indexOf("Other commands:"))
    }

    test(s"$helpOptionsString output includes launcher options") {
      expect(helpOutput.contains("--power"))
    }
  }

}

object HelpTests {
  val HelpVariants =
    Seq(Seq("help"), Seq("help", "-help"), Seq("help", "--help"), Seq("-help"), Seq("--help"))
}
