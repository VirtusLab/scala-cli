package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File

trait DocSlothTestDefinitions { this: DocTestDefinitions & TestScalaVersion =>
  if actualScalaVersion.startsWith("3.") then {
    val docScalaVersion   = Constants.scala3Lts
    val slothOptions      = Seq("--sloth", "--suppress-experimental-feature-warning")
    val slothAgentOptions = Seq("--sloth-agent", "--suppress-experimental-feature-warning")
    val slothCacheSegment = s"${File.separator}sloth${File.separator}"

    def expectScaladocClasspathContains(output: String, fragment: String): Unit = {
      val marker       = "dotty.tools.scaladoc.Main -classpath "
      val classpathOpt = output.split(marker).lift(1).map(_.takeWhile(c => c != ' ' && c != '\n'))
      expect(classpathOpt.exists(_.contains(fragment)))
    }

    def lazyValProjFile: String =
      s"""//> using scala $docScalaVersion
         |
         |/** A sample with a lazy val. */
         |object Main {
         |  lazy val greeting: String = "Hello"
         |  def main(args: Array[String]): Unit = println(greeting)
         |}
         |""".stripMargin

    def runDoc(
      root: os.Path,
      extraArgs: Seq[String],
      dest: os.RelPath = os.rel / "doc-out"
    ): os.CommandResult =
      os.proc(
        TestUtil.cli,
        "--power",
        "doc",
        extraOptions,
        extraArgs,
        ".",
        "-o",
        dest,
        "-v"
      ).call(cwd = root, mergeErrIntoOut = true)

    test("doc --sloth patches the scaladoc classpath") {
      TestInputs(
        os.rel / "Main.scala" -> lazyValProjFile
      ).fromRoot { root =>
        val dest = os.rel / "doc-out"
        val r    = runDoc(root, slothOptions, dest)
        expect(r.exitCode == 0)
        expect(os.isDir(root / dest))
        expectScaladocClasspathContains(r.out.text(), slothCacheSegment)
      }
    }

    test("doc --sloth-agent attaches the sloth java agent to scaladoc") {
      TestInputs(
        os.rel / "Main.scala" -> lazyValProjFile
      ).fromRoot { root =>
        val dest = os.rel / "doc-out"
        val r    = runDoc(root, slothAgentOptions, dest)
        expect(r.exitCode == 0)
        expect(os.isDir(root / dest))
        expect(r.out.text().contains("-javaagent"))
      }
    }
  }
}
