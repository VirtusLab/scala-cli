package cli.tests

import scala.cli.commands.shared.ArgSplitter

class ArgSplitterTest extends TestUtil.ScalaCliSuite {
  test("test scalac options are split correctly") {
    val args = List(
      List("-arg", "-other-arg"),
      List("-yet-another-arg", "dir/path\\ with\\ space", "'another arg with space'"),
      List("\"yet another arg with space\"")
    )
    val input = args.map(_.mkString("   ", " ", "")).mkString(" ", "\n", "")
    assertEquals(ArgSplitter.splitToArgs(input), args.flatten)
  }
}
