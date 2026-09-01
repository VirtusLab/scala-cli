package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.ProcOps
import scala.concurrent.duration.DurationInt
import scala.util.Properties

class RunTests3Lts extends RunTestDefinitions with RunLtsTagTestDefinitions with Test3Lts {
  if (!Properties.isMac || !TestUtil.isCI)
    test(s"--sloth works correctly under --watch mode") {
      val expectedMessage1 = "Hello from lazy val"
      val expectedMessage2 = "Updated lazy val"
      val inputPath        = os.rel / "Main.scala"

      def code(msg: String) =
        s"""object Main {
           |  lazy val greeting: String = "$msg"
           |  def main(args: Array[String]): Unit = println(greeting)
           |}
           |""".stripMargin

      TestInputs(inputPath -> code(expectedMessage1)).fromRoot { root =>
        TestUtil.withProcessWatching(
          proc = os.proc(
            TestUtil.cli,
            "--power",
            "run",
            ".",
            "--sloth",
            "--jvm",
            latestJava,
            "--watch",
            extraOptions
          ).spawn(cwd = root, stderr = os.Pipe),
          timeout = 120.seconds
        ) { (proc, timeout, ec) =>
          val output1 = TestUtil.readLine(proc.stdout, ec, timeout)
          expect(output1 == expectedMessage1)
          proc.printStderrUntilRerun(timeout)(ec)
          os.write.over(root / inputPath, code(expectedMessage2))
          val output2 = TestUtil.readLine(proc.stdout, ec, timeout)
          expect(output2 == expectedMessage2)
        }
      }
    }
}
