package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.ProcOps
import scala.concurrent.duration.DurationInt
import scala.util.Properties

class RunTests3Lts extends RunTestDefinitions with Test3Lts {
  import Constants.scala3LtsPrefix
  for (ltsNightlyAlias <- List("lts.nightly", "3.lts.nightly"))
    test(s"Scala $ltsNightlyAlias & $scala3LtsPrefix.nightly point to the same version") {
      TestInputs.empty.fromRoot { root =>
        val version1      = getScalaVersion(ltsNightlyAlias, root)
        val nightlyPrefix = version1.split('.').take(2).mkString(".")
        expect(nightlyPrefix == Constants.scala3LtsPrefix)
        val nightlyPatch = version1.split('.').take(3).last.takeWhile(_.isDigit).toInt
        if nightlyPrefix == "3.3" then expect(nightlyPatch >= 8) // new nightly repo
        val version2 = getScalaVersion(s"${Constants.scala3LtsPrefix}.nightly", root)
        expect(version1 == version2)
      }
    }

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
