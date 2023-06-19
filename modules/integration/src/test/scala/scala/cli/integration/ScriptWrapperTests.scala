package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

trait ScriptWrapperTests { _: RunTestDefinitions =>
  def expectObjectWrapper(wrapperName: String, path: os.Path) = {
    val generatedFileContent = os.read(path)
    assert(
      generatedFileContent.contains(s"object $wrapperName {"),
      clue(s"Generated file content: $generatedFileContent")
    )
    assert(
      !generatedFileContent.contains(s"final class $wrapperName$$_"),
      clue(s"Generated file content: $generatedFileContent")
    )
  }

  def expectClassWrapper(wrapperName: String, path: os.Path) = {
    val generatedFileContent = os.read(path)
    assert(
      generatedFileContent.contains(s"final class $wrapperName$$_"),
      clue(s"Generated file content: $generatedFileContent")
    )
    assert(
      !generatedFileContent.contains(s"object $wrapperName {"),
      clue(s"Generated file content: $generatedFileContent")
    )
  }

  if (actualScalaVersion.startsWith("3.")) {
    test("BSP class wrapper for Scala 3") {
      val inputs = TestInputs(
        os.rel / "script.sc" ->
          s"""//> using dep "com.lihaoyi::os-lib:0.9.1"
             |
             |def main(args: String*): Unit = println("Hello")
             |""".stripMargin,
        os.rel / "munit.sc" ->
          s"""//> using dep "org.scalatest::scalatest:3.2.15"
             |
             |import org.scalatest.*, flatspec.*, matchers.*
             |
             |class PiTest extends AnyFlatSpec with should.Matchers {
             |  "pi calculus" should "return a precise enough pi value" in {
             |    math.Pi shouldBe 3.14158d +- 0.001d
             |  }
             |}
             |org.scalatest.tools.Runner.main(Array("-oDF", "-s", classOf[PiTest].getName))""".stripMargin
      )
      inputs.fromRoot { root =>
        TestUtil.withThreadPool("script-wrapper-bsp-test", 2) { pool =>
          val timeout     = Duration("60 seconds")
          implicit val ec = ExecutionContext.fromExecutorService(pool)
          val bspProc = os.proc(TestUtil.cli, "--power", "bsp", "script.sc", "munit.sc")
            .spawn(cwd = root, mergeErrIntoOut = true, stdout = os.Pipe)

          def lineReaderIter =
            Iterator.continually(TestUtil.readLine(bspProc.stdout, ec, timeout))

          lineReaderIter.find(_.contains("\"build/taskFinish\""))

          bspProc.destroy()
          if (bspProc.isAlive())
            bspProc.destroyForcibly()

          val projectDir = os.list(root / Constants.workspaceDirName).filter(
            _.baseName.startsWith(root.baseName + "_")
          )
          expect(projectDir.size == 1)
          expectClassWrapper(
            "script",
            projectDir.head / "src_generated" / "main" / "script.scala"
          )
          expectClassWrapper(
            "munit",
            projectDir.head / "src_generated" / "main" / "munit.scala"
          )
        }
      }
    }

    for {
      useDirectives <- Seq(true, false)
      (directive, options) <- Seq(
        ("//> using object.wrapper", Seq("--object-wrapper")),
        ("//> using scala 2.13", Seq("--scala", "2.13")),
        ("//> using platform js", Seq("--js"))
      )
    } {
      val inputs = TestInputs(
        os.rel / "script1.sc" ->
          s"""//> using dep "com.lihaoyi::os-lib:0.9.1"
             |${if (useDirectives) directive else ""}
             |
             |def main(args: String*): Unit = println("Hello")
             |main()
             |""".stripMargin,
        os.rel / "script2.sc" ->
          """//> using dep "com.lihaoyi::os-lib:0.9.1"
            |
            |println("Hello")
            |""".stripMargin
      )

      test(
        s"object wrapper forced with ${if (useDirectives) directive else options.mkString(" ")}"
      ) {
        inputs.fromRoot { root =>
          os.proc(
            TestUtil.cli,
            "--power",
            "script1.sc",
            "script2.sc",
            if (useDirectives) Nil else options
          )
            .call(cwd = root, mergeErrIntoOut = true, stdout = os.Inherit)

          val projectDir = os.list(root / Constants.workspaceDirName).filter(
            _.baseName.startsWith(root.baseName + "_")
          )
          expect(projectDir.size == 1)
          expectObjectWrapper(
            "script1",
            projectDir.head / "src_generated" / "main" / "script1.scala"
          )
          expectObjectWrapper(
            "script2",
            projectDir.head / "src_generated" / "main" / "script2.scala"
          )
        }
      }

      test(
        s"BSP object wrapper forced with ${if (useDirectives) directive else options.mkString(" ")}"
      ) {
        inputs.fromRoot { root =>
          TestUtil.withThreadPool("script-wrapper-bsp-test", 2) { pool =>
            val timeout     = Duration("60 seconds")
            implicit val ec = ExecutionContext.fromExecutorService(pool)

            val bspProc = os.proc(
              TestUtil.cli,
              "--power",
              "bsp",
              "script1.sc",
              "script2.sc",
              if (useDirectives) Nil else options
            )
              .spawn(cwd = root, mergeErrIntoOut = true, stdout = os.Pipe)

            def lineReaderIter =
              Iterator.continually(TestUtil.readLine(bspProc.stdout, ec, timeout))

            lineReaderIter.find(_.contains("\"build/taskFinish\""))

            bspProc.destroy()
            if (bspProc.isAlive())
              bspProc.destroyForcibly()

            val projectDir = os.list(root / Constants.workspaceDirName).filter(
              _.baseName.startsWith(root.baseName + "_")
            )
            expect(projectDir.size == 1)
            expectObjectWrapper(
              "script1",
              projectDir.head / "src_generated" / "main" / "script1.scala"
            )
            expectObjectWrapper(
              "script2",
              projectDir.head / "src_generated" / "main" / "script2.scala"
            )
          }
        }
      }
    }
  }
}
