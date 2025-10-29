package scala.cli.integration

import ch.epfl.scala.bsp4j as b
import com.eed3si9n.expecty.Expecty.expect

import java.nio.file.Files

import scala.cli.integration.TestUtil.await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.util.Properties

class JmhTests extends ScalaCliSuite with JmhSuite with BspSuite {
  override def group: ScalaCliSuite.TestGroup      = ScalaCliSuite.TestGroup.First
  override protected val extraOptions: Seq[String] = TestUtil.extraOptions

  for {
    useDirective <- Seq(None, Some("//> using jmh"))
    directiveString = useDirective.getOrElse("")
    jmhOptions      = if (useDirective.isEmpty) Seq("--jmh") else Nil
    testMessage     = useDirective match {
      case None            => jmhOptions.mkString(" ")
      case Some(directive) => directive
    }
  } {
    test(s"run ($testMessage)") {
      // TODO extract running benchmarks to a separate scope, or a separate sub-command
      simpleBenchmarkingInputs(directiveString).fromRoot { root =>
        val res =
          os.proc(TestUtil.cli, "--power", extraOptions, ".", jmhOptions).call(cwd = root)
        val output = res.out.trim()
        expect(output.contains(expectedInBenchmarkingOutput))
        expect(output.contains(s"JMH version: ${Constants.jmhVersion}"))
      }
    }

    test(s"compile ($testMessage)") {
      simpleBenchmarkingInputs(directiveString).fromRoot { root =>
        os.proc(TestUtil.cli, "compile", "--power", extraOptions, ".", jmhOptions)
          .call(cwd = root)
      }
    }

    test(s"doc ($testMessage)") {
      simpleBenchmarkingInputs(directiveString).fromRoot { root =>
        val res = os.proc(TestUtil.cli, "doc", "--power", extraOptions, ".", jmhOptions)
          .call(cwd = root, stderr = os.Pipe)
        expect(!res.err.trim().contains("Error"))
      }
    }

    test(s"setup-ide ($testMessage)") {
      // TODO fix setting jmh via a reload & add tests for it
      simpleBenchmarkingInputs(directiveString).fromRoot { root =>
        os.proc(TestUtil.cli, "setup-ide", "--power", extraOptions, ".", jmhOptions)
          .call(cwd = root)
      }
    }

    test(s"bsp ($testMessage)") {
      withBsp(simpleBenchmarkingInputs(directiveString), Seq(".", "--power") ++ jmhOptions) {
        (_, _, remoteServer) =>
          Future {
            val buildTargetsResp = remoteServer.workspaceBuildTargets().asScala.await
            val targets          = buildTargetsResp.getTargets.asScala.map(_.getId).toSeq
            expect(targets.length == 2)

            val compileResult =
              remoteServer.buildTargetCompile(new b.CompileParams(targets.asJava)).asScala.await
            val expectedStatusCode = b.StatusCode.OK
            expect(compileResult.getStatusCode == expectedStatusCode)

          }
      }
    }

    test(s"setup-ide + bsp ($testMessage)") {
      val inputs = simpleBenchmarkingInputs(directiveString)
      inputs.fromRoot { root =>
        os.proc(TestUtil.cli, "setup-ide", "--power", extraOptions, ".", jmhOptions)
          .call(cwd = root)
        val ideOptionsPath = root / Constants.workspaceDirName / "ide-options-v2.json"
        expect(ideOptionsPath.toNIO.toFile.exists())
        val ideLauncherOptsPath = root / Constants.workspaceDirName / "ide-launcher-options.json"
        expect(ideLauncherOptsPath.toNIO.toFile.exists())
        val ideEnvsPath = root / Constants.workspaceDirName / "ide-envs.json"
        expect(ideEnvsPath.toNIO.toFile.exists())
        val jsonOptions = List(
          "--json-options",
          ideOptionsPath.toString,
          "--json-launcher-options",
          ideLauncherOptsPath.toString,
          "--envs-file",
          ideEnvsPath.toString
        )
        withBsp(inputs, Seq("."), bspOptions = jsonOptions, reuseRoot = Some(root)) {
          (_, _, remoteServer) =>
            Future {
              val buildTargetsResp = remoteServer.workspaceBuildTargets().asScala.await
              val targets          = buildTargetsResp.getTargets.asScala.map(_.getId).toSeq
              expect(targets.length == 2)

              val compileResult =
                remoteServer.buildTargetCompile(new b.CompileParams(targets.asJava)).asScala.await
              val expectedStatusCode = b.StatusCode.OK
              expect(compileResult.getStatusCode == expectedStatusCode)
            }
        }
      }
    }

    test(s"package ($testMessage)") {
      // TODO make package with --jmh build an artifact that actually runs benchmarks
      val expectedMessage = "Placeholder main method"
      simpleBenchmarkingInputs(directiveString)
        .add(os.rel / "Main.scala" -> s"""@main def main: Unit = println("$expectedMessage")""")
        .fromRoot { root =>
          val launcherName = {
            val ext = if (Properties.isWin) ".bat" else ""
            "launcher" + ext
          }
          os.proc(
            TestUtil.cli,
            "package",
            "--power",
            TestUtil.extraOptions,
            ".",
            jmhOptions,
            "-o",
            launcherName
          )
            .call(cwd = root)
          val launcher = root / launcherName
          expect(os.isFile(launcher))
          expect(Files.isExecutable(launcher.toNIO))
          val output = TestUtil.maybeUseBash(launcher)(cwd = root).out.trim()
          expect(output == expectedMessage)
        }
    }

    test(s"export ($testMessage)") {
      simpleBenchmarkingInputs(directiveString).fromRoot { root =>
        // TODO add proper support for JMH export, we're checking if it doesn't fail the command for now
        os.proc(TestUtil.cli, "export", "--power", extraOptions, ".", jmhOptions)
          .call(cwd = root)
      }
    }
  }

  for {
    useDirective <- Seq(None, Some("//> using jmh false"))
    directiveString = useDirective.getOrElse("")
    jmhOptions      = if (useDirective.isEmpty) Seq("--jmh=false") else Nil
    testMessage     = useDirective match {
      case None             => jmhOptions.mkString(" ")
      case Some(directives) => directives.linesIterator.mkString("; ")
    }
    if !Properties.isWin
  } test(s"should not compile when jmh is explicitly disabled ($testMessage)") {
    simpleBenchmarkingInputs(directiveString).fromRoot { root =>
      val res =
        os.proc(TestUtil.cli, "compile", "--power", extraOptions, ".", jmhOptions)
          .call(cwd = root, check = false)
      expect(res.exitCode == 1)
    }
  }

  for {
    useDirective <- Seq(
      None,
      Some(
        s"""//> using jmh
           |//> using jmhVersion $exampleOldJmhVersion
           |""".stripMargin
      )
    )
    directiveString = useDirective.getOrElse("")
    jmhOptions      =
      if (useDirective.isEmpty) Seq("--jmh", "--jmh-version", exampleOldJmhVersion) else Nil
    testMessage = useDirective match {
      case None             => jmhOptions.mkString(" ")
      case Some(directives) => directives.linesIterator.mkString("; ")
    }
    if !Properties.isWin
  } test(s"should use the passed jmh version ($testMessage)") {
    simpleBenchmarkingInputs(directiveString).fromRoot { root =>
      val res =
        os.proc(TestUtil.cli, "run", "--power", extraOptions, ".", jmhOptions)
          .call(cwd = root)
      val output = res.out.trim()
      expect(output.contains(expectedInBenchmarkingOutput))
      expect(output.contains(s"JMH version: $exampleOldJmhVersion"))
    }
  }
}
