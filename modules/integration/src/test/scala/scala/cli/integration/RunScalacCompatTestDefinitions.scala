package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File

import scala.jdk.CollectionConverters.*
import scala.util.Properties

trait RunScalacCompatTestDefinitions { _: RunTestDefinitions =>
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
        // format: off
        val cmd = Seq[os.Shellable](
          TestUtil.cli, extraOptions, ".",
          if (warnAny) Seq("-Xlint:infer-any") else Nil
        )
        // format: on
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
        // format: off
        val cmd = Seq[os.Shellable](
          TestUtil.cli, "compile", extraOptions,
          "--print-class-path", ".",
          if (inlineDelambdafy) Seq("-Ydelambdafy:inline") else Nil
        )
        // format: on
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

  test("scalac print options") {
    emptyInputs.fromRoot { root =>
      val printOptionsForAllVersions = Seq("-X", "-Xshow-phases", "-Y")
      val printOptionsSince213       = Seq("-V", "-Vphases", "-W")
      val version213OrHigher =
        actualScalaVersion.startsWith("2.13") || actualScalaVersion.startsWith("3")
      val printOptionsToTest = printOptionsForAllVersions ++
        (
          if (version213OrHigher) printOptionsSince213
          else Seq.empty
        )
      printOptionsToTest.foreach { printOption =>
        val res = os.proc(
          TestUtil.cli,
          extraOptions,
          printOption
        )
          .call(cwd = root, mergeErrIntoOut = true)
        expect(res.out.text().nonEmpty)
      }
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

  if (actualScalaVersion.startsWith("2.12."))
    test("verify that Scala version 2.12.x < 2.12.4 is respected and compiles correctly") {
      TestInputs(os.rel / "s.sc" -> "println(util.Properties.versionNumberString)").fromRoot {
        root =>
          (1 until 4).foreach { scalaPatchVersion =>
            val scala212VersionString = s"2.12.$scalaPatchVersion"
            val res =
              os.proc(TestUtil.cli, "run", ".", "-S", scala212VersionString, TestUtil.extraOptions)
                .call(cwd = root)
            expect(res.out.trim() == scala212VersionString)
          }
      }
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
}
