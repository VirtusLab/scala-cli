package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File

import scala.jdk.CollectionConverters.*
import scala.util.Properties

/** For the `compile` counterpart, refer to [[CompileScalacCompatTestDefinitions]] */
trait RunScalacCompatTestDefinitions {
  _: RunTestDefinitions =>

  final val smithyVersion = "1.50.0"
  private def shutdownBloop() =
    os.proc(TestUtil.cli, "bloop", "exit", "--power").call(mergeErrIntoOut = true)

  def commandLineScalacXOption(): Unit = {
    val inputs = TestInputs(
      os.rel / "Test.scala" ->
        """object Test {
          |  def main(args: Array[String]): Unit = {
          |    val msg = "Hello"
          |    val foo = List("Not printed", 2, true, new Object)
          |    println(msg)
          |  }
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      def run(warnAny: Boolean) = {
        val cmd = Seq[os.Shellable](
          TestUtil.cli,
          extraOptions,
          ".",
          if (warnAny) Seq("-Xlint:infer-any") else Nil
        )
        os.proc(cmd).call(
          cwd = root,
          stderr = os.Pipe
        )
      }

      val expectedWarning =
        "a type was inferred to be `Any`; this may indicate a programming error."

      val baseRes       = run(warnAny = false)
      val baseOutput    = baseRes.out.trim()
      val baseErrOutput = baseRes.err.text()
      expect(baseOutput == "Hello")
      expect(!baseErrOutput.contains(expectedWarning))

      val res       = run(warnAny = true)
      val output    = res.out.trim()
      val errOutput = res.err.text()
      expect(output == "Hello")
      expect(errOutput.contains(expectedWarning))
    }
  }

  if (actualScalaVersion.startsWith("2.12."))
    test("Command-line -X scalac options") {
      commandLineScalacXOption()
    }

  def commandLineScalacYOption(): Unit = {
    val inputs = TestInputs(
      os.rel / "Delambdafy.scala" ->
        """object Delambdafy {
          |  def main(args: Array[String]): Unit = {
          |    val l = List(0, 1, 2)
          |    println(l.map(_ + 1).mkString)
          |  }
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      // FIXME We don't really use the run command here, in spite of being in RunTests…
      def classNames(inlineDelambdafy: Boolean): Seq[String] = {
        val cmd = Seq[os.Shellable](
          TestUtil.cli,
          "compile",
          extraOptions,
          "--print-class-path",
          ".",
          if (inlineDelambdafy) Seq("-Ydelambdafy:inline") else Nil
        )
        val res = os.proc(cmd).call(cwd = root)
        val cp  = res.out.trim().split(File.pathSeparator).toVector.map(os.Path(_, os.pwd))
        cp
          .filter(os.isDir(_))
          .flatMap(os.list(_))
          .filter(os.isFile(_))
          .map(_.last)
          .filter(_.startsWith("Delambdafy"))
          .filter(_.endsWith(".class"))
          .map(_.stripSuffix(".class"))
      }

      val baseClassNames = classNames(inlineDelambdafy = false)
      expect(baseClassNames.nonEmpty)
      expect(!baseClassNames.exists(_.contains("$anonfun$")))

      val classNames0 = classNames(inlineDelambdafy = true)
      expect(classNames0.exists(_.contains("$anonfun$")))
    }
  }

  if (actualScalaVersion.startsWith("2."))
    test("Command-line -Y scalac options") {
      commandLineScalacYOption()
    }

  test("-X.. options passed to the child app") {
    val inputs = TestInputs(os.rel / "Hello.scala" -> "object Hello extends App {}")
    inputs.fromRoot { root =>
      // Binaries generated with Graal's native-image are run under SubstrateVM
      // that cuts some -X.. java options, so they're not passed
      // to the application's main method. This test ensures it is not
      // cut. "--java-opt" option requires a value, so it would fail
      // if -Xmx1g is cut
      val res = os.proc(TestUtil.cli, "Hello.scala", "--java-opt", "-Xmx1g").call(
        cwd = root,
        check = false
      )
      assert(res.exitCode == 0, clues(res.out.text(), res.err.text()))
    }
  }

  test("scalac help") {
    emptyInputs.fromRoot { root =>
      val res1 = os.proc(
        TestUtil.cli,
        "run",
        extraOptions,
        "--scalac-help"
      )
        .call(cwd = root, mergeErrIntoOut = true)
      expect(res1.out.text().contains("scalac <options> <source files>"))

      val res2 = os.proc(
        TestUtil.cli,
        "run",
        extraOptions,
        "--scalac-option",
        "-help"
      )
        .call(cwd = root, mergeErrIntoOut = true)
      expect(res1.out.text() == res2.out.text())
    }
  }

  for {
    printOption <- {
      val printOptionsForAllVersions   = Seq("-X", "-Xshow-phases", "-Xplugin-list", "-Y")
      val printOptionsScala213OrHigher = Seq("-V", "-Vphases", "-W", "-Xsource:help")
      val printOptionsScala2 = Seq("-Xlint:help", "-opt:help", "-Xmixin-force-forwarders:help")
      actualScalaVersion match {
        case v if v.startsWith("3") => printOptionsForAllVersions ++ printOptionsScala213OrHigher
        case v if v.startsWith("2.13") =>
          printOptionsForAllVersions ++ printOptionsScala213OrHigher ++ printOptionsScala2
        case v if v.startsWith("2.12") => printOptionsForAllVersions ++ printOptionsScala2
      }
    }
    explicitSubcommand <- Seq(true, false)
    explicitSubcommandString =
      if (explicitSubcommand) "(explicit run subcommand)" else "(default subcommand)"
  } test(s"scalac print option: $printOption $explicitSubcommandString") {
    emptyInputs.fromRoot { root =>
      val res =
        (
          if (explicitSubcommand) os.proc(TestUtil.cli, "run", printOption, extraOptions)
          else os.proc(TestUtil.cli, printOption, extraOptions)
        ).call(cwd = root, mergeErrIntoOut = true)
      expect(res.exitCode == 0)
      expect(res.out.text().nonEmpty)
    }
  }

  test("-classpath allows to run with scala-cli compile -d option pre-compiled classes") {
    val preCompileDir    = "PreCompileDir"
    val preCompiledInput = "Message.scala"
    val runDir           = "RunDir"
    val mainInput        = "Main.scala"
    val expectedOutput   = "Hello"
    TestInputs(
      os.rel / preCompileDir / preCompiledInput -> "case class Message(value: String)",
      os.rel / runDir / mainInput -> s"""object Main extends App { println(Message("$expectedOutput").value) }"""
    ).fromRoot { (root: os.Path) =>
      val preCompileOutputDir = os.rel / "outParentDir" / "out"

      // first, precompile to an explicitly specified output directory with -d
      os.proc(
        TestUtil.cli,
        "compile",
        preCompiledInput,
        "-d",
        preCompileOutputDir.toString,
        extraOptions
      ).call(cwd = root / preCompileDir)

      // next, run while relying on the pre-compiled class, specifying the path with -classpath
      val runRes = os.proc(
        TestUtil.cli,
        "run",
        mainInput,
        "-classpath",
        (os.rel / os.up / preCompileDir / preCompileOutputDir).toString,
        extraOptions
      ).call(cwd = root / runDir)
      expect(runRes.out.trim() == expectedOutput)
    }
  }

  test("-O -classpath allows to run with scala-cli compile -O -d option pre-compiled classes") {
    val preCompileDir    = "PreCompileDir"
    val preCompiledInput = "Message.scala"
    val runDir           = "RunDir"
    val mainInput        = "Main.scala"
    val expectedOutput   = "Hello"
    TestInputs(
      os.rel / preCompileDir / preCompiledInput -> "case class Message(value: String)",
      os.rel / runDir / mainInput -> s"""object Main extends App { println(Message("$expectedOutput").value) }"""
    ).fromRoot { (root: os.Path) =>
      val preCompileOutputDir = os.rel / "outParentDir" / "out"

      // first, precompile to an explicitly specified output directory with -O -d
      val compileRes = os.proc(
        TestUtil.cli,
        "compile",
        preCompiledInput,
        "-O",
        "-d",
        "-O",
        preCompileOutputDir.toString,
        extraOptions
      ).call(cwd = root / preCompileDir, stderr = os.Pipe)
      expect(!compileRes.err.trim().contains("Warning: Flag -d set repeatedly"))

      // next, run while relying on the pre-compiled class, specifying the path with -O -classpath
      val runRes = os.proc(
        TestUtil.cli,
        "run",
        mainInput,
        "-O",
        "-classpath",
        "-O",
        (os.rel / os.up / preCompileDir / preCompileOutputDir).toString,
        extraOptions
      ).call(cwd = root / runDir, stderr = os.Pipe)
      expect(!runRes.err.trim().contains("Warning: Flag -classpath set repeatedly"))
      expect(runRes.out.trim() == expectedOutput)
    }
  }

  test("run main class from -classpath even when no explicit inputs are passed") {
    val expectedOutput = "Hello"
    TestInputs(
      os.rel / "Main.scala" -> s"""object Main extends App { println("$expectedOutput") }"""
    ).fromRoot { (root: os.Path) =>
      val compilationOutputDir = os.rel / "compilationOutput"
      // first, precompile to an explicitly specified output directory with -d
      os.proc(
        TestUtil.cli,
        "compile",
        ".",
        "-d",
        compilationOutputDir,
        extraOptions
      ).call(cwd = root)

      // next, run while relying on the pre-compiled class instead of passing inputs
      val runRes = os.proc(
        TestUtil.cli,
        "run",
        "-classpath",
        (os.rel / compilationOutputDir).toString,
        extraOptions
      ).call(cwd = root)
      expect(runRes.out.trim() == expectedOutput)
    }
  }

  test("run main class from --dep even when no explicit inputs are passed") {
    shutdownBloop()
    val output = os.proc(
      TestUtil.cli,
      "--dep",
      s"software.amazon.smithy:smithy-cli:$smithyVersion",
      "--main-class",
      "software.amazon.smithy.cli.SmithyCli",
      "--",
      "--version"
    ).call()
    assert(output.exitCode == 0)
    assert(output.out.text().contains(smithyVersion))

    // assert bloop wasn't started
    assertNoDiff(shutdownBloop().out.text(), "No running Bloop server found.")
  }

  test("find and run main class from --dep even when no explicit inputs are passed") {
    shutdownBloop()
    val output = os.proc(
      TestUtil.cli,
      "run",
      "--dep",
      s"software.amazon.smithy:smithy-cli:$smithyVersion",
      "--",
      "--version"
    ).call()
    assert(output.exitCode == 0)
    assert(output.out.text().contains(smithyVersion))

    // assert bloop wasn't started
    assertNoDiff(shutdownBloop().out.text(), "No running Bloop server found.")
  }

  test("dont clear output dir") {
    val expectedOutput = "Hello"
    val `lib.scala`    = os.rel / "lib.scala"
    val `utils.scala`  = os.rel / "utils.scala"
    TestInputs(
      `lib.scala`   -> s"""object lib { def foo = "$expectedOutput" }""",
      `utils.scala` -> s"""object utils { def bar = lib.foo }"""
    ).fromRoot { (root: os.Path) =>
      val compilationOutputDir = os.rel / "compilationOutput"
      // first, precompile to an explicitly specified output directory with -d
      os.proc(
        TestUtil.cli,
        "compile",
        "-d",
        compilationOutputDir,
        `lib.scala`,
        extraOptions
      ).call(cwd = root)

      val outputFiles = os.list(root / compilationOutputDir)
      expect(outputFiles.exists(_.endsWith(os.rel / "lib$.class")))
      expect(outputFiles.exists(_.endsWith(os.rel / "lib.class")))

      os.proc(
        TestUtil.cli,
        "compile",
        "-d",
        compilationOutputDir,
        "-cp",
        compilationOutputDir,
        `utils.scala`,
        extraOptions
      ).call(cwd = root)

      val outputFlies2 = os.list(root / compilationOutputDir)
      expect(outputFlies2.exists(_.endsWith(os.rel / "utils$.class")))
      expect(outputFlies2.exists(_.endsWith(os.rel / "utils.class")))
      expect(outputFlies2.exists(_.endsWith(os.rel / "lib$.class")))
      expect(outputFlies2.exists(_.endsWith(os.rel / "lib.class")))
    }
  }

  test("run main class from a jar even when no explicit inputs are passed") {
    val expectedOutput = "Hello"
    TestInputs(
      os.rel / "Main.scala" -> s"""object Main extends App { println("$expectedOutput") }"""
    ).fromRoot { (root: os.Path) =>
      // first, package the code to a jar with a main class
      val jarPath = os.rel / "Main.jar"
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        ".",
        "--library",
        "-o",
        jarPath,
        extraOptions
      ).call(cwd = root)

      // next, run while relying on the jar instead of passing inputs
      val runRes = os.proc(
        TestUtil.cli,
        "run",
        "-classpath",
        jarPath,
        extraOptions
      ).call(cwd = root)
      expect(runRes.out.trim() == expectedOutput)
    }
  }

