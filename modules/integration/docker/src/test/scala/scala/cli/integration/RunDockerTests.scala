package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

class RunDockerTests extends munit.FunSuite {

  lazy val imageName = Option(System.getenv("SCALA_CLI_IMAGE")).getOrElse {
    sys.error("SCALA_CLI_IMAGE not set")
  }
  lazy val termOpt = if (System.console() == null) Nil else Seq("-t")
  lazy val ciOpt   = Option(System.getenv("CI")).map(v => Seq("-e", s"CI=$v")).getOrElse(Nil)
  lazy val slimScalaCliImage = "scala-cli-slim"

  test("run simple app in in docker") {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""val msg = "$message"
           |println(msg)
           |""".stripMargin
    )
    inputs.fromRoot { root =>
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

  if (!imageName.contains(slimScalaCliImage))
    test("package simple app with native in docker") {
      val fileName = "simple.sc"
      val inputs = TestInputs(
        os.rel / fileName -> """println("Hello")"""
      )
      inputs.fromRoot { root =>
        val cmdPackage = Seq[os.Shellable](
          // format: off
          "docker", "run", "--rm", termOpt, "-v", s"$root:/data", "-w", "/data", ciOpt,
          imageName, "--power", "package", "--native", fileName, "-o", "Hello"
          // format: on
        )
        val procPackage = os.proc(cmdPackage).call(cwd = root, check = false)
        expect(procPackage.exitCode == 0)
      }
    }
}
