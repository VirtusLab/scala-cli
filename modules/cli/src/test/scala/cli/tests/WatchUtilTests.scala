package cli.tests

import com.eed3si9n.expecty.Expecty.expect
import munit.FunSuite
import scala.cli.commands.WatchUtil
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class WatchUtilTests extends FunSuite {

  test("clearScreen prints correct ANSI escape codes") {
    val out    = new ByteArrayOutputStream()
    val ps     = new PrintStream(out)
    val oldOut = System.out
    try {
      System.setOut(ps)
      WatchUtil.clearScreen()
      ps.flush()
      val output = out.toString()
      expect(output == "\u001b[2J\u001b[H")
    }
    finally
      System.setOut(oldOut)
  }

  test("printWatchMessage prints to stderr") {
    val err    = new ByteArrayOutputStream()
    val ps     = new PrintStream(err)
    val oldErr = System.err
    try {
      System.setErr(ps)
      WatchUtil.printWatchMessage()
      ps.flush()
      val output = err.toString()
      expect(output.contains("Watching sources"))
    }
    finally
      System.setErr(oldErr)
  }

  test("printWatchWhileRunningMessage prints to stderr") {
    val err    = new ByteArrayOutputStream()
    val ps     = new PrintStream(err)
    val oldErr = System.err
    try {
      System.setErr(ps)
      WatchUtil.printWatchWhileRunningMessage()
      ps.flush()
      val output = err.toString()
      expect(output.contains("Watching sources"))
    }
    finally
      System.setErr(oldErr)
  }
}
