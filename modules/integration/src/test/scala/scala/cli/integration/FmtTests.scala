package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class FmtTests extends ScalaCliSuite {

  override def group = ScalaCliSuite.TestGroup.First

  val confFileName = ".scalafmt.conf"

  val emptyInputs: TestInputs = TestInputs(os.rel / ".placeholder" -> "")

  val simpleInputsUnformattedContent: String =
    """package foo
      |
      |    object Foo       extends       java.lang.Object  {
      |                     def           get()             = 2
      | }
      |""".stripMargin
  val simpleInputs: TestInputs = TestInputs(
    os.rel / confFileName ->
      s"""|version = "${Constants.defaultScalafmtVersion}"
          |runner.dialect = scala213
          |""".stripMargin,
    os.rel / "Foo.scala" -> simpleInputsUnformattedContent
  )
  val expectedSimpleInputsFormattedContent: String = noCrLf {
    """package foo
      |
      |object Foo extends java.lang.Object {
      |  def get() = 2
      |}
      |""".stripMargin
  }

  val simpleInputsWithFilter: TestInputs = TestInputs(
    os.rel / confFileName ->
      s"""|version = "${Constants.defaultScalafmtVersion}"
          |runner.dialect = scala213
          |project.excludePaths = [ "glob:**/should/not/format/**.scala" ]
          |""".stripMargin,
    os.rel / "Foo.scala"                 -> expectedSimpleInputsFormattedContent,
    os.rel / "scripts" / "SomeScript.sc" -> "println()\n",
    os.rel / "should" / "not" / "format" / "ShouldNotFormat.scala" -> simpleInputsUnformattedContent
  )

  val simpleInputsWithDialectOnly: TestInputs = TestInputs(
    os.rel / confFileName -> "runner.dialect = scala213".stripMargin,
    os.rel / "Foo.scala"  -> simpleInputsUnformattedContent
  )

  val simpleInputsWithVersionOnly: TestInputs = TestInputs(
    os.rel / confFileName -> "version = \"3.5.5\"".stripMargin,
    os.rel / "Foo.scala"  -> simpleInputsUnformattedContent
  )

  private def noCrLf(input: String): String =
    input.replaceAll("\r\n", "\n")

  test("simple") {
    simpleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "fmt", ".").call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("no inputs") {
    simpleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "fmt").call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("with --check") {
    simpleInputs.fromRoot { root =>
      val out = os.proc(TestUtil.cli, "fmt", "--check").call(cwd = root, check = false).out.text()
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(updatedContent == noCrLf(simpleInputsUnformattedContent))
      expect(noCrLf(out) == "error: --test failed\n")
    }
  }

  test("filter correctly with --check") {
    simpleInputsWithFilter.fromRoot { root =>
      val out      = os.proc(TestUtil.cli, "fmt", ".", "--check").call(cwd = root).out.text().trim
      val outLines = out.linesIterator.toSeq
      expect(outLines.length == 2)
      expect(outLines.head == "Looking for unformatted files...")
      expect(outLines.last == "All files are formatted with scalafmt :)")
    }
  }

  test("--scalafmt-help") {
    emptyInputs.fromRoot { root =>
      val out1 = os.proc(TestUtil.cli, "fmt", "--scalafmt-help").call(cwd = root).out.text().trim
      val out2 = os.proc(TestUtil.cli, "fmt", "-F", "--help").call(cwd = root).out.text().trim
      expect(out1.nonEmpty)
      expect(out1 == out2)
      val outLines       = out1.linesIterator.toSeq
      val outVersionLine = outLines.head
      expect(outVersionLine == s"scalafmt ${Constants.defaultScalafmtVersion}")
      val outUsageLine = outLines.drop(1).head
      expect(outUsageLine == "Usage: scalafmt [options] [<file>...]")
    }
  }

  test("--save-scalafmt-conf") {
    simpleInputsWithDialectOnly.fromRoot { root =>
      os.proc(TestUtil.cli, "fmt", ".", "--save-scalafmt-conf").call(cwd = root)
      val confLines      = os.read.lines(root / confFileName)
      val versionInConf  = confLines(0).stripPrefix("version = ")
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(versionInConf == s"\"${Constants.defaultScalafmtVersion}\"")
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("--scalafmt-dialect") {
    simpleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "fmt", ".", "--scalafmt-dialect", "scala3").call(cwd = root)
      val confLines      = os.read.lines(root / Constants.workspaceDirName / confFileName)
      val dialectInConf  = confLines(1).stripPrefix("runner.dialect = ").trim
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(dialectInConf == "scala3")
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("--scalafmt-version") {
    simpleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "fmt", ".", "--scalafmt-version", "3.5.5").call(cwd = root)
      val confLines      = os.read.lines(root / Constants.workspaceDirName / confFileName)
      val versionInConf  = confLines(0).stripPrefix("version = ").trim
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(versionInConf == "\"3.5.5\"")
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("creating workspace conf file") {
    simpleInputsWithDialectOnly.fromRoot { root =>
      val workspaceConfPath = root / Constants.workspaceDirName / confFileName
      expect(!os.exists(workspaceConfPath))
      os.proc(TestUtil.cli, "fmt", ".").call(cwd = root)
      expect(os.exists(workspaceConfPath))
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("scalafmt conf without version") {
    simpleInputsWithDialectOnly.fromRoot { root =>
      os.proc(TestUtil.cli, "fmt", ".").call(cwd = root)
      val confLines      = os.read.lines(root / Constants.workspaceDirName / confFileName)
      val versionInConf  = confLines(0).stripPrefix("version = ").trim
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(versionInConf == s"\"${Constants.defaultScalafmtVersion}\"")
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("scalafmt conf without dialect") {
    simpleInputsWithVersionOnly.fromRoot { root =>
      os.proc(TestUtil.cli, "fmt", ".").call(cwd = root)
      val confLines      = os.read.lines(root / Constants.workspaceDirName / confFileName)
      val dialectInConf  = confLines(1).stripPrefix("runner.dialect = ")
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(dialectInConf == "scala3")
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }
}
