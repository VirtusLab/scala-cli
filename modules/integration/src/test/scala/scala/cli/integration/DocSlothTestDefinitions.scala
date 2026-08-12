package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

trait DocSlothTestDefinitions extends LazyValTests {
  this: DocTestDefinitions & TestScalaVersion =>
  protected def lazyValProjFile: String =
    s"""/** A sample with a lazy val. */
       |object Main {
       |  lazy val greeting: String = "Hello"
       |  def main(args: Array[String]): Unit = println(greeting)
       |}
       |""".stripMargin

  if actualScalaVersion.startsWith("3") then {
    val scaladocClasspathTestName =
      if isScala38OrNewer then "doc --sloth leaves the scaladoc classpath as is when unnecessary"
      else "doc --sloth patches the scaladoc classpath"
    test(scaladocClasspathTestName) {
      TestInputs(
        os.rel / "Main.scala" -> lazyValProjFile
      ).fromRoot { root =>
        val dest = os.rel / "doc-out"
        val r    =
          os.proc(TestUtil.cli, "--power", "doc", extraOptions, slothOptions, ".", "-o", dest, "-v")
            .call(cwd = root, mergeErrIntoOut = true)
        expect(r.exitCode == 0)
        expect(os.isDir(root / dest))
        expectScaladocClasspathContains(
          r.out.text(),
          slothCacheSegment,
          shouldContain = !isScala38OrNewer
        )
      }
    }

    test("doc --sloth-agent attaches the sloth java agent to scaladoc") {
      TestInputs(
        os.rel / "Main.scala" -> lazyValProjFile
      ).fromRoot { root =>
        val dest = os.rel / "doc-out"
        val r    =
          os.proc(
            TestUtil.cli,
            "--power",
            "doc",
            extraOptions,
            slothAgentOptions,
            ".",
            "-o",
            dest,
            "-v"
          ).call(cwd = root, mergeErrIntoOut = true)
        expect(r.exitCode == 0)
        expect(os.isDir(root / dest))
        expect(r.out.text().contains("-javaagent"))
      }
    }

    test(s"doc --sloth patches external -cp class directory with ${Constants.scala3Lts} lazy vals") {
      TestInputs(
        externalLazyValsInput(),
        os.rel / "project" / "Main.scala" ->
          """/** Docs that reference an external lazy val. */
            |object Main {
            |  def value: Boolean = slothful
            |}
            |""".stripMargin
      ).fromRoot { root =>
        val (classDir, _) =
          compileExternalLazyValClassDir(workspace = root, scalaVersion = Constants.scala3Lts)
        val dest = os.rel / "doc-out"
        val r    = os.proc(
          TestUtil.cli,
          "--power",
          "doc",
          "--server=false",
          "-cp",
          classDir.toString,
          slothOptions,
          "project",
          "-o",
          dest,
          "-v",
          extraOptions
        ).call(cwd = root, mergeErrIntoOut = true)
        expect(r.exitCode == 0)
        expect(os.isDir(root / dest))
        expectScaladocClasspathContains(r.out.text(), "external-dirs")
      }
    }
  }
}
