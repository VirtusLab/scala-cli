package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

class RunDockerTests extends munit.FunSuite {

  lazy val imageName = Option(System.getenv("SCALA_CLI_IMAGE")).getOrElse {
    sys.error("SCALA_CLI_IMAGE not set")
  }

  test("run simple app in in docker") {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / fileName ->
          s"""val msg = "$message"
             |println(msg)
             |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val termOpt   = if (System.console() == null) Nil else Seq("-t")
      val ciOpt     = Option(System.getenv("CI")).map(v => Seq("-e", s"CI=$v")).getOrElse(Nil)
      val rawOutput = new ByteArrayOutputStream
      val cmd = Seq[os.Shellable](
        // format: off
        "docker", "run", "--rm", termOpt, "-v", s"$root:/data", "-w", "/data", ciOpt,
        imageName, fileName
        // format: on
      )
      os.proc(cmd).call(
        cwd = root,
        stdout = os.ProcessOutput { (b, len) =>
          rawOutput.write(b, 0, len)
          System.err.write(b, 0, len)
        },
        mergeErrIntoOut = true
      )
      val output = new String(rawOutput.toByteArray, Charset.defaultCharset())
      expect(output.linesIterator.toVector.last == message)
    }
  }
}
