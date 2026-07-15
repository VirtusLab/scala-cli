package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File

trait FixSlothTestDefinitions { this: FixTestDefinitions & TestScalaVersion =>
  if actualScalaVersion.startsWith("3.") then {
    val fixScalaVersion     = Constants.scala3Lts
    val slothOptions        = Seq("--sloth")
    val slothAgentOptions   = Seq("--sloth-agent")
    val slothNoOpWarnPrefix = "Sloth patching is not applicable to"
    val slothCacheSegment   = s"${File.separator}sloth${File.separator}"

    def expectScalafixClasspathContains(output: String, fragment: String): Unit = {
      val cpOpt = output.split("--classpath ").lift(1).map(_.takeWhile(c => c != ' ' && c != '\n'))
      expect(cpOpt.exists(_.contains(fragment)))
    }

    def lazyValProjFile: String =
      s"""//> using scala $fixScalaVersion
         |
         |object Main {
         |  lazy val greeting: String = "Hello"
         |  def main(args: Array[String]): Unit = println(greeting)
         |}
         |""".stripMargin

    def fixInputs: TestInputs =
      TestInputs(
        os.rel / "Main.scala"     -> lazyValProjFile,
        os.rel / ".scalafix.conf" ->
          """rules = [
            |  RedundantSyntax
            |]
            |""".stripMargin
      )

    def runFix(
      root: os.Path,
      extraArgs: Seq[String],
      mergeErrIntoOut: Boolean = true
    ): os.CommandResult =
      os.proc(
        TestUtil.cli,
        "--power",
        "fix",
        extraOptions,
        extraArgs,
        ".",
        "-v"
      ).call(cwd = root, mergeErrIntoOut = mergeErrIntoOut)

    test("fix --sloth patches the scalafix classpath") {
      fixInputs.fromRoot { root =>
        val r = runFix(root, slothOptions)
        expect(r.exitCode == 0)
        expectScalafixClasspathContains(r.out.text(), slothCacheSegment)
      }
    }

    test("fix --sloth-agent attaches the sloth java agent to scalafix") {
      fixInputs.fromRoot { root =>
        val r = runFix(root, slothAgentOptions)
        expect(r.exitCode == 0)
        expect(r.out.text().contains("-javaagent"))
      }
    }

    test("fix --sloth --enable-scalafix=false warns it is not applicable") {
      fixInputs.fromRoot { root =>
        val r = os.proc(
          TestUtil.cli,
          "--power",
          "fix",
          extraOptions,
          slothOptions,
          "--enable-scalafix=false",
          "."
        ).call(cwd = root, mergeErrIntoOut = true)
        expect(r.out.text().contains(slothNoOpWarnPrefix))
      }
    }
  }
}
