package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.nio.charset.Charset

/** This is a trait that defined test definitions for scala-oriented build tools like sbt and mill.
  * The build tools like maven doesn't support some of the features like scalaJs, ScalaNative or
  * compile-only dependencies.
  */
trait ExportScalaOrientedBuildToolsTestDefinitions {
  this: ExportCommonTestDefinitions & ScalaCliSuite & TestScalaVersionArgs =>

  def compileOnlyTest(mainClass: String, extraExportArgs: Seq[String] = Nil): Unit = {
    val userName = "John"
    prepareTestInputs(
      ExportTestProjects.compileOnlySource(actualScalaVersion, userName = userName)
    ).fromRoot { root =>
      exportCommand("." +: extraExportArgs*).call(cwd = root, stdout = os.Inherit)
      val res = buildToolCommand(root, None, runMainArgs(Some(mainClass))*)
        .call(cwd = root / outputDir)
      val output = res.out.trim(Charset.defaultCharset())
      expect(output.contains(userName))
      expect(!output.contains("jsoniter-scala-macros"))
    }
  }

  def testZioTest(testClassName: String, extraExportArgs: Seq[String] = Nil): Unit = {
    val testInput = TestInputs(
      // todo: remove this hack after the PR https://github.com/VirtusLab/scala-cli/pull/3046 is merged
      os.rel / "Hello.scala"    -> """object Hello extends App""",
      os.rel / "Zio.test.scala" ->
        s"""|//> using dep dev.zio::zio::1.0.8
            |//> using dep dev.zio::zio-test-sbt::1.0.8
            |
            |import zio._
            |import zio.test._
            |import zio.test.Assertion.equalTo
            |
            |object $testClassName extends DefaultRunnableSpec {
            |  def spec = suite("associativity")(
            |    testM("associativity") {
            |      check(Gen.anyInt, Gen.anyInt, Gen.anyInt) { (x, y, z) =>
            |        assert((x + y) + z)(equalTo(x + (y + z)))
            |      }
            |    }
            |  )
            |}
            |""".stripMargin,
      os.rel / "input" / "input" ->
        """|1
           |2""".stripMargin
    )

    prepareTestInputs(testInput).fromRoot { root =>
      val exportArgs     = "." +: extraExportArgs
      val testArgsToPass = runTestsArgs(None)
      exportCommand(exportArgs*).call(cwd = root, stdout = os.Inherit)
      val testRes    = buildToolCommand(root, None, testArgsToPass*).call(cwd = root / outputDir)
      val testOutput = testRes.out.text(Charset.defaultCharset())
      expect(testOutput.contains("1 succeeded"))
    }
  }
  protected def logbackBugCase(mainClass: String, extraExportArgs: Seq[String] = Nil): Unit =
    prepareTestInputs(ExportTestProjects.logbackBugCase(actualScalaVersion)).fromRoot { root =>
      exportCommand("." +: extraExportArgs*).call(cwd = root, stdout = os.Inherit)
      val res = buildToolCommand(root, Some(mainClass), runMainArgs(Some(mainClass))*)
        .call(cwd = root / outputDir)
      val output = res.out.text(Charset.defaultCharset())
      expect(output.contains("Hello"))
    }

  if runExportTests then {
    test(s"compile-time only for jsoniter macros$commonTestDescriptionSuffix") {
      TestUtil.retryOnCi() {
        compileOnlyTest(mainClass = "main", extraExportArgs = defaultExportCommandArgs)
      }
    }
    test(s"Scala.js$commonTestDescriptionSuffix") {
      TestUtil.retryOnCi() {
        simpleTest(
          inputs = ExportTestProjects.jsTest(actualScalaVersion),
          mainClass = None,
          extraExportArgs = defaultExportCommandArgs
        )
      }
    }
    test(s"zio test$commonTestDescriptionSuffix") {
      TestUtil.retryOnCi() {
        testZioTest(testClassName = "ZioSpec", extraExportArgs = defaultExportCommandArgs)
      }
    }
    test(
      s"Ensure test framework NPE is not thrown when depending on logback$commonTestDescriptionSuffix"
    ) {
      TestUtil.retryOnCi() {
        logbackBugCase(mainClass = "main", extraExportArgs = defaultExportCommandArgs)
      }
    }
  }
}
