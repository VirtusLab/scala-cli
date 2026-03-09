package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.nio.file.Files

import scala.util.Properties

class PackageTestsDefault extends PackageTestDefinitions with TestDefault {
  test("reuse run native binary") {
    TestUtil.retryOnCi() {
      val inputs = TestInputs(
        os.rel / "Hello.scala" ->
          """object Hello {
            |  def main(args: Array[String]): Unit =
            |    println("Hello")
            |}
            |""".stripMargin
      )
      inputs.fromRoot { root =>
        val runRes = os.proc(TestUtil.cli, "run", "--native", ".", extraOptions)
          .call(cwd = root)
        val runOutput = runRes.out.trim().linesIterator.filter(!_.startsWith("[info] ")).toVector
        expect(runOutput == Seq("Hello"))

        val packageRes =
          os.proc(TestUtil.cli, "--power", "package", "--native", ".", "-o", "hello", extraOptions)
            .call(cwd = root, mergeErrIntoOut = true)
        val packageOutput    = packageRes.out.trim()
        val topPackageOutput =
          packageOutput.linesIterator.takeWhile(!_.startsWith("Wrote ")).toVector
        expect(topPackageOutput.forall(!_.startsWith("[info] ")))
      }
    }
  }

  for {
    (packageOpts, extension) <- Seq(
      Nil              -> (if (Properties.isWin) ".bat" else ""),
      Seq("--library") -> ".jar"
    ) ++
      (if (!TestUtil.isNativeCli || !Properties.isWin) Seq(
         Seq("--assembly") -> ".jar"
       )
       else Nil)
    packageDescription = packageOpts.headOption.getOrElse("bootstrap")
    crossScalaVersions = Seq(actualScalaVersion, Constants.scala213, Constants.scala212)
    numberOfBuilds     = crossScalaVersions.size
  } {
    test(s"package --cross ($packageDescription) produces $numberOfBuilds artifacts") {
      TestUtil.retryOnCi() {
        val mainClass = "Main"
        val message   = "Hello"
        TestInputs(
          os.rel / "project.scala"     -> s"//> using scala ${crossScalaVersions.mkString(" ")}",
          os.rel / s"$mainClass.scala" ->
            s"""object $mainClass extends App { println("$message") }"""
        ).fromRoot { root =>
          os.proc(
            TestUtil.cli,
            "--power",
            "package",
            "--cross",
            extraOptions,
            ".",
            packageOpts
          ).call(cwd = root)

          crossScalaVersions.foreach { version =>
            val expectedFile = root / s"${mainClass}_$version$extension"
            expect(os.isFile(expectedFile))
          }

          if packageDescription == "bootstrap" then
            crossScalaVersions.foreach { version =>
              val outputFile = root / s"${mainClass}_$version$extension"
              expect(Files.isExecutable(outputFile.toNIO))
              val output = TestUtil.maybeUseBash(outputFile)(cwd = root).out.trim()
              expect(output == message)
            }
        }
      }
    }

    test(s"package without --cross ($packageDescription) produces single artifact") {
      TestUtil.retryOnCi() {
        val mainClass = "Main"
        val message   = "Hello"
        TestInputs(
          os.rel / "project.scala"     -> s"//> using scala ${crossScalaVersions.mkString(" ")}",
          os.rel / s"$mainClass.scala" ->
            s"""object $mainClass extends App { println("$message") }"""
        ).fromRoot { root =>
          val r = os.proc(
            TestUtil.cli,
            "--power",
            "package",
            extraOptions,
            ".",
            packageOpts
          ).call(cwd = root, stderr = os.Pipe)

          val expectedFile = root / s"$mainClass$extension"
          expect(os.isFile(expectedFile))

          expect(r.err.trim().contains(s"ignoring ${numberOfBuilds - 1} builds"))
          expect(r.err.trim().contains(s"Defaulting to Scala $actualScalaVersion"))
        }
      }
    }
  }

}
