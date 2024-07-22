package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.nio.charset.Charset

import scala.util.Properties

trait ExportCommonTestDefinitions { _: ScalaCliSuite & TestScalaVersionArgs =>
  protected lazy val extraOptions: Seq[String] =
    scalaVersionArgs ++ TestUtil.extraOptions ++ Seq("--suppress-experimental-warning")

  protected def runExportTests: Boolean = Properties.isMac
  protected def exportCommand(args: String*): os.proc

  protected def buildToolCommand(root: os.Path, mainClass:Option[String], args: String*): os.proc

  protected def runMainArgs(mainClass: Option[String]): Seq[String]
  protected def runTestsArgs(mainClass: Option[String]): Seq[String]

  protected val prepareTestInputs: TestInputs => TestInputs = identity

  protected val outputDir: os.RelPath = os.rel / "output-project"

  protected def simpleTest(inputs: TestInputs, mainClass:Option[String], extraExportArgs: Seq[String] = Nil): Unit =
    prepareTestInputs(inputs).fromRoot { root =>
      val exportArgs = "." +: extraExportArgs
      exportCommand(exportArgs*).call(cwd = root, stdout = os.Inherit)
      val res =
        buildToolCommand(root, mainClass, runMainArgs(mainClass)*).call(cwd = root / outputDir)
      val output = res.out.text(Charset.defaultCharset())
      expect(output.contains("Hello from exported Scala CLI project"))
    }

  protected def jvmTest(
    mainArgs: Seq[String],
    testArgs: Seq[String],
    extraExportArgs: Seq[String] = Nil,
    mainClassName: String
  ): Unit =
    prepareTestInputs(ExportTestProjects.jvmTest(actualScalaVersion, mainClassName)).fromRoot { root =>
      val exportArgs = "." +: extraExportArgs
      exportCommand(exportArgs*).call(cwd = root, stdout = os.Inherit)
      // main
      val res    = buildToolCommand(root, Some(mainClassName), mainArgs*).call(cwd = root / outputDir)
      val output = res.out.text(Charset.defaultCharset())
      expect(output.contains("Hello from " + actualScalaVersion))
      // resource
      expect(output.contains("resource:1,2"))
      // test
      val testRes    = buildToolCommand(root, Some(mainClassName), testArgs*).call(cwd = root / outputDir)
      val testOutput = testRes.out.text(Charset.defaultCharset())
      expect(testOutput.contains("1 succeeded") || testOutput.contains("BUILD SUCCESS")) //maven returns 'BUILD SUCCESS'
    }

  protected def scalaVersionTest(scalaVersion: String, mainClass: String): Unit =
    prepareTestInputs(ExportTestProjects.scalaVersionTest(scalaVersion, mainClass)).fromRoot {
      root =>
        exportCommand(".").call(cwd = root, stdout = os.Inherit)
        val res = buildToolCommand(root, Some(mainClass), runMainArgs(Some(mainClass))*)
          .call(cwd = root / outputDir)
        val output = res.out.text(Charset.defaultCharset())
        expect(output.contains("Hello"))
    }

  def extraSourceFromDirectiveWithExtraDependency(mainClass: String, inputs: String*): Unit =
    prepareTestInputs(
      ExportTestProjects.extraSourceFromDirectiveWithExtraDependency(actualScalaVersion, mainClass)
    ).fromRoot { root =>
      exportCommand(inputs*).call(cwd = root, stdout = os.Inherit)
      val res = buildToolCommand(root, Some(mainClass), runMainArgs(Some(mainClass))*)
        .call(cwd = root / outputDir)
      val output = res.out.trim(Charset.defaultCharset())
      expect(output.contains(root.toString))
    }

  private val scalaVersionsInDir: Seq[String] = Seq("2.12", "2.13", "2", "3", "3.lts")

  if (runExportTests) {
    test("JVM") {
      jvmTest(runMainArgs(Some("Main")), runTestsArgs(Some("Main")), mainClassName = "Main")
    }
    test("extra source from a directive introducing a dependency") {
      extraSourceFromDirectiveWithExtraDependency("Main","Main.scala")
    }
    test("extra source passed both via directive and from command line") {
      extraSourceFromDirectiveWithExtraDependency("Main", ".")
    }
    scalaVersionsInDir.foreach { scalaV =>
      test(s"check export for project with scala version in directive as $scalaV") {
        scalaVersionTest(scalaV, "Main")
      }
    }

  }
}
