package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class HelpTests extends ScalaCliSuite {
  for (helpOptions <- HelpTests.variants) {
    lazy val help         = os.proc(TestUtil.cli, helpOptions).call(check = false)
    lazy val helpOutput   = help.out.trim()
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

    test(s"$helpOptionsString output does not include legacy scala runner options") {
      expect(!helpOutput.contains("Legacy Scala runner options"))
    }

    test(s"$helpOptionsString output includes external help options") {
      expect(helpOutput.contains("--scalac-help"))
      expect(helpOutput.contains("--help-js"))
      expect(helpOutput.contains("--help-native"))
      expect(helpOutput.contains("--help-doc"))
      expect(helpOutput.contains("--help-repl"))
      expect(helpOutput.contains("--help-fmt"))
    }
  }

  for (fullHelpOptions <- HelpTests.fullHelpVariants) {
    lazy val fullHelp         = os.proc(TestUtil.cli, fullHelpOptions).call(check = false)
    lazy val fullHelpOutput   = fullHelp.out.trim()
    val fullHelpOptionsString = fullHelpOptions.mkString(" ")
    test(s"$fullHelpOptionsString works correctly") {
      assert(
        fullHelp.exitCode == 0,
        clues(fullHelpOptions, fullHelp.out.text(), fullHelp.err.text(), fullHelp.exitCode)
      )
      expect(fullHelpOutput.contains("Usage:"))
    }
    test(s"$fullHelpOptionsString output includes legacy scala runner options") {
      expect(fullHelpOutput.contains("Legacy Scala runner options"))
    }
  }

  test("name aliases limited for standard help") {
    val help       = os.proc(TestUtil.cli, "run", "--help").call()
    val helpOutput = help.out.trim()

    expect(TestUtil.removeAnsiColors(helpOutput).contains(
      "--jar, --extra-jars paths"
    ))
  }

  test("name aliases not limited for full help") {
    val help       = os.proc(TestUtil.cli, "run", "--full-help").call()
    val helpOutput = help.out.trim()
    expect(TestUtil.removeAnsiColors(helpOutput).contains(
      "-cp, --jar, --jars, --class, --classes, -classpath, --extra-jar, --classpath, --extra-jars, --class-path, --extra-class, --extra-classes, --extra-class-path paths"
    ))
  }
  for (withPower <- Seq(true, false))
    test("envs help" + (if (withPower) " with power" else "")) {
      val powerOptions = if (withPower) Seq("--power") else Nil
      val help         = os.proc(TestUtil.cli, "--envs-help", powerOptions).call()
      val helpOutput   = help.out.trim()
      if (!withPower) expect(!helpOutput.contains("(power)"))
      expect(helpOutput.nonEmpty)
      expect(helpOutput.contains("environment variables"))
    }
}

object HelpTests {
  val variants =
    Seq(
      Seq("help"),
      Seq("help", "-help"),
      Seq("help", "--help"),
      Seq("-help"),
      Seq("--help")
    )
  val fullHelpVariants =
    Seq(
      Seq("help", "--full-help"),
      Seq("help", "-full-help"),
      Seq("help", "--help-full"),
      Seq("help", "-help-full"),
      Seq("--full-help"),
      Seq("-full-help"),
      Seq("-help-full")
    )
}
