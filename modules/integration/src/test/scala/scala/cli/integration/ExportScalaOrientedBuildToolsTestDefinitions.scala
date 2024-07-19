package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.nio.charset.Charset

/** This is a trait that defined test definitions for scala-oriented build tools like sbt and mill.
  * The build tools like maven doesn't support some of the features like scalaJs, ScalaNative or
  * compile-only dependencies.
  */
trait ExportScalaOrientedBuildToolsTestDefinitions {
  _: ExportCommonTestDefinitions & ScalaCliSuite & TestScalaVersionArgs =>

  def compileOnlyTest(mainClass: String): Unit = {
    val userName = "John"
    prepareTestInputs(
      ExportTestProjects.compileOnlySource(actualScalaVersion, userName = userName)
    ).fromRoot { root =>
      exportCommand(".").call(cwd = root, stdout = os.Inherit)
      val res = buildToolCommand(root, None, runMainArgs(Some(mainClass))*)
        .call(cwd = root / outputDir)
      val output = res.out.trim(Charset.defaultCharset())
      expect(output.contains(userName))
      expect(!output.contains("jsoniter-scala-macros"))
    }
  }

  if (runExportTests) {
    test("compile-time only for jsoniter macros") {
      compileOnlyTest("main")
    }
    test("Scala.js") {
      simpleTest(ExportTestProjects.jsTest(actualScalaVersion), mainClass = None)
    }
  }
}