  test("run main class from a jar in a directory even when no explicit inputs are passed") {
    val expectedOutput = "Hello"
    TestInputs(
      os.rel / "Main.scala" -> s"""object Main extends App { println("$expectedOutput") }"""
    ).fromRoot { (root: os.Path) =>
      // first, package the code to a jar with a main class
      val jarParentDirectory = os.rel / "out"
      val jarPath            = jarParentDirectory / "Main.jar"
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        ".",
        "--library",
        "-o",
        jarPath,
        extraOptions
      ).call(cwd = root)

      // next, run while relying on the jar instead of passing inputs
      val runRes = os.proc(
        TestUtil.cli,
        "run",
        "-cp",
        jarParentDirectory,
        extraOptions
      ).call(cwd = root)
      expect(runRes.out.trim() == expectedOutput)
    }
  }

  def verifyScala212VersionCompiles(scalaPatchVersion: String): Unit = {
    TestInputs(os.rel / "s.sc" -> "println(util.Properties.versionNumberString)").fromRoot {
      root =>
        val scala212VersionString = s"2.12.$scalaPatchVersion"
        val res =
          os.proc(TestUtil.cli, "run", ".", "-S", scala212VersionString, TestUtil.extraOptions)
            .call(cwd = root)
        expect(res.out.trim() == scala212VersionString)
    }
  }

  if (actualScalaVersion.startsWith("2.12."))
    (1 until 4).map(_.toString).foreach { scalaPatchVersion =>
      // FIXME this shouldn't be flaky on Mac
      if (TestUtil.isCI && Properties.isMac && TestUtil.isNativeCli) test(
        s"verify that Scala version 2.12.$scalaPatchVersion is respected and compiles correctly".flaky
      )(verifyScala212VersionCompiles(scalaPatchVersion))
      else test(
        s"verify that Scala version 2.12.$scalaPatchVersion is respected and compiles correctly"
      )(verifyScala212VersionCompiles(scalaPatchVersion))
    }

  test("scalac verbose") {
    val expectedOutput = "Hello"
    val mainClass      = "Main"
    val inputRelPath   = os.rel / s"$mainClass.scala"
    TestInputs(inputRelPath -> s"""object $mainClass extends App { println("$expectedOutput") }""")
      .fromRoot { root =>
        val res = os.proc(TestUtil.cli, ".", "--scalac-verbose", extraOptions)
          .call(cwd = root, stderr = os.Pipe)
        val errLines = res.err.trim().lines.toList.asScala
        // there should be a lot of logs, but different stuff is logged depending on the Scala version
        expect(errLines.length > 100)
        expect(errLines.exists(_.startsWith("[loaded package loader scala")))
        expect(errLines.exists(_.contains(s"$mainClass.scala")))
        expect(res.out.trim() == expectedOutput)
      }
  }

  if (!Properties.isWin)
    test("-encoding CP1252 should be handled correctly in .scala files") {
      TestInputs(
        charsetName = "Windows-1252",
        os.rel / "s.scala" -> """object Main extends App { println("€") }"""
      )
        .fromRoot { root =>
          val res = os.proc(
            TestUtil.cli,
            "s.scala",
            "-encoding",
            "cp1252",
            extraOptions
          ).call(cwd = root)
          expect(res.out.trim() == "€")
        }
    }

  if (actualScalaVersion.startsWith("3") || actualScalaVersion.startsWith("2.13")) {
    val fileName       = "Main.scala"
    val expectedOutput = "Hello"
    val oldSyntaxCode =
      s"""object Main extends App {
         |  if (true) println("$expectedOutput") else println("Error")
         |}
         |""".stripMargin
    val newSyntaxCode =
      s"""object Main extends App {
         |  if true then println("$expectedOutput") else println("Error")
         |}
         |""".stripMargin

    test("rewrite code to new syntax and then run it correctly (no -O required)") {
      TestInputs(os.rel / fileName -> oldSyntaxCode)
        .fromRoot { root =>
          val res = os.proc(
            TestUtil.cli,
            fileName,
            "-new-syntax",
            "-rewrite",
            "-source:3.2-migration"
          ).call(cwd = root, stderr = os.Pipe)
          val filePath = root / fileName
          expect(res.err.trim().contains(s"[patched file $filePath]"))
          expect(os.read(filePath) == newSyntaxCode)
          expect(res.out.trim() == expectedOutput)
        }
    }

    test("rewrite code to old syntax and then run it correctly (no -O required)") {
      TestInputs(os.rel / fileName -> newSyntaxCode)
        .fromRoot { root =>
          val res = os.proc(
            TestUtil.cli,
            fileName,
            "-old-syntax",
            "-rewrite",
            "-source:3.2-migration"
          ).call(cwd = root, stderr = os.Pipe)
          val filePath = root / fileName
          expect(res.err.trim().contains(s"[patched file $filePath]"))
          expect(os.read(filePath) == oldSyntaxCode)
          expect(res.out.trim() == expectedOutput)
        }
    }
  }

  if (actualScalaVersion.startsWith("3"))
    test("ensure -with-compiler is supported for Scala 3") {
      TestInputs(
        os.rel / "s.sc" ->
          "println(dotty.tools.dotc.config.Properties.simpleVersionString)"
      )
        .fromRoot { root =>
          val res =
            os.proc(TestUtil.cli, "-with-compiler", "s.sc", extraOptions)
              .call(cwd = root)
          expect(res.out.trim() == actualScalaVersion)
        }
    }

  if (actualScalaVersion.startsWith("2.13"))
    test("ensure -with-compiler is supported for Scala 2.13") {
      TestInputs(os.rel / "s.sc" -> "println(scala.tools.nsc.Properties.versionString)")
        .fromRoot { root =>
          val res =
            os.proc(TestUtil.cli, "-with-compiler", "s.sc", extraOptions)
              .call(cwd = root)
          expect(res.out.trim() == s"version $actualScalaVersion")
        }
    }

  for {
    useDirective <- Seq(true, false)
    if !Properties.isWin
    optionsSource = if (useDirective) "using directive" else "command line"
    if actualScalaVersion == Constants.scala3Next || actualScalaVersion == Constants.scala3NextRc
  }
    test(s"consecutive -Xmacro-settings:* flags are not ignored (passed via $optionsSource)") {
      val sourceFileName = "example.scala"
      val macroFileName  = "macro.scala"
      val macroSettings @ Seq(macroSetting1, macroSetting2, macroSetting3) =
        Seq("one", "two", "three")
      val macroSettingOptions = macroSettings.map(s => s"-Xmacro-settings:$s")
      val maybeDirectiveString =
        if (useDirective) s"//> using options ${macroSettingOptions.mkString(" ")}" else ""
      TestInputs(
        os.rel / macroFileName ->
          """package x
            |import scala.quoted.*
            |object M:
            |  inline def settingsContains(inline x:String): Boolean = ${
            |     settingsContainsImpl('x)
            |  }
            |  def settingsContainsImpl(x:Expr[String])(using Quotes): Expr[Boolean] =
            |     import quotes.reflect.*
            |     val v = x.valueOrAbort
            |     val r = CompilationInfo.XmacroSettings.contains(v)
            |     Expr(r)
            |""".stripMargin,
        os.rel / sourceFileName ->
          s"""$maybeDirectiveString
             |import x.M
             |@main def main(): Unit = {
             |  val output = Seq(
             |    if M.settingsContains("$macroSetting1") then Seq("$macroSetting1") else Nil,
             |    if M.settingsContains("$macroSetting2") then Seq("$macroSetting2") else Nil,
             |    if M.settingsContains("$macroSetting3") then Seq("$macroSetting3") else Nil,
             |    if M.settingsContains("dummy") then Seq("dummy") else Nil,
             |  )
             |  println(output.flatten.mkString(", "))
             |}
             |
             |""".stripMargin
      ).fromRoot { root =>
        val r = os.proc(
          TestUtil.cli,
          "run",
          ".",
          "-O",
          "-experimental",
          if (useDirective) Nil else macroSettingOptions,
          extraOptions
        )
          .call(cwd = root, stderr = os.Pipe)
        expect(r.out.trim() == macroSettings.mkString(", "))
      }
    }
}
