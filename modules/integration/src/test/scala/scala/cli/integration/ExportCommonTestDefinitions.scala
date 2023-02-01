package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.nio.charset.Charset

import scala.util.Properties

trait ExportCommonTestDefinitions { _: ScalaCliSuite & TestScalaVersionArgs =>
  protected lazy val extraOptions: Seq[String] = scalaVersionArgs ++ TestUtil.extraOptions

  protected def runExportTests: Boolean = Properties.isLinux

  protected def exportCommand(args: String*): os.proc

  protected def buildToolCommand(root: os.Path, args: String*): os.proc

  protected def runMainArgs: Seq[String]
  protected def runTestsArgs: Seq[String]

  protected val prepareTestInputs: TestInputs => TestInputs = identity

  protected val outputDir: os.RelPath = os.rel / "output-project"

  protected def simpleTest(inputs: TestInputs, extraExportArgs: Seq[String] = Nil): Unit =
    prepareTestInputs(inputs).fromRoot { root =>
      val exportArgs = "." +: extraExportArgs
      exportCommand(exportArgs*).call(cwd = root, stdout = os.Inherit)
      val res =
        buildToolCommand(root, runMainArgs*).call(cwd = root / outputDir)
      val output = res.out.text(Charset.defaultCharset())
      expect(output.contains("Hello from exported Scala CLI project"))
    }

  protected def jvmTest(
    mainArgs: Seq[String] = runMainArgs,
    testArgs: Seq[String] = runTestsArgs,
    extraExportArgs: Seq[String] = Nil
  ): Unit =
    prepareTestInputs(ExportTestProjects.jvmTest(actualScalaVersion)).fromRoot { root =>
      val exportArgs = "." +: extraExportArgs
      exportCommand(exportArgs*).call(cwd = root, stdout = os.Inherit)
      // main
      val res    = buildToolCommand(root, mainArgs*).call(cwd = root / outputDir)
      val output = res.out.text(Charset.defaultCharset())
      expect(output.contains("Hello from " + actualScalaVersion))
      // resource
      expect(output.contains("resource:1,2"))
      // test
      val testRes    = buildToolCommand(root, testArgs*).call(cwd = root / outputDir)
      val testOutput = testRes.out.text(Charset.defaultCharset())
      expect(testOutput.contains("1 succeeded"))
    }

  protected def logbackBugCase(): Unit =
    prepareTestInputs(ExportTestProjects.logbackBugCase(actualScalaVersion)).fromRoot { root =>
      exportCommand(".").call(cwd = root, stdout = os.Inherit)
      val res = buildToolCommand(root, runMainArgs*)
        .call(cwd = root / outputDir)
      val output = res.out.text(Charset.defaultCharset())
      expect(output.contains("Hello"))
    }

  if (runExportTests) {
    test("JVM") {
      jvmTest()
    }
    test("Scala.js") {
      simpleTest(ExportTestProjects.jsTest(actualScalaVersion))
    }
    test("Scala Native") {
      simpleTest(ExportTestProjects.nativeTest(actualScalaVersion))
    }
    test("Ensure test framework NPE is not thrown when depending on logback") {
      logbackBugCase()
    }
  }
}
