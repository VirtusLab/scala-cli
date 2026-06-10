package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.removeAnsiColors

class FmtTests extends ScalaCliSuite {
  override def group: ScalaCliSuite.TestGroup = ScalaCliSuite.TestGroup.First

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

  val simpleInputsWithCustomConfLocation: TestInputs = TestInputs(
    os.rel / "custom.conf" ->
      s"""|version = "3.5.5"
          |runner.dialect = scala213
          |""".stripMargin,
    os.rel / "Foo.scala" -> simpleInputsUnformattedContent
  )

  val simpleInputsWithoutConf: TestInputs = TestInputs(
    os.rel / "Foo.scala" -> simpleInputsUnformattedContent
  )

  private def workspaceConfPath(root: os.Path): os.Path =
    root / Constants.workspaceDirName / confFileName

  private def expectNoWorkspaceScalafmtConf(root: os.Path): Unit = {
    expect(!os.exists(workspaceConfPath(root)))
    expect(!os.exists(root / Constants.workspaceDirName))
  }

  private def noCrLf(input: String): String =
    input.replaceAll("\r\n", "\n")

  test("simple") {
    simpleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, TestUtil.powerOptions, "fmt", TestUtil.offlineOptions, ".").call(cwd =
        root
      )
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("no inputs") {
    simpleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, TestUtil.powerOptions, "fmt", TestUtil.offlineOptions).call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("with --check") {
    simpleInputs.fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        TestUtil.powerOptions,
        "fmt",
        TestUtil.offlineOptions,
        "--check"
      ).call(cwd = root, check = false)
      expect(res.exitCode == 1)
      val out            = res.out.text()
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(updatedContent == noCrLf(simpleInputsUnformattedContent))
      expect(noCrLf(out) == "error: --test failed\n")
    }
  }

  test("filter correctly with --check") {
    simpleInputsWithFilter.fromRoot { root =>
      val out = os.proc(
        TestUtil.cli,
        TestUtil.powerOptions,
        "fmt",
        TestUtil.offlineOptions,
        ".",
        "--check"
      ).call(cwd = root).out.trim()
      expect(out == "All files are formatted with scalafmt :)")
    }
  }

  test("--scalafmt-help") {
    emptyInputs.fromRoot { root =>
      val out1 = os.proc(
        TestUtil.cli,
        TestUtil.powerOptions,
        "fmt",
        TestUtil.offlineOptions,
        "--scalafmt-help"
      ).call(cwd = root).out.trim()
      val out2 = os.proc(
        TestUtil.cli,
        TestUtil.powerOptions,
        "fmt",
        TestUtil.offlineOptions,
        "-F",
        "--help"
      ).call(cwd = root).out.trim()
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
      os.proc(
        TestUtil.cli,
        TestUtil.powerOptions,
        "fmt",
        TestUtil.offlineOptions,
        ".",
        "--save-scalafmt-conf"
      ).call(cwd = root)
      val confLines      = os.read.lines(root / confFileName)
      val versionInConf  = confLines(0).stripPrefix("version = ")
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(versionInConf == s"\"${Constants.defaultScalafmtVersion}\"")
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("--scalafmt-dialect") {
    simpleInputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        TestUtil.powerOptions,
        "fmt",
        TestUtil.offlineOptions,
        ".",
        "--scalafmt-dialect",
        "scala3"
      ).call(cwd = root)
      val confLines      = os.read.lines(root / Constants.workspaceDirName / confFileName)
      val dialectInConf  = confLines(1).stripPrefix("runner.dialect = ").trim
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(dialectInConf == "scala3")
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("--scalafmt-version") {
    simpleInputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        TestUtil.powerOptions,
        "fmt",
        TestUtil.offlineOptions,
        ".",
        "--scalafmt-version",
        "3.5.5"
      ).call(cwd = root)
      val confLines      = os.read.lines(root / Constants.workspaceDirName / confFileName)
      val versionInConf  = confLines(0).stripPrefix("version = ").trim
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(versionInConf == "\"3.5.5\"")
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("--scalafmt-conf") {
    simpleInputsWithCustomConfLocation.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        TestUtil.powerOptions,
        "fmt",
        TestUtil.offlineOptions,
        ".",
        "--scalafmt-conf",
        "custom.conf"
      ).call(cwd = root)
      val confLines      = os.read.lines(root / Constants.workspaceDirName / confFileName)
      val versionInConf  = confLines(0).stripPrefix("version = ").trim
      val dialectInConf  = confLines(1).stripPrefix("runner.dialect = ").trim
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(versionInConf == "\"3.5.5\"")
      expect(dialectInConf == "scala213")
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("--scalafmt-conf-str") {
    simpleInputsWithVersionOnly.fromRoot { root =>
      val confStr =
        s"""version = 3.5.7${System.lineSeparator}runner.dialect = scala213${System.lineSeparator}"""
      os.proc(
        TestUtil.cli,
        TestUtil.powerOptions,
        "fmt",
        TestUtil.offlineOptions,
        ".",
        "--scalafmt-conf-str",
        s"$confStr"
      ).call(cwd = root)
      val confLines      = os.read.lines(root / Constants.workspaceDirName / confFileName)
      val versionInConf  = confLines(0).stripPrefix("version = ")
      val dialectInConf  = confLines(1).stripPrefix("runner.dialect = ")
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(versionInConf == "\"3.5.7\"")
      expect(dialectInConf == "scala213")
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("complete .scalafmt.conf is not duplicated under .scala-build") {
    simpleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, TestUtil.powerOptions, "fmt", TestUtil.offlineOptions, ".").call(cwd =
        root
      )
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(updatedContent == expectedSimpleInputsFormattedContent)
      expectNoWorkspaceScalafmtConf(root)
    }
  }

  test("complete .scalafmt.conf + matching --scalafmt-version still avoids .scala-build") {
    simpleInputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        TestUtil.powerOptions,
        "fmt",
        TestUtil.offlineOptions,
        ".",
        "--scalafmt-version",
        Constants.defaultScalafmtVersion
      ).call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(updatedContent == expectedSimpleInputsFormattedContent)
      expectNoWorkspaceScalafmtConf(root)
    }
  }

  test("complete .scalafmt.conf + matching --scalafmt-dialect still avoids .scala-build") {
    simpleInputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        TestUtil.powerOptions,
        "fmt",
        TestUtil.offlineOptions,
        ".",
        "--scalafmt-dialect",
        "scala213"
      ).call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(updatedContent == expectedSimpleInputsFormattedContent)
      expectNoWorkspaceScalafmtConf(root)
    }
  }

  test("complete .scalafmt.conf in git root is used in place") {
    simpleInputs.fromRoot { root =>
      TestUtil.initializeGit(root)
      val subdir = root / "subdir"
      os.makeDir.all(subdir)
      os.move(root / "Foo.scala", subdir / "Foo.scala")
      os.proc(TestUtil.cli, TestUtil.powerOptions, "fmt", TestUtil.offlineOptions, ".").call(cwd =
        subdir
      )
      val updatedContent = noCrLf(os.read(subdir / "Foo.scala"))
      expect(updatedContent == expectedSimpleInputsFormattedContent)
      expectNoWorkspaceScalafmtConf(subdir)
      expectNoWorkspaceScalafmtConf(root)
    }
  }

  test("no .scalafmt.conf still triggers .scala-build") {
    simpleInputsWithoutConf.fromRoot { root =>
      // Isolate from the enclosing scala-cli git repo
      // (whose .scalafmt.conf would otherwise be discovered)
      TestUtil.initializeGit(root)
      val confPath = workspaceConfPath(root)
      expect(!os.exists(confPath))
      os.proc(TestUtil.cli, TestUtil.powerOptions, "fmt", TestUtil.offlineOptions, ".").call(cwd =
        root
      )
      expect(os.exists(confPath))
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("creating workspace conf file") {
    simpleInputsWithDialectOnly.fromRoot { root =>
      val confPath = workspaceConfPath(root)
      expect(!os.exists(confPath))
      os.proc(TestUtil.cli, TestUtil.powerOptions, "fmt", TestUtil.offlineOptions, ".").call(cwd =
        root
      )
      expect(os.exists(confPath))
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("scalafmt conf without version") {
    simpleInputsWithDialectOnly.fromRoot { root =>
      os.proc(TestUtil.cli, TestUtil.powerOptions, "fmt", TestUtil.offlineOptions, ".").call(cwd =
        root
      )
      val confLines      = os.read.lines(root / Constants.workspaceDirName / confFileName)
      val versionInConf  = confLines(0).stripPrefix("version = ").trim
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(versionInConf == s"\"${Constants.defaultScalafmtVersion}\"")
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("scalafmt conf without dialect") {
    simpleInputsWithVersionOnly.fromRoot { root =>
      os.proc(TestUtil.cli, TestUtil.powerOptions, "fmt", TestUtil.offlineOptions, ".").call(cwd =
        root
      )
      val confLines      = os.read.lines(root / Constants.workspaceDirName / confFileName)
      val dialectInConf  = confLines(1).stripPrefix("runner.dialect = ")
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(dialectInConf == "scala3")
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("default values in help") {
    TestInputs.empty.fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        TestUtil.powerOptions,
        "fmt",
        TestUtil.offlineOptions,
        "--help"
      ).call(cwd = root)
      val lines          = removeAnsiColors(res.out.trim()).linesIterator.toVector
      val fmtVersionHelp = lines.find(_.contains("--fmt-version")).getOrElse("")
      expect(fmtVersionHelp.contains(s"(${Constants.defaultScalafmtVersion} by default)"))
    }
  }

  test("project.scala gets formatted correctly, as any other input") {
    val projectFileName = "project.scala"
    TestInputs(
      os.rel / projectFileName -> simpleInputsUnformattedContent,
      os.rel / confFileName    ->
        s"""|version = "${Constants.defaultScalafmtVersion}"
            |runner.dialect = scala3
            |""".stripMargin
    )
      .fromRoot { root =>
        os.proc(TestUtil.cli, TestUtil.powerOptions, "fmt", TestUtil.offlineOptions, ".").call(cwd =
          root
        )
        val updatedContent = noCrLf(os.read(root / projectFileName))
        expect(updatedContent == expectedSimpleInputsFormattedContent)
      }
  }

  val sbtUnformattedContent: String =
    """val message    =       "hello"
      |""".stripMargin
  val expectedSbtFormattedContent: String = noCrLf {
    """val message = "hello"
      |""".stripMargin
  }
  val sbtInputs: TestInputs = TestInputs(
    os.rel / confFileName ->
      s"""|version = "${Constants.defaultScalafmtVersion}"
          |runner.dialect = scala213
          |""".stripMargin,
    os.rel / "build.sbt" -> sbtUnformattedContent
  )

  test("sbt file is formatted when passed explicitly") {
    sbtInputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        TestUtil.powerOptions,
        "fmt",
        TestUtil.offlineOptions,
        "build.sbt"
      ).call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "build.sbt"))
      expect(updatedContent == expectedSbtFormattedContent)
    }
  }

  test("sbt file is formatted when directory is passed") {
    sbtInputs.fromRoot { root =>
      os.proc(TestUtil.cli, TestUtil.powerOptions, "fmt", TestUtil.offlineOptions, ".").call(cwd =
        root
      )
      val updatedContent = noCrLf(os.read(root / "build.sbt"))
      expect(updatedContent == expectedSbtFormattedContent)
    }
  }
}
